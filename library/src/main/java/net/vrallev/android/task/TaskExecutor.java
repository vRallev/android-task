package net.vrallev.android.task;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

    private static volatile TaskExecutor instance;

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

    private final SparseArray<Task<?>> mTasks;
    private final SparseArray<WeakReference<TaskRunnable<?>>> mTaskRunnables;
    private final TargetMethodFinder mTargetMethodFinder;

    private Application mApplication;

    private TaskExecutor(ExecutorService executorService, PostResult postResult) {
        mExecutorService = executorService;
        mPostResult = postResult;

        mTasks = new SparseArray<>();
        mTaskRunnables = new SparseArray<>();
        mTargetMethodFinder = new TargetMethodFinder(TaskResult.class);
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Fragment callback) {
        return execute(task, callback, null);
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Fragment callback, @Nullable String annotationId) {
        FragmentActivity activity = callback.getActivity();
        return executeInner(task, activity, annotationId, FragmentIdHelper.getFragmentId(callback));
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Activity callback) {
        return execute(task, callback, null);
    }

    public synchronized int execute(@NonNull Task<?> task, @NonNull Activity callback, @Nullable String annotationId) {
        return executeInner(task, callback, annotationId, null);
    }

    private synchronized int executeInner(Task<?> task, Activity activity, String annotationId, String fragmentId) {
        if (isShutdown()) {
            return -1;
        }

        if (mApplication == null) {
            mApplication = activity.getApplication();
        }

        int key = TASK_COUNTER.incrementAndGet();

        task.setKey(key);
        task.setTaskExecutor(this);
        task.setCachedActivity(activity);
        task.setAnnotationId(annotationId);
        task.setFragmentId(fragmentId);

        mTasks.put(key, task);

        TaskRunnable<?> taskRunnable = new TaskRunnable<>(task, activity);

        mTaskRunnables.put(key, new WeakReference<TaskRunnable<?>>(taskRunnable));

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

        int key = task.getKey();
        mTaskRunnables.remove(key);
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

    /*package*/ void postResultNow(Pair<Method, Object> target, Object result, TaskRunnable<?> taskRunnable) {
        cleanUpTask(taskRunnable);

        mTargetMethodFinder.invoke(target, result, taskRunnable.mTask);
    }

    /*package*/ boolean updateCallback(Task<?> task, Fragment callback, String annotationId) {
        if (callback == null) {
            return false;
        }

        if (updateCallback(task, callback.getActivity(), annotationId)) {
            task.setFragmentId(FragmentIdHelper.getFragmentId(callback));
            return true;
        } else {
            return false;
        }
    }

    /*package*/ boolean updateCallback(Task<?> task, Activity callback, String annotationId) {
        if (task == null || callback == null) {
            return false;
        }

        WeakReference<TaskRunnable<?>> reference = mTaskRunnables.get(task.getKey());
        if (reference == null) {
            return false;
        }
        TaskRunnable<?> runnable = reference.get();
        if (runnable == null || runnable.mPostingResult || runnable.mTask != task) {
            return false;
        }

        task.setCachedActivity(callback);
        task.setAnnotationId(annotationId);
        runnable.updateCallbackActivity(callback);
        return true;
    }

    private void cleanUpTask(TaskRunnable<?> taskRunnable) {
        if (taskRunnable.mTask.isExecuting() && !taskRunnable.mTask.isCancelled()) {
            taskRunnable.mTask.cancel();
        }
        taskRunnable.mTask.setFinished();
        removeTask(taskRunnable.mTask);
        mApplication.unregisterActivityLifecycleCallbacks(taskRunnable);
    }

    private final class TaskRunnable<T> implements Runnable, Application.ActivityLifecycleCallbacks {

        private static final String ACTIVITY_HASH = "ACTIVITY_HASH";

        private final Task<T> mTask;

        private int mActivityHash;
        private volatile boolean mCanSaveInstanceState;
        private volatile boolean mPostingResult;

        private TaskRunnable(Task<T> task, Activity activity) {
            mTask = task;
            updateCallbackActivity(activity);
        }

        @Override
        public void run() {
            final T result = mTask.executeInner();

            if (mTask instanceof TaskNoCallback) {
                cleanUpTask(this);
                return;
            }

            Activity activity = mTask.getActivity();
            if (activity != null) {
                postResult(result, activity);
            }
            // else wait for onCreate of activity in life cycle callbacks
        }

        private void postResultFromLifeCycleCallback(final Activity activity) {
            if (isCallbackActivity(activity)) {
                mCanSaveInstanceState = true;
                if (!mTask.isExecuting()) {
                    postResult(mTask.getResult(), activity);
                }
            }
        }

        private void postResult(final T result, final Activity activity) {
            if (mPostingResult) {
                return;
            }
            if (mTask.isFinished()) {
                // already cleaned up
                return;
            }

            mPostingResult = true;

            if (isShutdown()) {
                cleanUpTask(this);
                return;
            }

            final Pair<Method, Object> target = mTargetMethodFinder.getMethod(activity, mTargetMethodFinder.getResultType(result, mTask), mTask);
            if (target == null) {
                cleanUpTask(this);
                return;
            }

            if (mPostResult == PostResult.IMMEDIATELY) {
                postResultNow(target, result, this);
                return;
            }

            if (mCanSaveInstanceState) {
                if (mPostResult == PostResult.ON_ANY_THREAD || Looper.getMainLooper() == Looper.myLooper()) {
                    postResultNow(target, result, this);

                } else {
                    final CountDownLatch latch = new CountDownLatch(1);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mCanSaveInstanceState) {
                                postResultNow(target, result, TaskRunnable.this);
                            } else {
                                mPostingResult = false;
                            }
                            latch.countDown();
                        }
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException ignored) {
                    }
                }
            } else {
                mPostingResult = false;
            }
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (savedInstanceState == null) {
                return;
            }

            int activityHash = savedInstanceState.getInt(ACTIVITY_HASH, -1);
            if (activityHash != mActivityHash) {
                return;
            }

            mActivityHash = activity.hashCode();
            mTask.setCachedActivity(activity);

            postResultFromLifeCycleCallback(activity);
        }

        @Override
        public void onActivityStarted(Activity activity) {
            postResultFromLifeCycleCallback(activity);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            postResultFromLifeCycleCallback(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            // do nothing
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (isCallbackActivity(activity)) {
                mCanSaveInstanceState = false;
                if (activity.isFinishing()) {
                    cleanUpTask(this);
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            if (isCallbackActivity(activity)) {
                mCanSaveInstanceState = false;
                outState.putInt(ACTIVITY_HASH, mActivityHash);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            // do nothing
        }

        private void updateCallbackActivity(Activity activity) {
            mActivityHash = activity.hashCode();
            mCanSaveInstanceState = getInitialSaveInstanceState(activity);
        }

        private boolean isCallbackActivity(Activity activity) {
            return activity.hashCode() == mActivityHash;
        }

        private boolean getInitialSaveInstanceState(Activity activity) {
            try {
                android.app.Fragment fragment = new android.app.Fragment();
                activity.getFragmentManager().beginTransaction().add(fragment, "GetVisibilityFragment").commit();
                activity.getFragmentManager().beginTransaction().remove(fragment).commit();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static class Builder {

        private PostResult mPostResult;
        private ExecutorService mExecutorService;

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

        public TaskExecutor build() {
            if (mPostResult == null) {
                mPostResult = PostResult.UI_THREAD;
            }
            if (mExecutorService == null) {
                mExecutorService = Executors.newCachedThreadPool();
            }
            return new TaskExecutor(mExecutorService, mPostResult);
        }
    }

    public enum PostResult {
        IMMEDIATELY,
        ON_ANY_THREAD,
        UI_THREAD
    }
}
