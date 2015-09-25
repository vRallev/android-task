/*
 * Copyright (c) 2014 Evernote Corporation. All rights reserved.
 */

package net.vrallev.android.task.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import net.vrallev.android.task.Task;

import java.util.concurrent.CountDownLatch;

public class ExplainActivity extends Activity {

    private static volatile CountDownLatch countDownLatch;

    public static boolean explainPermission(Context context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Don't call this method from the UI thread");
        }

        if (countDownLatch == null) {
            countDownLatch = new CountDownLatch(1);

            Intent intent = new Intent(context, ExplainActivity.class);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isFinishing()) {
            if (countDownLatch != null) {
                countDownLatch.countDown();
                countDownLatch = null;
            }
        }
    }

    public static class ExplainTask extends Task<Boolean> {

        @Override
        protected Boolean execute() {
            return ExplainActivity.explainPermission(getActivity());
        }
    }
}
