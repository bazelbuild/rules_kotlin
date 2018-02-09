# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
workspace(name="io_bazel_rules_kotlin")

git_repository(
    name = "io_bazel_rules_sass",
    remote = "https://github.com/bazelbuild/rules_sass.git",
    tag = "0.0.3",
)
load("@io_bazel_rules_sass//sass:sass.bzl", "sass_repositories")
sass_repositories()

git_repository(
    name = "io_bazel_skydoc",
    remote = "https://github.com/bazelbuild/skydoc.git",
    commit = "b36d22c"
)
load("@io_bazel_skydoc//skylark:skylark.bzl", "skydoc_repositories")
skydoc_repositories()

load("//kotlin:kotlin.bzl", "kotlin_repositories")
kotlin_repositories()

# test and example dependencies.
maven_jar(name = "junit_junit",artifact = "junit:junit:jar:4.12")
maven_jar(name = "autovalue", artifact="com.google.auto.value:auto-value:1.5")
maven_jar(name = "guava", artifact="com.google.guava:guava:24.0-jre")
maven_jar(name = "auto_common", artifact="com.google.auto:auto-common:0.10")
maven_jar(name = "autoservice", artifact="com.google.auto.service:auto-service:1.0-rc4")
maven_jar(name = "javax_inject", artifact = "javax.inject:javax.inject:1")
# After 2.9 dagger requires much more dependencies.
maven_jar(name = "dagger", artifact="com.google.dagger:dagger:2.9")
maven_jar(name = "dagger_compiler", artifact="com.google.dagger:dagger-compiler:2.9")
maven_jar(name = "dagger_producers", artifact="com.google.dagger:dagger-producers:2.9")
