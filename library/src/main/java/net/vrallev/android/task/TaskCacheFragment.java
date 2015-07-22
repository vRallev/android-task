package net.vrallev.android.task;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
public final class TaskCacheFragment extends Fragment implements TaskCacheFragmentInterface {

    private static final String TAG = "TaskCacheFragment";

    /*package*/
    static TaskCacheFragment getFrom(Activity activity) {
        FragmentManager fragmentManager = activity.getFragmentManager();

        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment instanceof TaskCacheFragment) {
            return (TaskCacheFragment) fragment;
        }

        TaskCacheFragmentInterface cacheFragment = Helper.getTempCacheFragment(activity);
        if (cacheFragment instanceof TaskCacheFragment) {
            return (TaskCacheFragment) cacheFragment;
        }

        TaskCacheFragment result = new TaskCacheFragment();
        result.mActivity = activity;
        fragmentManager.beginTransaction()
                .add(result, TAG)
                .commitAllowingStateLoss();

        try {
            fragmentManager.executePendingTransactions();
        } catch (IllegalStateException ignored) {
            // may throw java.lang.IllegalStateException: Recursive entry to executePendingTransactions
            TaskCacheFragmentInterface.Helper.putTempCacheFragment(activity, result);
        }

        return result;
    }

    private final Map<String, Object> mCache;
    private boolean mCanSaveInstanceState;
    private Activity mActivity;

    public TaskCacheFragment() {
        setRetainInstance(true);

        mCache = Collections.synchronizedMap(new HashMap<String, Object>());
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCanSaveInstanceState = true;
    }

    @Override
    public void onStart() {
        super.onStart();

        mCanSaveInstanceState = true;

        List<TaskPendingResult> list = get(PENDING_RESULT_KEY);
        if (list != null && !list.isEmpty()) {
            TaskCacheFragmentInterface.Helper.postPendingResults(list, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCanSaveInstanceState = true;
    }

    @Override
    public void onStop() {
        mCanSaveInstanceState = false;
        super.onStop();
    }

    @Override
    public void onDetach() {
        if (mActivity.isFinishing()) {
            mActivity = null;
        }
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mCanSaveInstanceState = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean canSaveInstanceState() {
        return mCanSaveInstanceState;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T get(String key) {
        return (T) mCache.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T put(String key, Object object) {
        return (T) mCache.put(key, object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T remove(String key) {
        return (T) mCache.remove(key);
    }

    @Override
    public synchronized void putPendingResult(TaskPendingResult pendingResult) {
        List<TaskPendingResult> list = get(PENDING_RESULT_KEY);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<TaskPendingResult>());
            put(PENDING_RESULT_KEY, list);
        }

        list.add(pendingResult);
    }

    @Override
    public Activity getParentActivity() {
        return mActivity;
    }
}
