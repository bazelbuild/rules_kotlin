# Kotlin Bazel Roadmap

This document describes the major release milestones for the Kotlin Bazel Rules.
There are three major pillars that we are focused on when developing the Kotlin
rules - **Performance**, **Features**, and **Developer Experience** - and for
each milestone we list the main items for each pillar. Progress on each item is
tracked via an issue.

If you have feedback on this roadmap (including feature and reprioritization
requests) please open an issue or comment on the existing one.

## Kotlin 1.3 (est. mid 2019)

The existing Kotlin rules only support up to version 1.2. The primary goal of
this release is to bring preliminary 1.3 support to Bazel. We will seek to
provide a migration path for users of the existing rules, with the intention of
1.3 eventually becoming the master branch. This includes documenting the
differences between the rulesets, and providing migration tooling and support.

### Performance

*   Compilation avoidance for non-structural changes to dependencies

### Features

*   Support android_instrumentation_test on Linux and macOS
*   Support building and testing on Google Cloud Platform Remote Build Execution
*   Support for ktlint
*   Simplified package and dependency management
*   Improve Android interoperability

### Developer Experience

*   Document major differences between the rulesets
*   Documentation for Kotlin with Bazel compatibility across Windows, macOS,
    Linux
*   Stable and reliable CI
*   Sample projects

## XPlat (est. late 2019)

The goal for the XPlat release is to provide a stable cross-platform (XPlat)
experience for developers. We intend to provide first class Kotlin/Native
support for Android and iOS, and collaborate with the community to add
additional target platforms. We also plan to deliver performance improvements
for build speed and binary size.

### Performance

*   Implement persistent workers for faster compilation
*   Reduce output binary sizes

### Features

*   Stable Kotlin/Native support for Android and iOS
*   Support `bazel coverage` for all test rules
*   Support for Android Lint

### Developer Experience

*   Documentation and guides for writing a cross platform app
