Android-Task
============

An utility library for Android to run actions in background. It handles orientation changes and delegates results to the visible `Activity`.

Download
--------

Download [the latest version][1] or grab via Gradle:

```groovy
dependencies {
    compile 'net.vrallev.android:android-task:1.0.6'
}
```

Usage
-----

The class `TaskExecutor` serves as entry point. Your background actions need to extend the class `Task`. Your callback method needs be annotated with `TaskResult` and has to accept exactly one result object, which the `Task` returns. You may want to take a look at the [demo][2].

```java
public class MyActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Task myTask = new MyTask();
		int taskId = TaskExecutor.getInstance().execute(myTask, this);
	}

	@TaskResult
	public void onResult(Integer result) {
		// handle result, this method gets called on the UI thread and only if the activity is visible
	}
}

public class MyTask extends Task<Integer> {
	
	@Override
	protected Integer execute() {
		return 5;
	}
}
```

Advanced
--------

You can create your own `TaskExecutor` instance, if you want to change the behavior. 

```java
new Builder()
	.setExecutorService(Executors.newSingleThreadExecutor()) // default CachedThreadPool
	.setPostResult(PostResult.ON_ANY_THREAD) // default PostResult.UI_THREAD
	.build()
	.asSingleton();
```

The `TaskExecutor` returns an ID. You can restore this ID in `onCreate(Bundle savedInstanceState` to find your `Task`.

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
        int taskId = savedInstanceState.getInt(TASK_ID_KEY, -1);
        MyTask task = TaskExecutor.getInstance().getTask(taskId);
    }
}
```

A callback method can have a second parameter to get the specific `Task` instance.

```java
@TaskResult
public void onResult(Integer result, MyTask task) {
	// handle result, this method gets called on the UI thread and only if the activity is visible
}
```

You can annotate the callback method with a concrete ID, if you want to reuse a `Task` but provide different callbacks. **Attention:** If your callback method has an ID, then a `Task` submitted without an ID won't find this callback method.

```java
public void startTask() {
	Task myTask = new MyTask();
	TaskExecutor.getInstance().execute(myTask, this, "my_id");
}

@TaskResult(id = "my_id")
public void onResult(Integer result, MyTask task) {
	// handle result, this method gets called on the UI thread and only if the activity is visible
}
```

If you want to submit a `Task`, which should not return any result and shouldn't invoke any callback method, then extend `TaskNoCallback`. Compared to a normal `Thread` the purpose of this class is that you still have access to the `Context` and can find the `Task` in the `TaskExecutor`.

```java
public class SimpleNoCallbackTask extends TaskNoCallback {
    @Override
    protected void executeTask() {
        // do anything
    }
}
```

How it works
------------

When you submit a task, a `Fragment` gets attached to the `Activity`. The `Fragment` instance is retained and caches information and results. When a `Task` finished, the `TaskExecutor` searches the callback method and invokes it on the visible `Activity` or `Fragment`. 

UI components are only referenced with a `WeakReference` to avoid memory leaks. If a callback method can't be found or the `Activity` was already cleared, then the result from the `Task` is dropped.

Limitations
-----------

Your class, which can receive callbacks, must be an instance of `Activity`, `FragmentActivity` or `Fragment` **from the support library**.

The library uses reflection to find your callback method at runtime. This costs time, but classes are scanned in the background.



License
-------

    Copyright 2015 Ralf Wondratschek

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: http://search.maven.org/#search|gav|1|g:"net.vrallev.android"%20AND%20a:"android-task"
[2]: https://github.com/vRallev/SQRL-Protocol/tree/master/android-sdk/src/main/java/net/vrallev/android/task/demo