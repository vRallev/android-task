## 1.0.5 (2015-07-03)

- add a task class not invoking any callback
- performance improvements (caching target methods and return types)

## 1.0.4 (2015-05-18)

Bugfixes:

  - TargetMethodFinder checks methods before returned result to avoid conflicts with interface classes
  - TargetMethodFinder generates an ID for a Fragment to avoid conflicts with multiple instances from the same class

## 1.0.3 (2015-03-12)

- add Annotation ID for concrete callback method
- add Task reference in callback method
- add option to resolve all pending tasks
- bug fixes

## 1.0.2 (2015-03-08)

Bugfixes:

  - publish results even when the Activity was killed and the cache Fragment destroyed