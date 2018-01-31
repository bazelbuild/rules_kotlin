[Skydoc documentation](https://bazelbuild.github.io/rules_kotlin)

# Overview

These rules were initially forked from [pubref/rules_kotlin](http://github.com/pubref/rules_kotlin). Key changes:

* Replace the macros with three basic rules. `kotlin_binary`, `kotlin_library` and `kotlin_test`.
* Use a single dep attribute instead of `java_dep` and `dep`.
* Add support for the following standard java rules attributes:
  * `data`
  * `resource_jars`
  * `runtime_deps`
  * `resources`
  * `resources_strip_prefix`
  * `exports`
* Persistent worker support.
* Mixed-Mode compilation (compile Java and Kotlin in one pass).