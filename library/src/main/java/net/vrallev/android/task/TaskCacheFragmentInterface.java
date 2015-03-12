package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author rwondratschek
 */
@SuppressWarnings({"UnusedDeclaration", "UnnecessaryInterfaceModifier"})
public interface TaskCacheFragmentInterface {

    public static final String PENDING_RESULT_KEY = "PENDING_RESULT_KEY";

    public boolean canSaveInstanceState();

    public <T> T get(String key);

    public <T> T put(String key, Object value);

    public <T> T remove(String key);

    public void putPendingResult(TaskPendingResult pendingResult);

    public Activity getParentActivity();

    public static interface Factory {
        public TaskCacheFragmentInterface create(Activity activity);
    }

    public static final Factory DEFAULT_FACTORY = new Factory() {
        @Override
        public TaskCacheFragmentInterface create(Activity activity) {
            if (activity instanceof FragmentActivity) {
                return TaskCacheFragmentSupport.getFrom((FragmentActivity) activity);
            } else {
                return TaskCacheFragment.getFrom(activity);
            }
        }
    };

    /*package*/ static final class Helper {
        public static void postPendingResults(List<TaskPendingResult> pendingResults, TaskCacheFragmentInterface cacheFragment) {
            final TargetMethodFinder targetMethodFinder = new TargetMethodFinder(TaskResult.class);

            for (TaskPendingResult pendingResult : pendingResults) {
                Pair<Method, Object> target = targetMethodFinder.getMethod(cacheFragment, pendingResult.getResultType(), pendingResult.getTask());

                TaskExecutor taskExecutor = pendingResult.getTaskExecutor();
                taskExecutor.postResultNow(target, pendingResult.getResult(), pendingResult.getTask());
            }

            pendingResults.clear();
        }
    }
}
