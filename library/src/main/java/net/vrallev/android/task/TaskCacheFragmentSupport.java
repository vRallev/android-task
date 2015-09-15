package net.vrallev.android.task;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
public final class TaskCacheFragmentSupport extends Fragment implements TaskCacheFragmentInterface {

    private static final String TAG = "TaskCacheFragmentSupport";

    /*package*/
    static TaskCacheFragmentSupport getFrom(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment instanceof TaskCacheFragmentSupport) {
            return (TaskCacheFragmentSupport) fragment;
        }

        TaskCacheFragmentInterface cacheFragment = Helper.getTempCacheFragment(activity);
        if (cacheFragment instanceof TaskCacheFragmentSupport) {
            return (TaskCacheFragmentSupport) cacheFragment;
        }

        TaskCacheFragmentSupport result = new TaskCacheFragmentSupport();
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
    private volatile boolean mCanSaveInstanceState;
    private Activity mActivity;

    public TaskCacheFragmentSupport() {
        setRetainInstance(true);

        mCache = Collections.synchronizedMap(new HashMap<String, Object>());
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCanSaveInstanceState = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        mCanSaveInstanceState = true;
        TaskCacheFragmentInterface.Helper.postPendingResults(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCanSaveInstanceState = true;
        TaskCacheFragmentInterface.Helper.postPendingResults(this);
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
    public synchronized <T> T put(String key, Object value) {
        return (T) mCache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T> T remove(String key) {
        return (T) mCache.remove(key);
    }

    @Override
    public Activity getParentActivity() {
        return mActivity;
    }
}
