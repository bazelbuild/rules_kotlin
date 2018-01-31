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
load(
    "//kotlin/rules:defs.bzl",
    _KotlinInfo = "KotlinInfo",
)

def _kotlin_stdlib(ctx):
    if len(ctx.files.jars) != 1:
        fail("a jar target must be provided")

    java_info = java_common.create_provider(
        use_ijar = False,
        source_jars=[ctx.file.srcjar],
        compile_time_jars = ctx.files.jars,
        runtime_jars=ctx.files.jars,
        transitive_compile_time_jars= ctx.files.jars,
        transitive_runtime_jars=ctx.files.jars
    )
    kotlin_info=_KotlinInfo(
      outputs = struct(
          jars = [struct(
            class_jar = ctx.files.jars[0],
            source_jar = ctx.file.srcjar,
            ijar = None,
          )]
      )
    )
    default_info = DefaultInfo(files=depset(ctx.files.jars))
    return struct(kt = kotlin_info, providers= [default_info, java_info, kotlin_info])

kotlin_stdlib = rule(
    attrs = {
        "jars": attr.label_list(
            allow_files = True,
            mandatory = True,
            cfg = "host",
        ),
        "srcjar": attr.label(
            allow_single_file = True,
            cfg = "host",
        ),
    },
    implementation = _kotlin_stdlib,
)

"""Import Kotlin libraries that are part of the compiler release."""
