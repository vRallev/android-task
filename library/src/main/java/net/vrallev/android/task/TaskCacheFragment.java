package net.vrallev.android.task;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Pair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author rwondratschek
 */
@SuppressWarnings("UnusedDeclaration")
public final class TaskCacheFragment extends Fragment implements TaskCacheFragmentInterface {

    private static final String TAG = "TaskCacheFragment";
    private final Map<String, Object> mCache;
    private boolean mCanSaveInstanceState;

    public TaskCacheFragment() {
        setRetainInstance(true);

        mCache = Collections.synchronizedMap(new HashMap<String, Object>());
    }

    /*package*/ static TaskCacheFragment getFrom(Activity activity) {
        TaskCacheFragment result;

        FragmentManager fragmentManager = activity.getFragmentManager();

        Fragment fragment = fragmentManager.findFragmentByTag(TAG);
        if (fragment instanceof TaskCacheFragment) {
            result = (TaskCacheFragment) fragment;
        } else {
            result = new TaskCacheFragment();
            fragmentManager.beginTransaction()
                .add(result, TAG)
                .commitAllowingStateLoss();
        }

        return result;
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
        if (list != null) {
            Iterator<TaskPendingResult> iterator = list.iterator();
            TargetMethodFinder targetMethodFinder = new TargetMethodFinder(TaskResult.class);

            while (iterator.hasNext()) {
                TaskPendingResult pendingResult = iterator.next();
                iterator.remove();

                Pair<Method, Object> target = targetMethodFinder.getMethod(this, pendingResult.getResultType());
                targetMethodFinder.invoke(target, pendingResult.getResult());
            }
        }
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
}
