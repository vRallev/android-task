package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author rwondratschek
 */
/*package*/
@SuppressWarnings("unused")
class TargetMethodFinder {

    private static final String TAG = "TargetMethodFinder";

    private static final LruCache<Class<?>, Class<?>> CACHE_RETURN_TYPE = new LruCache<Class<?>, Class<?>>(15) {
        @Override
        protected Class<?> create(Class<?> taskClass) {
            return findReturnType(taskClass);
        }
    };

    private static final LruCache<MethodHolderKey, MethodHolder> CACHE_METHOD = new LruCache<MethodHolderKey, MethodHolder>(25) {
        @Override
        protected MethodHolder create(MethodHolderKey key) {
            return MethodHolder.obtain(findMethodInClass(key));
        }

        @Override
        protected void entryRemoved(boolean evicted, MethodHolderKey key, MethodHolder oldValue, MethodHolder newValue) {
            key.recycle();
            if (oldValue != null) {
                oldValue.recycle();
            }
        }
    };

    private final Class<? extends TaskResult> mAnnotation;

    public TargetMethodFinder(Class<? extends TaskResult> annotation) {
        mAnnotation = annotation;
    }

    public Class<?> getResultType(Object result, Task<?> task) {
        Class<?> resultType = task.getResultClass();
        if (resultType == null) {
            resultType = CACHE_RETURN_TYPE.get(task.getClass());
        }
        if (resultType == null && result != null) {
            resultType = result.getClass();
        }
        if (resultType == null) {
            Log.w(TAG, "Couldn't find result type");
        }

        return resultType;
    }

    public Pair<Method, Object> getMethod(Activity activity, Class<?> resultType, Task<?> task) {
        if (activity == null) {
            Log.w(TAG, "Activity is null, can't find target");
            return null;
        }

        Pair<Method, Object> pair;
        if (activity instanceof FragmentActivity) {
            pair = findMethodInActivityAndFragments((FragmentActivity) activity, resultType, mAnnotation, task, true);
            if (pair == null) {
                pair = findMethodInActivityAndFragments((FragmentActivity) activity, resultType, mAnnotation, task, false);
            }

        } else {
            pair = findMethodInActivity(activity, activity.getClass(), resultType, mAnnotation, task);
        }

        if (pair == null) {
            Object result = task.getResult();
            Log.w(TAG, String.format("Didn't find method, result type %s, result %s, annotationId %s, fragmentId %s",
                    resultType, result, task.getAnnotationId(), task.getFragmentId()));
        }
        return pair;
    }

    public void invoke(Pair<Method, Object> target, Object result, Task<?> task) {
        invoke(target.first, target.second, result, task);
    }

    public void invoke(Method method, Object target, Object result, Task<?> task) {
        // not sure why, but Lint doesn't like this collapse
        //noinspection TryWithIdenticalCatches
        try {
            if (method.getParameterTypes().length == 2) {
                method.invoke(target, result, task);
            } else {
                method.invoke(target, result);
            }

        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage(), e);

        } catch (InvocationTargetException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /*
    public void post(TaskCacheFragmentInterface cacheFragment, Object result, Task<?> task) {
        Pair<Method, Object> target = getMethod(cacheFragment, getResultType(result, task), task);
        if (target != null) {
            invoke(target, result, task);
        }
    }
    */

    private static Pair<Method, Object> findMethodInActivityAndFragments(FragmentActivity activity, Class<?> resultType,
                                                                         Class<? extends TaskResult> annotation, Task<?> task, boolean compareFragmentIndex) {

        Pair<Method, Object> pair = findMethodInActivity(activity, activity.getClass(), resultType, annotation, task);
        if (pair != null) {
            return pair;
        }

        return findMethodInFragmentManager(activity.getSupportFragmentManager(), resultType, annotation, task, compareFragmentIndex);
    }

    private static Pair<Method, Object> findMethodInActivity(Activity activity, Class<?> target, Class<?> resultType,
                                                             Class<? extends TaskResult> annotation, Task<?> task) {

        if (target.equals(FragmentActivity.class) || target.equals(Activity.class)) {
            return null;
        }

        Method method = findMethodInClass(target, resultType, annotation, task);
        if (method != null) {
            return new Pair<>(method, (Object) activity);
        }

        return findMethodInActivity(activity, target.getSuperclass(), resultType, annotation, task);
    }

    private static Pair<Method, Object> findMethodInFragment(Fragment fragment, Class<?> target, Class<?> resultType,
                                                             Class<? extends TaskResult> annotation, Task<?> task, boolean compareFragmentIndex) {

        if (target.equals(Fragment.class) || target.equals(DialogFragment.class)) {
            return null;
        }

        final String fragmentId = task.getFragmentId();
        final boolean useFragmentId = !TextUtils.isEmpty(fragmentId);

        if (!useFragmentId || FragmentIdHelper.equals(fragmentId, FragmentIdHelper.getFragmentId(fragment), compareFragmentIndex)) {
            Method method = findMethodInClass(target, resultType, annotation, task);
            if (method != null) {
                return new Pair<>(method, (Object) fragment);
            }

            Pair<Method, Object> pair = findMethodInFragment(fragment, target.getSuperclass(), resultType, annotation, task, compareFragmentIndex);
            if (pair != null) {
                return pair;
            }
        }

        return findMethodInFragmentManager(fragment.getChildFragmentManager(), resultType, annotation, task, compareFragmentIndex);
    }

    private static Pair<Method, Object> findMethodInFragmentManager(FragmentManager fragmentManager, Class<?> resultType,
                                                                    Class<? extends TaskResult> annotation, Task<?> task, boolean compareFragmentIndex) {

        if (fragmentManager == null) {
            return null;
        }

        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments == null) {
            return null;
        }

        for (Fragment childFragment : fragments) {
            if (childFragment == null) {
                continue;
            }
            Pair<Method, Object> pair = findMethodInFragment(childFragment, childFragment.getClass(), resultType, annotation, task, compareFragmentIndex);
            if (pair != null) {
                return pair;
            }
        }

        return null;
    }

    private static Method findMethodInClass(Class<?> target, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
        if (resultType == null) {
            return null;
        }

        MethodHolderKey methodHolderKey = MethodHolderKey.obtain(target, resultType, annotation, task);
        return CACHE_METHOD.get(methodHolderKey).getMethod();
    }

    private static Method findMethodInClass(MethodHolderKey methodHolderKey) {
        Class<?> target = methodHolderKey.getTarget();
        Class<? extends TaskResult> annotation = methodHolderKey.getAnnotation();
        Class<?> resultType = methodHolderKey.getResultType();
        Class<? extends Task> taskClass = methodHolderKey.getTaskClass();

        final String annotationId = methodHolderKey.getAnnotationId();
        final boolean useAnnotationId = !TextUtils.isEmpty(annotationId);

        Method[] declaredMethods;
        try {
            declaredMethods = target.getDeclaredMethods();
        } catch (Error e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

        if (declaredMethods == null) {
            return null;
        }

        Method candidate = null;

        for (Method method : declaredMethods) {
            if (!method.isAnnotationPresent(annotation)) {
                continue;
            }
            if (useAnnotationId && !annotationId.equals(method.getAnnotation(annotation).id())) {
                continue;
            }
            if (!useAnnotationId && !TextUtils.isEmpty(method.getAnnotation(annotation).id())) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0 || parameterTypes.length > 2) {
                continue;
            }
            if (parameterTypes[0].isAssignableFrom(resultType)) {
                if (candidate == null) {
                    candidate = method;
                } else {
                    Log.w(TAG, "Found another method, which is ignored " + method.getName());
                }
            }

            if (!parameterTypes[0].equals(resultType)) {
                continue;
            }

            if (parameterTypes.length == 2 && !parameterTypes[1].isAssignableFrom(taskClass)) {
                continue;
            }

            return method;
        }

        return candidate;
    }

    private static Class<?> findReturnType(Class<?> taskClass) {
        if (taskClass.equals(Object.class) || taskClass.equals(Task.class)) {
            return null;
        }

        for (Method method : taskClass.getDeclaredMethods()) {
            String name = method.getName();
            if (!"execute".equals(name)) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes == null || parameterTypes.length != 0) {
                continue;
            }

            Class<?> returnType = method.getReturnType();
            if (!Object.class.equals(returnType)) {
                return returnType;
            }
        }

        return findReturnType(taskClass.getSuperclass());
    }
}
