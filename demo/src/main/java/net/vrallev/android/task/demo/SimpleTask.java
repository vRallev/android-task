package net.vrallev.android.task.demo;

import android.os.SystemClock;

import net.vrallev.android.task.Task;

import java.util.Random;

/**
 * @author rwondratschek
 */
public class SimpleTask extends Task<Boolean> {

    private static final long SLEEP_TIME = 5_000L;

    private ProgressCallback mProgressCallback;

    public void setProgressCallback(ProgressCallback progressCallback) {
        mProgressCallback = progressCallback;
    }

    @Override
    public Boolean execute() {

        final long start = System.currentTimeMillis();
        final long end = start + SLEEP_TIME;

        long progress = start;

        while (progress < end && !isCancelled()) {
            onProgress(progress - start, end - start);

            SystemClock.sleep(5l);
            progress = System.currentTimeMillis();
        }

        if (isCancelled()) {
            return new Random().nextBoolean() ? false : null;
        } else {
            return true;
        }
    }

    private void onProgress(long progress, long max) {
        if (mProgressCallback != null) {
            mProgressCallback.onProgress(progress, max);
        }
    }

    public interface ProgressCallback {
        public void onProgress(long progress, long max);
    }
}
