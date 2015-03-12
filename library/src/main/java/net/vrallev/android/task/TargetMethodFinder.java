package net.vrallev.android.task;

import android.app.Activity;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author rwondratschek
 */
/*package*/ class TargetMethodFinder {

    private static final String TAG = "TargetMethodFinder";

    private final Class<? extends TaskResult> mAnnotation;

    public TargetMethodFinder(Class<? extends TaskResult> annotation) {
        mAnnotation = annotation;
    }

    public Class<?> getResultType(Object result, Task<?> task) {
        Class<?> resultType;
        if (result != null) {
            resultType = result.getClass();
        } else {
            resultType = findReturnType(task.getClass());
            if (resultType == null) {
                Log.w(TAG, "Couldn't find result type");
                return null;
            }
        }

        return resultType;
    }

    public Pair<Method, Object> getMethod(TaskCacheFragmentInterface cacheFragment, Class<?> resultType, Task<?> task) {
        Activity activity = cacheFragment.getParentActivity();
        if (activity == null) {
            Log.w(TAG, "Activity is null, can't find target");
            return null;
        }

        Pair<Method, Object> pair;
        if (activity instanceof FragmentActivity) {
            pair = findMethodInActivityAndFragments((FragmentActivity) activity, resultType, mAnnotation, task);
        } else {
            pair = findMethodInActivity(activity, activity.getClass(), resultType, mAnnotation, task);
        }

        if (pair == null) {
            Log.w(TAG, "Didn't find annotated method with correct result type");
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

    public void post(TaskCacheFragmentInterface cacheFragment, Object result, Task<?> task) {
        Pair<Method, Object> target = getMethod(cacheFragment, getResultType(result, task), task);
        if (target != null) {
            invoke(target, result, task);
        }
    }

    private static Pair<Method, Object> findMethodInActivityAndFragments(FragmentActivity activity, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
        Pair<Method, Object> pair = findMethodInActivity(activity, activity.getClass(), resultType, annotation, task);
        if (pair != null) {
            return pair;
        }

        return findMethodInFragmentManager(activity.getSupportFragmentManager(), resultType, annotation, task);
    }

    private static Pair<Method, Object> findMethodInActivity(Activity activity, Class<?> target, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
        if (target.equals(FragmentActivity.class) || target.equals(Activity.class)) {
            return null;
        }

        Method method = findMethodInClass(target, resultType, annotation, task);
        if (method != null) {
            return new Pair<>(method, (Object) activity);
        }

        return findMethodInActivity(activity, target.getSuperclass(), resultType, annotation, task);
    }

    private static Pair<Method, Object> findMethodInFragment(Fragment fragment, Class<?> target, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
        if (target.equals(Fragment.class) || target.equals(DialogFragment.class)) {
            return null;
        }

        Method method = findMethodInClass(target, resultType, annotation, task);
        if (method != null) {
            return new Pair<>(method, (Object) fragment);
        }

        Pair<Method, Object> pair = findMethodInFragment(fragment, target.getSuperclass(), resultType, annotation, task);
        if (pair != null) {
            return pair;
        }

        return findMethodInFragmentManager(fragment.getChildFragmentManager(), resultType, annotation, task);
    }

    private static Pair<Method, Object> findMethodInFragmentManager(FragmentManager fragmentManager, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
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
            Pair<Method, Object> pair = findMethodInFragment(childFragment, childFragment.getClass(), resultType, annotation, task);
            if (pair != null) {
                return pair;
            }
        }

        return null;
    }

    private static Method findMethodInClass(Class<?> target, Class<?> resultType, Class<? extends TaskResult> annotation, Task<?> task) {
        Method[] declaredMethods;
        try {
            declaredMethods = target.getDeclaredMethods();
        } catch (Error e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

        final String annotationId = task.getAnnotationId();
        final boolean useAnnotationId = !TextUtils.isEmpty(annotationId);

        if (declaredMethods != null) {
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
                if (!parameterTypes[0].equals(resultType)) {
                    continue;
                }

                if (parameterTypes.length == 2 && !parameterTypes[1].isInstance(task)) {
                    continue;
                }

                return method;
            }
        }

        return null;
    }

    private static Class<?> findReturnType(Class<?> taskClass) {
        if (taskClass.equals(Object.class) || taskClass.equals(Task.class)) {
            return null;
        }

        for (Method method : taskClass.getDeclaredMethods()) {
            if ("execute".equals(method.getName()) && method.getParameterTypes() != null && method.getParameterTypes().length == 1) {
                return method.getReturnType();
            }
        }

        return findReturnType(taskClass.getSuperclass());
    }
}
