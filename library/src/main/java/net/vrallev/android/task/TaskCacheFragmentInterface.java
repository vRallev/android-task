package net.vrallev.android.task;

import android.app.Activity;

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

    public Activity getActivity();
}
