package net.vrallev.android.task.demo;

import android.os.SystemClock;

import net.vrallev.android.task.Task;

/**
 * @author rwondratschek
 */
public class CrashingTask extends Task<Boolean> {

    @Override
    protected Boolean execute() {
        SystemClock.sleep(1_000);
        throw new IllegalStateException("bla");
    }
}
