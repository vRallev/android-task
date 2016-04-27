## 1.1.5 (2016-04-27)

- Fix bug where callback method is getting invoked with the wrong target

## 1.1.4 (2016-03-29)

- Add option to replace callback context with explicit annotation

## 1.1.3 (2016-02-26)

- Add option to replace callback context

## 1.1.2 (2016-02-22)

- Bugfix

## 1.1.1 (2016-01-07)

- Catch crash while recycling some cached objects

## 1.1.0 (2015-09-25)

- Major refactoring to handle activity state easier (remove fragments)
- Fix bug where callback never was invoked if the activity was recreated, but the old was still cached
- Remove deprecated methods  

## 1.0.8 (2015-09-15)

Bugfixes:

  - Deliver pending results in onResume() as well 

## 1.0.7 (2015-07-22)

Bugfixes:

  - Catch IllegalStateException thrown while attaching the CacheFragment during a fragment transaction
    (e.g. start a Task in onCreateView() of a Fragment in a ViewPager)

## 1.0.6 (2015-07-07)

Bugfixes:

  - After opening a translucent activity the CacheFragment returns a wrong visibility state

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