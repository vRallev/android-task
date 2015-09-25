package net.vrallev.android.task;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

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

    private final Object mMonitor;
    private final CountDownLatch mCountDownLatch;

    private volatile boolean mCancelled;
    private volatile boolean mFinished;

    private int mKey = -1;
    private TaskExecutor mTaskExecutor;
    private Application mApplication;
    private WeakReference<Activity> mCachedActivity;
    private String mAnnotationId;
    private String mFragmentId;

    private RESULT mResult;

    public Task() {
        mCountDownLatch = new CountDownLatch(1);
        mMonitor = new Object();
    }

    /*package*/ final void setKey(int key) {
        synchronized (mMonitor) {
            mKey = key;
        }
    }

    /*package*/ final void setTaskExecutor(TaskExecutor taskExecutor) {
        synchronized (mMonitor) {
            mTaskExecutor = taskExecutor;
        }
    }

    /*package*/ final void setCachedActivity(Activity activity) {
        synchronized (mMonitor) {
            if (mApplication == null) {
                mApplication = activity.getApplication();
            }
            mCachedActivity = new WeakReference<>(activity);
        }
    }

    /*package*/ final void setAnnotationId(String annotationId) {
        synchronized (mMonitor) {
            mAnnotationId = annotationId;
        }
    }

    /*package*/ final String getAnnotationId() {
        synchronized (mMonitor) {
            return mAnnotationId;
        }
    }

    /*package*/ final void setFragmentId(String fragmentId) {
        synchronized (mMonitor) {
            mFragmentId = fragmentId;
        }
    }

    /*package*/ final String getFragmentId() {
        synchronized (mMonitor) {
            return mFragmentId;
        }
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
        synchronized (mMonitor) {
            return mKey;
        }
    }

    public final void cancel() {
        mCancelled = true;
    }

    public final boolean isCancelled() {
        return mCancelled || Thread.currentThread().isInterrupted();
    }

    public RESULT getResult() {
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            Log.e("Task", "Interruption while waiting for result", e);
        }
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
        return mCachedActivity.get();
    }

    protected final Context getApplicationContext() {
        return mApplication;
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

    @Override
    public String toString() {
        return String.format(Locale.US, "%s{mKey=%d, executing=%b, finished=%b, cancelled=%b",
                getClass().getSimpleName(), mKey, isExecuting(), isFinished(), isCancelled());
    }
}
