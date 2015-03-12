package net.vrallev.android.task.demo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import net.vrallev.android.task.TaskExecutor;
import net.vrallev.android.task.TaskResult;

/**
 * @author rwondratschek
 */
public class FragmentTestActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            new MyDialog().show(getSupportFragmentManager(), "Dialog");
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class MyDialog extends DialogFragment implements SimpleTask.ProgressCallback {

        private static final String TASK_ID_KEY = "TASK_ID_KEY";

        private int mTaskId;
        private SimpleTask mTask;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                mTask = new SimpleTask();
                mTaskId = TaskExecutor.getInstance().execute(mTask, this);

            } else {
                mTaskId = savedInstanceState.getInt(TASK_ID_KEY, -1);
                mTask = (SimpleTask) TaskExecutor.getInstance().getTask(mTaskId);
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.rotate_the_device));
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mTaskId != -1) {
                        SimpleTask task = (SimpleTask) TaskExecutor.getInstance().getTask(mTaskId);
                        if (task != null) {
                            task.cancel();
                        }
                    }
                }
            });

            return progressDialog;
        }

        @Override
        public void onStart() {
            super.onStart();
            mTask.setProgressCallback(this);
        }

        @Override
        public void onStop() {
            mTask.setProgressCallback(null);
            super.onStop();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(TASK_ID_KEY, mTaskId);
        }

        @Override
        public void onProgress(final long progress, final long max) {
            final ProgressDialog dialog = (ProgressDialog) getDialog();
            FragmentActivity activity = getActivity();

            if (dialog != null && activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMax((int) max);
                        dialog.setProgress((int) progress);
                    }
                });
            }
        }

        @TaskResult
        public void onResult(Boolean result) {
            FragmentActivity activity = getActivity();

            if (activity != null) {
                Toast.makeText(activity, "Result " + result, Toast.LENGTH_SHORT).show();
                activity.finish();
            }

        }
    }

}
