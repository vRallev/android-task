package net.vrallev.android.task;

import android.support.v4.util.Pools;

import java.lang.reflect.Method;

/**
 * @author rwondratschek
 */
/*package*/ final class MethodHolder {

    private static final Pools.SynchronizedPool<MethodHolder> POOL = new Pools.SynchronizedPool<>(30);

    private static final MethodHolder NULL_METHOD_HOLDER = new MethodHolder();

    public static MethodHolder obtain(Method method) {
        if (method == null) {
            return NULL_METHOD_HOLDER;
        }

        MethodHolder methodHolder = POOL.acquire();
        if (methodHolder == null) {
            methodHolder = new MethodHolder();
        }

        methodHolder.init(method);
        return methodHolder;
    }

    private Method mMethod;

    private MethodHolder() {
    }

    private void init(Method method) {
        mMethod = method;
    }

    public Method getMethod() {
        return mMethod;
    }

    public void recycle() {
        try {
            POOL.release(this);
        } catch (Exception e) {
            // seeing "IllegalStateException: Already in the pool!" from time to time, then don't recycle this instance
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }

        MethodHolder that = (MethodHolder) o;

        return !(mMethod != null ? !mMethod.equals(that.mMethod) : that.mMethod != null);

    }

    @Override
    public int hashCode() {
        return mMethod != null ? mMethod.hashCode() : 0;
    }
}
