package net.vrallev.android.task;

/**
 * @author rwondratschek
 */
@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public abstract class Task <RESULT> {

    protected abstract RESULT execute();

    private int mKey = -1;
    private boolean mCancelled;
    private TaskExecutor mTaskExecutor;

    /*package*/ final void setKey(int key) {
        mKey = key;
    }

    /*package*/ final void setTaskExecutor(TaskExecutor taskExecutor) {
        mTaskExecutor = taskExecutor;
    }

    public final int getKey() {
        return mKey;
    }

    public void cancel() {
        mCancelled = true;
    }

    public boolean isCancelled() {
        return mCancelled || Thread.currentThread().isInterrupted();
    }
}
