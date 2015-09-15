package net.vrallev.android.task;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
public final class TaskExecutor {

    private static final String TAG = "TaskExecutor";

    private static final AtomicInteger TASK_COUNTER = new AtomicInteger(0);

    private static TaskExecutor instance;

    public static TaskExecutor getInstance() {
        if (instance == null) {
            synchronized (TaskExecutor.class) {
                if (instance == null) {
                    new Builder()
                            .build()
                            .asSingleton();
                }
            }
        }

        return instance;
    }

    private ExecutorService mExecutorService;
    private final PostResult mPostResult;
    private final TaskCacheFragmentInterface.Factory mCacheFactory;

    private SparseArray<Task<?>> mTasks;
    private TargetMethodFinder mTargetMethodFinder;

    private Application mApplication;

    private TaskExecutor(ExecutorService executorService, PostResult postResult, TaskCacheFragmentInterface.Factory factory) {
        mExecutorService = executorService;
        mPostResult = postResult;
        mCacheFactory = factory;

        mTasks = new SparseArray<>();
        mTargetMethodFinder = new TargetMethodFinder(TaskResult.class);
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Fragment callback) {
        return execute(task, callback, null);
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Fragment callback, @Nullable String annotationId) {
        FragmentActivity activity = callback.getActivity();
        return executeInner(task, activity, mCacheFactory.create(activity), annotationId, FragmentIdHelper.getFragmentId(callback));
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Activity callback) {
        return execute(task, callback, null);
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Activity callback, @Nullable String annotationId) {
        return executeInner(task, callback, mCacheFactory.create(callback), annotationId, null);
    }

    private synchronized int executeInner(Task<?> task, Activity activity, TaskCacheFragmentInterface cacheFragment, String annotationId, String fragmentId) {
        if (isShutdown()) {
            return -1;
        }

        if (mApplication == null) {
            mApplication = activity.getApplication();
        }

        int key = TASK_COUNTER.incrementAndGet();

        task.setKey(key);
        task.setTaskExecutor(this);
        task.setCacheFragment(cacheFragment);
        task.setAnnotationId(annotationId);
        task.setFragmentId(fragmentId);

        mTasks.put(key, task);

        TaskRunnable<?> taskRunnable = new TaskRunnable<>(task, cacheFragment);
        mApplication.registerActivityLifecycleCallbacks(taskRunnable);
        mExecutorService.execute(taskRunnable);

        return key;
    }

    @SuppressWarnings("unchecked")
    public synchronized Task<?> getTask(int key) {
        if (mTasks.indexOfKey(key) < 0) {
            return null;
        } else {
            return mTasks.get(key);
        }
    }

    public synchronized List<Task<?>> getAllTasks() {
        List<Task<?>> result = new ArrayList<>();
        for (int i = 0; i < mTasks.size(); i++) {
            result.add(mTasks.valueAt(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends Task<?>> List<T> getAllTasks(Class<T> taskClass) {
        List<Task<?>> list = getAllTasks();
        Iterator<Task<?>> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (!taskClass.isAssignableFrom(iterator.next().getClass())) {
                iterator.remove();
            }
        }
        return (List<T>) list;
    }

    private synchronized void removeTask(Task<?> task) {
        int index = mTasks.indexOfValue(task);
        if (index >= 0) {
            mTasks.removeAt(index);
        }
    }

    public TaskExecutor asSingleton() {
        synchronized (TaskExecutor.class) {
            instance = this;
        }
        return this;
    }

    public synchronized void shutdown() {
        mExecutorService.shutdownNow();
        mExecutorService = null;

        synchronized (TaskExecutor.class) {
            if (this == instance) {
                instance = null;
            }
        }
    }

    public synchronized boolean isShutdown() {
        return mExecutorService == null;
    }

    /*package*/ void postResultNow(Pair<Method, Object> target, Object result, Task<?> task) {
        cleanUpTask(task);

        mTargetMethodFinder.invoke(target, result, task);
    }

    private void cleanUpTask(Task<?> task) {
        task.setFinished();
        removeTask(task);
    }

    private final class TaskRunnable<T> implements Runnable, Application.ActivityLifecycleCallbacks {

        private final Task<T> mTask;
        private final WeakReference<TaskCacheFragmentInterface> mWeakReference;

        private TaskRunnable(Task<T> task, TaskCacheFragmentInterface cacheFragment) {
            mTask = task;
            mWeakReference = new WeakReference<>(cacheFragment);
        }

        @Override
        public void run() {
            final T result = mTask.executeInner();

            if (mTask instanceof TaskNoCallback) {
                cleanUpTask(mTask);
                mApplication.unregisterActivityLifecycleCallbacks(this);
                return;
            }

            final TaskCacheFragmentInterface cacheFragment = mWeakReference.get();
            if (cacheFragment != null) {
                postResult(result, cacheFragment);
            }
            // else wait for onCreate of activity
        }

        private void postResult(final T result, final TaskCacheFragmentInterface cacheFragment) {
            if (isShutdown()) {
                cleanUpTask(mTask);
                mApplication.unregisterActivityLifecycleCallbacks(this);
                return;
            }

            final Pair<Method, Object> target = mTargetMethodFinder.getMethod(cacheFragment, mTargetMethodFinder.getResultType(result, mTask), mTask);
            if (target == null) {
                cleanUpTask(mTask);
                mApplication.unregisterActivityLifecycleCallbacks(this);
                return;
            }

            if (mPostResult.equals(PostResult.IMMEDIATELY)) {
                mApplication.unregisterActivityLifecycleCallbacks(this);
                postResultNow(target, result, mTask);
                return;
            }

            if (cacheFragment.canSaveInstanceState()) {
                mApplication.unregisterActivityLifecycleCallbacks(this);

                if (mPostResult.equals(PostResult.ON_ANY_THREAD)) {
                    postResultNow(target, result, mTask);

                } else {
                    cacheFragment.getParentActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            postResultNow(target, result, mTask);
                        }
                    });
                }

            } else {
                final Class<?> resultType = mTargetMethodFinder.getResultType(result, mTask);
                if (resultType != null) {
                    TaskCacheFragmentInterface.Helper.putPendingResult(cacheFragment, new TaskPendingResult(resultType, result, mTask, TaskExecutor.this));

                    // race condition
                    if (cacheFragment.canSaveInstanceState()) {
                        if (mPostResult == PostResult.ON_ANY_THREAD) {
                            TaskCacheFragmentInterface.Helper.postPendingResults(cacheFragment);
                        } else {
                            cacheFragment.getParentActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TaskCacheFragmentInterface.Helper.postPendingResults(cacheFragment);
                                }
                            });
                        }
                    }

                }
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (savedInstanceState == null || mTask.isExecuting()) {
                return;
            }

            int key = savedInstanceState.getInt(String.valueOf(mTask.getKey()), -1);
            if (key == -1) {
                mApplication.unregisterActivityLifecycleCallbacks(this);
                return;
            }

            if (key != mTask.getKey()) {
                return;
            }

            mApplication.unregisterActivityLifecycleCallbacks(this);

            TaskCacheFragmentInterface cacheFragment = mCacheFactory.create(activity);
            try {
                postResult(mTask.getResult(), cacheFragment);
            } catch (InterruptedException e) {
                Log.e(TAG, "getResult failed", e);
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (mTask.isExecuting()) {
                return;
            }

            TaskCacheFragmentInterface cacheFragment = mCacheFactory.create(activity);
            List<TaskPendingResult> list = cacheFragment.get(TaskCacheFragmentInterface.PENDING_RESULT_KEY);
            if (list != null && !list.isEmpty()) {
                mApplication.unregisterActivityLifecycleCallbacks(this);
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            // do nothing
        }

        @Override
        public void onActivityPaused(Activity activity) {
            // do nothing
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (activity.isFinishing()) {
                mApplication.unregisterActivityLifecycleCallbacks(this);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            TaskCacheFragmentInterface fragment = mWeakReference.get();
            if (fragment == null || fragment.getParentActivity() != activity) {
                return;
            }

            outState.putInt(String.valueOf(mTask.getKey()), mTask.getKey());
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            // do nothing
        }
    }

    public static class Builder {

        private PostResult mPostResult;
        private ExecutorService mExecutorService;
        private TaskCacheFragmentInterface.Factory mCacheFactory;

        public Builder() {

        }

        public Builder setPostResult(PostResult postResult) {
            mPostResult = postResult;
            return this;
        }

        public Builder setExecutorService(ExecutorService executorService) {
            mExecutorService = executorService;
            return this;
        }

        public Builder setCacheFactory(TaskCacheFragmentInterface.Factory cacheFactory) {
            mCacheFactory = cacheFactory;
            return this;
        }

        public TaskExecutor build() {
            if (mPostResult == null) {
                mPostResult = PostResult.UI_THREAD;
            }
            if (mExecutorService == null) {
                mExecutorService = Executors.newCachedThreadPool();
            }
            if (mCacheFactory == null) {
                mCacheFactory = TaskCacheFragmentInterface.DEFAULT_FACTORY;
            }
            return new TaskExecutor(mExecutorService, mPostResult, mCacheFactory);
        }
    }

    public enum PostResult {
        IMMEDIATELY,
        ON_ANY_THREAD,
        UI_THREAD
    }
}
