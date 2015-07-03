package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * @author rwondratschek
 */
@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public abstract class Task<RESULT> {

    protected abstract RESULT execute();

    private final CountDownLatch mCountDownLatch;

    private volatile boolean mCancelled;
    private volatile boolean mFinished;

    private int mKey = -1;
    private TaskExecutor mTaskExecutor;
    private WeakReference<TaskCacheFragmentInterface> mCacheFragment;
    private String mAnnotationId;
    private String mFragmentId;

    private RESULT mResult;

    public Task() {
        mCountDownLatch = new CountDownLatch(1);
    }

    /*package*/ final void setKey(int key) {
        mKey = key;
    }

    /*package*/ final void setTaskExecutor(TaskExecutor taskExecutor) {
        mTaskExecutor = taskExecutor;
    }

    /*package*/ final void setCacheFragment(TaskCacheFragmentInterface cacheFragment) {
        mCacheFragment = new WeakReference<>(cacheFragment);
    }

    /*package*/ final void setAnnotationId(String annotationId) {
        mAnnotationId = annotationId;
    }

    /*package*/ final String getAnnotationId() {
        return mAnnotationId;
    }

    /*package*/ final void setFragmentId(String fragmentId) {
        mFragmentId = fragmentId;
    }

    /*package*/ final String getFragmentId() {
        return mFragmentId;
    }

    /*package*/ final RESULT executeInner() {
        mResult = execute();
        mCountDownLatch.countDown();
        return mResult;
    }

    /*package*/ final void setFinished() {
        mFinished = true;
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

    public RESULT getResult() throws InterruptedException {
        mCountDownLatch.await();
        return mResult;
    }

    public final boolean isExecuting() {
        return mCountDownLatch.getCount() > 0;
    }

    public final boolean isFinished() {
        return mFinished;
    }

    protected Class<RESULT> getResultClass() {
        return null;
    }

    protected final Activity getActivity() {
        TaskCacheFragmentInterface fragment = mCacheFragment.get();
        if (fragment != null) {
            return fragment.getParentActivity();
        } else {
            return null;
        }
    }

    protected final Fragment getFragment() {
        if (mFragmentId == null) {
            return null;
        }
        Activity baseActivity = getActivity();
        if (!(baseActivity instanceof FragmentActivity)) {
            return null;
        }

        return findFragment(((FragmentActivity) baseActivity).getSupportFragmentManager());
    }

    private Fragment findFragment(FragmentManager manager) {
        List<Fragment> fragments = manager.getFragments();
        if (fragments == null) {
            return null;
        }

        for (Fragment fragment : fragments) {
            if (fragment == null) {
                continue;
            }
            String fragmentId = FragmentIdHelper.getFragmentId(fragment);
            if (mFragmentId.equals(fragmentId)) {
                return fragment;
            }

            if (fragment.getChildFragmentManager() != null) {
                Fragment child = findFragment(fragment.getChildFragmentManager());
                if (child != null) {
                    return child;
                }
            }
        }

        return null;
    }

    @Deprecated
    protected final Fragment findFragmentSupport(String tag) {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity) activity).getSupportFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    @Deprecated
    protected final Fragment findFragmentSupport(int id) {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity) activity).getSupportFragmentManager().findFragmentById(id);
        } else {
            return null;
        }
    }

    @Deprecated
    protected final android.app.Fragment findFragment(String tag) {
        Activity activity = getActivity();
        if (activity != null) {
            return activity.getFragmentManager().findFragmentByTag(tag);
        } else {
            return null;
        }
    }

    @Deprecated
    protected final android.app.Fragment findFragment(int id) {
        Activity activity = getActivity();
        if (activity != null) {
            return activity.getFragmentManager().findFragmentById(id);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s{mKey=%d, executing=%b, finished=%b, cancelled=%b",
                getClass().getSimpleName(), mKey, isExecuting(), isFinished(), isCancelled());
    }
}
