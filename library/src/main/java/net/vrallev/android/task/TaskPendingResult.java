package net.vrallev.android.task;

/**
 * @author rwondratschek
 */
public class TaskPendingResult {

    private final Class<?> mResultType;
    private final Object mResult;

    public TaskPendingResult(Class<?> resultType, Object result) {
        mResultType = resultType;
        mResult = result;
    }

    public Class<?> getResultType() {
        return mResultType;
    }

    public Object getResult() {
        return mResult;
    }
}
