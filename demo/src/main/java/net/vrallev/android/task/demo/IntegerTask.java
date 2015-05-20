package net.vrallev.android.task.demo;

import android.os.SystemClock;

import net.vrallev.android.task.Task;

import java.util.Random;

/**
 * @author rwondratschek
 */
public class IntegerTask extends Task<Integer> {

    private final int mSleepOffset;

    public IntegerTask() {
        this(2000);
    }

    public IntegerTask(int sleepOffset) {
        mSleepOffset = sleepOffset;
    }

    @Override
    protected Integer execute() {
        int sleep = new Random().nextInt(1000) + mSleepOffset;
        SystemClock.sleep(sleep);

        return sleep;
    }
}
