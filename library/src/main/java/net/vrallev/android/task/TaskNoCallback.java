package net.vrallev.android.task;

/**
 * @author rwondratschek
 */
public abstract class TaskNoCallback extends Task<Void> {

    @Override
    protected final Void execute() {
        executeTask();
        return null;
    }

    protected abstract void executeTask();
}
