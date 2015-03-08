package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
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
}
