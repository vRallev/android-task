package net.vrallev.android.task.demo;

import android.app.Activity;
import android.os.SystemClock;
import android.widget.Toast;

import net.vrallev.android.task.TaskNoCallback;

/**
 * @author rwondratschek
 */
public class SimpleNoCallbackTask extends TaskNoCallback {

    @Override
    protected void executeTask() {
        SystemClock.sleep(1_000);

        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Finished without callback", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
