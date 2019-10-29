# Announcements and Notable Changes

|  Date&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | News  | 
| :----------- | -------- |
| Oct 29, 2019 | Released version [1.3.0-rc1](https://github.com/bazelbuild/rules_kotlin/releases/tag/legacy-1.3.0-rc1). | 
| Oct 28, 2019 | Fix various regressions (#217, #221, #222).  | 
| Oct 28, 2019 | Improve file handling for maven-packaged JS jars in `kt_js_import()` (#223).  | 
| Oct 15, 2019 | Infer the test class from the test name in `kt_jvm_unit_test()` (#209).  | 
| Oct 5, 2019  | [cgruber/rules_kotlin](github.com/cgruber/rules_kotlin) upstreamed into this repository.  | 
| Oct 2, 2019  | Fixes to bazel 1.0.0 (as of rc4). Release (cgruber) `legacy_modded-1_0_0-01` | 
| Oct 2, 2019  | NOTE: (cgruber) `legacy_modded-1_0_0-01` will be the last release on this fork, prior to upstreaming to github.com/bazelbuild/rules_kotlin.  For any further releases after this one, please look at that repository. | 
| Oct 2, 2019  | Fixes to bazel 1.0.0 (as of rc4). Release (cgruber) `legacy_modded-1_0_0-01` | 
| Sep 27, 2019 | Fixes to support latest versions and bring upstream in, release (cgruber) `legacy_modded-0_29_0-01` | 
| Jun 20, 2019 | Fix to support 0.27.0 and release of (cgruber) `legacy_modded-0_26_1-02`<br>Serious reconsideration of the version naming scheme... | 
| Jun 12, 2019 | Kotlin 1.3 support, and release of (cgruber) `legacy_modded-0_26_1-01` | 
| Jun 11, 2019 | Fix to kotlin worker to allow modern dagger to be used in kapt. (worker was leaking its dagger dep) | 
| May 17, 2019 | More changes from upstream (mostly fixes for bazel version bump) and releases from the fork | 
| Apr 1, 2019  | [Roadmap](https://github.com/bazelbuild/rules_kotlin/blob/master/ROADMAP.md) for rules_kotlin published by the Google team.  The `cgruber` fork will continue until the legacy branch in the parent repo can be updated (or no one needs the fork) | 
| Feb 20, 2019 | [Future directions](https://github.com/bazelbuild/rules_kotlin/issues/174) of rules_kotlin. | 
| Oct 24, 2018 | Christian Gruber forks the main kotlin rules repository, adding in two fixes. | 
| Aug 14, 2018 | Js support. No documentation yet but see the nested example workspace `examples/node`. | 
| Aug 14, 2018 | Android support. No documentation but it's a simple integration. see `kotlin/internal/jvm/android.bzl`. | 
| Jun 29, 2018 | The commits from this date forward are compatible with bazel `>=0.14`. JDK9 host issues were fixed as well some other deprecations. I recommend skipping `0.15.0` if you   are on a Mac.  | 
| May 25, 2018 | Test "friend" support. A single friend dep can be provided to `kt_jvm_test` which allows the test to access internal members of the module under test. | 
| Feb 15, 2018 | Toolchains for the JVM rules. Currently this allow tweaking The JVM target (bytecode level), API and Language levels. Coroutines, enabled by default. | 
| Feb 9, 2018  | Annotation processing (kapt) support
| Feb 5, 2018  | JVM rule name change:</b> the prefix has changed from `kotlin_` to `kt_jvm_`.

For fixes and more detail, please view the [list of pull requests](https://github.com/bazelbuild/rules_kotlin/pulls?q=is%3Apr+is%3Aclosed).
