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
load("//kotlin/internal:bootstrap.bzl",github_archive="github_archive")

github_archive(
    name = "com_google_protobuf",
    repo = "google/protobuf",
    commit = "106ffc04be1abf3ff3399f54ccf149815b287dd9",
)

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
    tag = "0.1.4"
)
load("@io_bazel_skydoc//skylark:skylark.bzl", "skydoc_repositories")
skydoc_repositories()

http_jar(
    name = "bazel_deps",
    url = "https://github.com/axsy-dev/bazel-deps/releases/download/2/bazel-deps.jar",
    sha256 = "bbd51188141f2bb09222a35675af25edbbb5b6507d779acb6c70c19e51cd67bd",
)

load("//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()
