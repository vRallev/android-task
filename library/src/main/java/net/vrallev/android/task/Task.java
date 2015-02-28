package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.lang.ref.WeakReference;

/**
 * @author rwondratschek
 */
@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public abstract class Task<RESULT> {

    protected abstract RESULT execute();

    private volatile boolean mCancelled;

    private int mKey = -1;
    private TaskExecutor mTaskExecutor;
    private WeakReference<TaskCacheFragmentInterface> mCacheFragment;

    /*package*/ final void setKey(int key) {
        mKey = key;
    }

    /*package*/ final void setTaskExecutor(TaskExecutor taskExecutor) {
        mTaskExecutor = taskExecutor;
    }

    /*package*/ final void setCacheFragment(TaskCacheFragmentInterface cacheFragment) {
        mCacheFragment = new WeakReference<>(cacheFragment);
    }

    public final int getKey() {
        return mKey;
    }

    public final void cancel() {
        mCancelled = true;
    }

    public final boolean isCancelled() {
        return mCancelled || Thread.currentThread().isInterrupted();
    }

    protected final Activity getActivity() {
        TaskCacheFragmentInterface fragment = mCacheFragment.get();
        if (fragment != null) {
            return fragment.getParentActivity();
        } else {
            return null;
        }
    }

    protected final Fragment findFragmentSupport(String tag) {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity) activity).getSupportFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    protected final Fragment findFragmentSupport(int id) {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity) activity).getSupportFragmentManager().findFragmentById(id);
        } else {
            return null;
        }
    }

    protected final android.app.Fragment findFragment(String tag) {
        Activity activity = getActivity();
        if (activity != null) {
            return activity.getFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    protected final android.app.Fragment findFragment(int id) {
        Activity activity = getActivity();
        if (activity != null) {
            return activity.getFragmentManager().findFragmentById(id);
        } else {
            return null;
        }
    }
}
