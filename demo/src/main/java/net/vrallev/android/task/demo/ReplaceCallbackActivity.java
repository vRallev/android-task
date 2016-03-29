package net.vrallev.android.task.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import net.vrallev.android.task.TaskExecutor;
import net.vrallev.android.task.TaskResult;

import java.util.List;

/**
 * @author rwondratschek
 */
public class ReplaceCallbackActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<IntegerTask> tasks = TaskExecutor.getInstance().getAllTasks(IntegerTask.class);
        for (IntegerTask task : tasks) {
            task.replaceCallback(this, "test");
        }
    }

    @TaskResult(id = "test")
    public void onResult(Integer integer) {
        Toast.makeText(this, "Replaced " + integer, Toast.LENGTH_SHORT).show();
    }
}
