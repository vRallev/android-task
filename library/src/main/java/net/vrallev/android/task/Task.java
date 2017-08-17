package net.vrallev.android.task;

import android.annotation.SuppressLint;
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

    protected abstract RESULT execute();

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
        try {
            mResult = execute();
        } catch (Throwable t) {
            Log.e("Task", getClass().getName() + " crashed", t);
        }
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

    public final int start(Fragment callback) {
        return TaskExecutor.getInstance().execute(this, callback);
    }

    public final int start(Fragment callback, String annotationId) {
        return TaskExecutor.getInstance().execute(this, callback, annotationId);
    }

    public final int start(Activity callback) {
        return TaskExecutor.getInstance().execute(this, callback);
    }

    public final int start(Activity callback, String annotationId) {
        return TaskExecutor.getInstance().execute(this, callback, annotationId);
    }

    public final boolean replaceCallback(Fragment callback) {
        return replaceCallback(callback, null);
    }

    public final boolean replaceCallback(Fragment callback, String annotationId) {
        return mTaskExecutor.updateCallback(this, callback, annotationId);
    }

    public final boolean replaceCallback(Activity callback) {
        return replaceCallback(callback, null);
    }

    public final boolean replaceCallback(Activity callback, String annotationId) {
        return mTaskExecutor.updateCallback(this, callback, annotationId);
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

        return findFragment(FragmentHack.getFragmentManager((FragmentActivity) baseActivity));
    }

    @SuppressLint("RestrictedApi")
    private Fragment findFragment(FragmentManager manager) {
        if (manager == null) {
            return null;
        }

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

            if (FragmentHack.getChildFragmentManager(fragment) != null) {
                Fragment child = findFragment(FragmentHack.getChildFragmentManager(fragment));
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
