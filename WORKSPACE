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
load("//kotlin/internal/repositories:repositories.bzl","github_archive")

github_archive(
    name = "com_google_protobuf",
    repo = "google/protobuf",
    commit = "106ffc04be1abf3ff3399f54ccf149815b287dd9",
)

github_archive(
    name = "build_bazel_rules_nodejs",
    repo = "bazelbuild/rules_nodejs",
    commit = "df3d2f577ec57ef5a622c809288a29545470c15d",
)
load("@build_bazel_rules_nodejs//:defs.bzl", "node_repositories")
node_repositories(package_json = [])

github_archive(
    name = "io_bazel_rules_sass",
    repo = "bazelbuild/rules_sass",
    commit = "38989d69ef3ba5847640f007fee5cc489be6ede9"
)
load("@io_bazel_rules_sass//sass:sass_repositories.bzl", "sass_repositories")
sass_repositories()

github_archive(
    name = "bazel_skylib",
    repo = "bazelbuild/bazel-skylib",
    commit = "3fea8cb680f4a53a129f7ebace1a5a4d1e035914"
)

github_archive(
    name = "io_bazel_skydoc",
    repo = "bazelbuild/skydoc",
    commit="f531844d137c7accc44d841c08a2a2a366688571"
)
load("@io_bazel_skydoc//skylark:skylark.bzl", "skydoc_repositories")
skydoc_repositories()

http_jar(
    name = "bazel_deps",
    url = "https://github.com/hsyed/bazel-deps/releases/download/v0.1.0/parseproject_deploy.jar",
    sha256 = "05498224710808be9687f5b9a906d11dd29ad592020246d4cd1a26eeaed0735e",
)

load("//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()
