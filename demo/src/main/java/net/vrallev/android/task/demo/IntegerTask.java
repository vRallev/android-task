package net.vrallev.android.task.demo;

import android.os.SystemClock;

import net.vrallev.android.task.Task;

import java.util.Random;

/**
 * @author rwondratschek
 */
public class IntegerTask extends Task<Integer> {

    @Override
    protected Integer execute() {
        int sleep = new Random().nextInt(1000);
        SystemClock.sleep(sleep);

        return sleep;
    }
}
