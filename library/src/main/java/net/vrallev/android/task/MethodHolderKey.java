package net.vrallev.android.task;

import android.support.v4.util.Pools;

/**
 * @author rwondratschek
 */
/*package*/ final class MethodHolderKey {

    private static final Pools.SynchronizedPool<MethodHolderKey> POOL = new Pools.SynchronizedPool<>(20);

    public static MethodHolderKey obtain(Class<?> target, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
        MethodHolderKey instance = POOL.acquire();
        if (instance == null) {
            instance = new MethodHolderKey();
        }

        instance.init(target, resultType, annotation, task.getAnnotationId(), task.getClass());

        return instance;
    }

    private Class<?> mTarget;
    private Class<?> mResultType;
    private Class<? extends TaskResult> mAnnotation;
    private String mAnnotationId;
    private Class<? extends Task> mTaskClass;

    private MethodHolderKey() {
        // no op
    }

    private void init(Class<?> target, Class<?> resultType, Class<? extends TaskResult> annotation, String annotationId, Class<? extends Task> taskClass) {
        mTarget = target;
        mResultType = resultType;
        mAnnotation = annotation;
        mAnnotationId = annotationId;
        mTaskClass = taskClass;
    }

    public void recycle() {
        POOL.release(this);
    }

    public Class<?> getTarget() {
        return mTarget;
    }

    public Class<?> getResultType() {
        return mResultType;
    }

    public Class<? extends TaskResult> getAnnotation() {
        return mAnnotation;
    }

    public String getAnnotationId() {
        return mAnnotationId;
    }

    public Class<? extends Task> getTaskClass() {
        return mTaskClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodHolderKey that = (MethodHolderKey) o;

        if (mTarget != null ? !mTarget.equals(that.mTarget) : that.mTarget != null) return false;
        if (mResultType != null ? !mResultType.equals(that.mResultType) : that.mResultType != null)
            return false;
        if (mAnnotation != null ? !mAnnotation.equals(that.mAnnotation) : that.mAnnotation != null)
            return false;
        //noinspection SimplifiableIfStatement
        if (mAnnotationId != null ? !mAnnotationId.equals(that.mAnnotationId) : that.mAnnotationId != null)
            return false;
        return !(mTaskClass != null ? !mTaskClass.equals(that.mTaskClass) : that.mTaskClass != null);

    }

    @Override
    public int hashCode() {
        int result = mTarget != null ? mTarget.hashCode() : 0;
        result = 31 * result + (mResultType != null ? mResultType.hashCode() : 0);
        result = 31 * result + (mAnnotation != null ? mAnnotation.hashCode() : 0);
        result = 31 * result + (mAnnotationId != null ? mAnnotationId.hashCode() : 0);
        result = 31 * result + (mTaskClass != null ? mTaskClass.hashCode() : 0);
        return result;
    }
}
