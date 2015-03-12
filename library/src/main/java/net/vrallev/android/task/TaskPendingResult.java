package net.vrallev.android.task;

/**
 * @author rwondratschek
 */
public class TaskPendingResult {

    private final Class<?> mResultType;
    private final Object mResult;
    private final Task<?> mTask;
    private final TaskExecutor mTaskExecutor;

    public TaskPendingResult(Class<?> resultType, Object result, Task<?> task, TaskExecutor taskExecutor) {
        mResultType = resultType;
        mResult = result;
        mTask = task;
        mTaskExecutor = taskExecutor;
    }

    public Class<?> getResultType() {
        return mResultType;
    }

    public Object getResult() {
        return mResult;
    }

    public Task<?> getTask() {
        return mTask;
    }

    public TaskExecutor getTaskExecutor() {
        return mTaskExecutor;
    }
}
