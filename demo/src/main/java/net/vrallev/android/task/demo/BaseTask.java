package net.vrallev.android.task.demo;

import net.vrallev.android.task.Task;

/**
 * @author rwondratschek
 */
public abstract class BaseTask<RESULT> extends Task<RESULT> {

    @Override
    protected final RESULT execute() {
        return otherExecute();
    }

    protected abstract RESULT otherExecute();
}
