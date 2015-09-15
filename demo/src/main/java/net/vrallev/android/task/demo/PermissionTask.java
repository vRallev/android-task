package net.vrallev.android.task.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import net.vrallev.android.task.Task;

import java.util.concurrent.CountDownLatch;

/**
 * @author rwondratschek
 */
public class PermissionTask extends Task<Boolean> {

    private final CountDownLatch mCountDownLatch;
    private boolean mResult;

    public PermissionTask() {
        mCountDownLatch = new CountDownLatch(1);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected Boolean execute() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, 2);

        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mResult;
    }

    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mResult = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        mCountDownLatch.countDown();
    }
}
