package net.vrallev.android.task;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
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

    public synchronized int execute(Task<?> task, Fragment callback) {
        return execute(task, callback.getActivity());
    }

    public synchronized int execute(Task<?> task, Activity callback) {
        return executeInner(task, callback, mCacheFactory.create(callback));
    }

    private synchronized int executeInner(Task<?> task, Activity activity, TaskCacheFragmentInterface cacheFragment) {
        if (mApplication == null) {
            mApplication = activity.getApplication();
        }

        int key = TASK_COUNTER.incrementAndGet();

        task.setKey(key);
        task.setTaskExecutor(this);
        task.setCacheFragment(cacheFragment);

        mTasks.put(key, task);

        TaskRunnable<?> taskRunnable = new TaskRunnable<>(task, cacheFragment);
        mApplication.registerActivityLifecycleCallbacks(taskRunnable);
        mExecutorService.execute(taskRunnable);

        return key;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T getTask(int key) {
        return (T) mTasks.get(key);
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

    public void shutdown() {
        mExecutorService.shutdownNow();
        mExecutorService = null;
        mTasks.clear();
        mTasks = null;

        synchronized (TaskExecutor.class) {
            if (this == instance) {
                instance = null;
            }
        }
    }

    public boolean isShutdown() {
        return mExecutorService == null;
    }

    private void postResultNow(TaskCacheFragmentInterface cacheFragment, Object result, Task<?> task) {
        mTargetMethodFinder.post(cacheFragment, result, task);
    }

    private final class TaskRunnable <T> implements Runnable, Application.ActivityLifecycleCallbacks {

        private final Task<T> mTask;
        private final WeakReference<TaskCacheFragmentInterface> mWeakReference;

        private TaskRunnable(Task<T> task, TaskCacheFragmentInterface cacheFragment) {
            mTask = task;
            mWeakReference = new WeakReference<>(cacheFragment);
        }

        @Override
        public void run() {
            final T result = mTask.executeInner();

            final TaskCacheFragmentInterface cacheFragment = mWeakReference.get();
            if (cacheFragment != null) {
                postResult(result, cacheFragment);
            }
            // else wait for onCreate of activity
        }

        private void postResult(final T result, TaskCacheFragmentInterface cacheFragment) {
            if (TaskExecutor.this.isShutdown()) {
                return;
            }

            removeTask(mTask);

            if (mPostResult.equals(PostResult.IMMEDIATELY)) {
                mApplication.unregisterActivityLifecycleCallbacks(this);
                TaskExecutor.this.postResultNow(cacheFragment, result, mTask);
                return;
            }

            if (cacheFragment.canSaveInstanceState()) {
                mApplication.unregisterActivityLifecycleCallbacks(this);

                if (mPostResult.equals(PostResult.ON_ANY_THREAD)) {
                    TaskExecutor.this.postResultNow(cacheFragment, result, mTask);

                } else {
                    final Pair<Method, Object> target = mTargetMethodFinder.getMethod(cacheFragment, mTargetMethodFinder.getResultType(result, mTask));
                    if (target != null) {
                        cacheFragment.getParentActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTargetMethodFinder.invoke(target, result);
                            }
                        });
                    }
                }

            } else {
                final Class<?> resultType = mTargetMethodFinder.getResultType(result, mTask);
                if (resultType != null) {
                    cacheFragment.putPendingResult(new TaskPendingResult(resultType, result));
                }
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (savedInstanceState == null || !mTask.isFinished()) {
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
            List<TaskPendingResult> list = cacheFragment.get(TaskCacheFragmentInterface.PENDING_RESULT_KEY);
            if (list == null || list.isEmpty()) {
                try {
                    postResult(mTask.getResult(), cacheFragment);
                } catch (InterruptedException e) {
                    Log.e(TAG, "getResult failed", e);
                }
            }
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (!mTask.isFinished()) {
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
            // do nothing
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

    public static enum PostResult {
        IMMEDIATELY,
        ON_ANY_THREAD,
        UI_THREAD
    }
}
