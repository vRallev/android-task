package net.vrallev.android.task;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author rwondratschek
 */
/*package*/ final class FragmentHack {

  private FragmentHack() {
    // no op
  }

  private static final Handler HANDLER = new Handler(Looper.getMainLooper());

  public static FragmentManager getFragmentManager(final FragmentActivity activity) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      try {
        return activity.getSupportFragmentManager();
      } catch (Exception e) {
        return null;
      }
    } else {

      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<FragmentManager> reference = new AtomicReference<>();

      HANDLER.post(new Runnable() {
        @Override
        public void run() {
          reference.set(getFragmentManager(activity));
          latch.countDown();
        }
      });

      try {
        latch.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException ignored) {
      }

      return reference.get();
    }
  }

  public static FragmentManager getChildFragmentManager(final Fragment fragment) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      try {
        return fragment.getChildFragmentManager();
      } catch (Exception e) {
        return null;
      }
    } else {

      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<FragmentManager> reference = new AtomicReference<>();

      HANDLER.post(new Runnable() {
        @Override
        public void run() {
          reference.set(getChildFragmentManager(fragment));
          latch.countDown();
        }
      });

      try {
        latch.await(3, TimeUnit.SECONDS);
      } catch (InterruptedException ignored) {
      }

      return reference.get();
    }
  }
}
