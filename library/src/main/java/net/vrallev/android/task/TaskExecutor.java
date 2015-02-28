package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
public final class TaskExecutor {

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
    private PostResult mPostResult;

    private SparseArray<Task<?>> mTasks;
    private TargetMethodFinder mTargetMethodFinder;

    private TaskExecutor(ExecutorService executorService, PostResult postResult) {
        mExecutorService = executorService;
        mPostResult = postResult;

        mTasks = new SparseArray<>();
        mTargetMethodFinder = new TargetMethodFinder(TaskResult.class);
    }

    public synchronized int execute(Task<?> task, Fragment callback) {
        return execute(task, callback.getActivity());
    }

    public synchronized int execute(Task<?> task, Activity callback) {
        if (callback instanceof FragmentActivity) {
            return executeInner(task, TaskCacheFragmentSupport.getFrom((FragmentActivity) callback));
        } else {
            return executeInner(task, TaskCacheFragment.getFrom(callback));
        }
    }

    private synchronized int executeInner(Task<?> task, TaskCacheFragmentInterface cacheFragment) {
        int key = TASK_COUNTER.incrementAndGet();

        task.setKey(key);
        task.setTaskExecutor(this);
        task.setCacheFragment(cacheFragment);

        mTasks.put(key, task);

        mExecutorService.execute(new TaskRunnable<>(task, cacheFragment));

        return key;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> T getTask(int key) {
        return (T) mTasks.get(key);
    }

    private synchronized void removeTask(Task<?> task) {
        mTasks.removeAt(mTasks.indexOfValue(task));
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

    private final class TaskRunnable <T> implements Runnable {

        private final Task<T> mTask;
        private final WeakReference<TaskCacheFragmentInterface> mWeakReference;

        private TaskRunnable(Task<T> task, TaskCacheFragmentInterface cacheFragment) {
            mTask = task;
            mWeakReference = new WeakReference<>(cacheFragment);
        }

        @Override
        public void run() {
            final T result = mTask.execute();
            if (TaskExecutor.this.isShutdown()) {
                return;
            }

            TaskExecutor.this.removeTask(mTask);

            final TargetMethodFinder targetMethodFinder = TaskExecutor.this.mTargetMethodFinder;

            final TaskCacheFragmentInterface cacheFragment = mWeakReference.get();
            if (cacheFragment == null) {
                return;
            }

            if (TaskExecutor.this.mPostResult.equals(PostResult.IMMEDIATELY)) {
                TaskExecutor.this.postResultNow(cacheFragment, result, mTask);
                return;
            }

            if (cacheFragment.canSaveInstanceState()) {
                if (TaskExecutor.this.mPostResult.equals(PostResult.ON_ANY_THREAD)) {
                    TaskExecutor.this.postResultNow(cacheFragment, result, mTask);

                } else {
                    final Pair<Method, Object> target = targetMethodFinder.getMethod(cacheFragment, targetMethodFinder.getResultType(result, mTask));
                    if (target != null) {
                        cacheFragment.getParentActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                targetMethodFinder.invoke(target, result);
                            }
                        });
                    }
                }

            } else {
                final Class<?> resultType = targetMethodFinder.getResultType(result, mTask);
                if (resultType != null) {
                    cacheFragment.putPendingResult(new TaskPendingResult(resultType, result));
                }
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

    public static enum PostResult {
        IMMEDIATELY,
        ON_ANY_THREAD,
        UI_THREAD
    }
}
