package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.util.Pair;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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

        private static final SparseArray<WeakReference<TaskCacheFragmentInterface>> TEMP_FRAG_CACHE = new SparseArray<>();

        public static synchronized void postPendingResults(TaskCacheFragmentInterface cacheFragment) {
            List<TaskPendingResult> pendingResults = cacheFragment.get(PENDING_RESULT_KEY);
            if (pendingResults == null || pendingResults.isEmpty()) {
                return;
            }

            final TargetMethodFinder targetMethodFinder = new TargetMethodFinder(TaskResult.class);

            for (TaskPendingResult pendingResult : pendingResults) {
                Pair<Method, Object> target = targetMethodFinder.getMethod(cacheFragment, pendingResult.getResultType(), pendingResult.getTask());

                TaskExecutor taskExecutor = pendingResult.getTaskExecutor();
                taskExecutor.postResultNow(target, pendingResult.getResult(), pendingResult.getTask());
            }

            pendingResults.clear();
        }

        public static synchronized void putPendingResult(TaskCacheFragmentInterface cacheFragment, TaskPendingResult pendingResult) {
            List<TaskPendingResult> list = cacheFragment.get(PENDING_RESULT_KEY);
            if (list == null) {
                list = Collections.synchronizedList(new ArrayList<TaskPendingResult>());
                cacheFragment.put(PENDING_RESULT_KEY, list);
            }

            list.add(pendingResult);
        }

        public static void putTempCacheFragment(Activity activity, TaskCacheFragmentInterface cacheFragment) {
            TEMP_FRAG_CACHE.put(activity.hashCode(), new WeakReference<>(cacheFragment));
        }

        public static TaskCacheFragmentInterface getTempCacheFragment(Activity activity) {
            WeakReference<TaskCacheFragmentInterface> weakReference = TEMP_FRAG_CACHE.get(activity.hashCode());
            return weakReference == null ? null : weakReference.get();
        }
    }
}
