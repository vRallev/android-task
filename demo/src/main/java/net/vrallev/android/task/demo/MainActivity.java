package net.vrallev.android.task.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import net.vrallev.android.task.Task;
import net.vrallev.android.task.TaskExecutor;
import net.vrallev.android.task.TaskResult;

import java.util.Iterator;
import java.util.List;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
public class MainActivity extends Activity {

    private static final String ANNOTATION_ID = "annotationId";

    private static final String TASK_ID_KEY = "TASK_ID_KEY";

    private int mTaskId;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mTaskId = savedInstanceState.getInt(TASK_ID_KEY, -1);
        } else {
            mTaskId = -1;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mTaskId != -1) {
            showDialog();
        }
    }

    @Override
    protected void onStop() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(TASK_ID_KEY, mTaskId);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_default_activity:
                testDefaultActivity();
                break;

            case R.id.button_support_fragment:
                testSupportFragment();
                break;

            case R.id.button_integer:
                testIntegerTask();
                break;

            case R.id.button_annotation_with_id:
                testAnnotationWithId();
                break;

            case R.id.button_get_all_tasks:
                testGetAllTasks();
                break;

            case R.id.button_shutdown_executor:
                testShutdownExecutor();
                break;
        }
    }

    @TaskResult
    public void onResult(Boolean result) {
        Toast.makeText(this, "Result " + result, Toast.LENGTH_SHORT).show();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        mTaskId = -1;
    }

    @TaskResult
    public void onResult(Integer integer, IntegerTask task) {
        Toast.makeText(this, "Result " + integer, Toast.LENGTH_SHORT).show();
    }

    @TaskResult(id = ANNOTATION_ID)
    public void onResultWithId(Integer integer, Task<?> task) {
        Toast.makeText(this, "Result with ID " + integer + ", finished = " + task.isFinished(), Toast.LENGTH_SHORT).show();
    }

    private void testDefaultActivity() {
        mTaskId = TaskExecutor.getInstance().execute(new SimpleTask(), this);
        showDialog();
    }

    private void testSupportFragment() {
        startActivity(new Intent(this, FragmentTestActivity.class));
    }

    private void testIntegerTask() {
        TaskExecutor.getInstance().execute(new IntegerTask(), this);
    }

    private void testAnnotationWithId() {
        TaskExecutor.getInstance().execute(new IntegerTask(), this, ANNOTATION_ID);
    }

    private void testGetAllTasks() {
        List<IntegerTask> allTasks = TaskExecutor.getInstance().getAllTasks(IntegerTask.class);
        Iterator<IntegerTask> iterator = allTasks.iterator();

        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            stringBuilder.append(iterator.next());
            if (iterator.hasNext()) {
                stringBuilder.append("\n\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Tasks")
                .setMessage(stringBuilder)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void testShutdownExecutor() {
        TaskExecutor.getInstance().shutdown();
    }

    private void showDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.rotate_the_device));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
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
        mProgressDialog.show();
    }
}
