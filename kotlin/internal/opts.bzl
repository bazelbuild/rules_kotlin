# Copyright 2020 The Bazel Authors. All rights reserved.
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

load(
    "//src/main/starlark/core/options:opts.kotlinc.bzl",
    _KotlincOptions = "KotlincOptions",
    _kotlinc_options_to_flags = "kotlinc_options_to_flags",
    _kt_kotlinc_options = "kt_kotlinc_options",
)
load(
    "//src/main/starlark/core/options:opts.javac.bzl",
    _JavacOptions = "JavacOptions",
    _javac_options_to_flags = "javac_options_to_flags",
    _kt_javac_options = "kt_javac_options",
)
load(
    "//src/main/starlark/core/options:kapt_opts.bzl",
    _KaptOptions = "KaptOptions",
    _kapt_options = "kapt_options",
    _kapt_options_to_flag = "kapt_options_to_flag",
)

JavacOptions = _JavacOptions
javac_options_to_flags = _javac_options_to_flags
kt_javac_options = _kt_javac_options

KotlincOptions = _KotlincOptions
kotlinc_options_to_flags = _kotlinc_options_to_flags
kt_kotlinc_options = _kt_kotlinc_options

KaptOptions = _KaptOptions
kapt_options_to_flag = _kapt_options_to_flag
kapt_options = _kapt_options
