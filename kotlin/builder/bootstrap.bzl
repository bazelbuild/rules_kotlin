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
load("//kotlin:kotlin.bzl", _for_ide = "kt_jvm_library", "kt_jvm_import")

_HEADER = """
function join_by { local IFS="$$1"; shift; echo "$$*"; }

CP=$$(join_by : $(locations :%s))
"""

# This is kept arround incase it's needed -- there shouldn't be a need for mixed compilation, but just in case.
def _gen_mixed_cmd(name, args):
    return _HEADER + ("""
ARGS="-Xcompile-java -Xuse-javac %s"

mkdir -p compile_classes

JAVA_HOME=external/local_jdk  ./$(location @com_github_jetbrains_kotlin//:kotlinc) -cp $${CP} -d compile_classes $${ARGS} $(SRCS)

jar cf  $(OUTS) -C compile_classes .

rm -rf compile_classes
""") % (name, args)

def _gen_cmd(name, args):
    return (_HEADER + """
ARGS="%s"

JAVA_HOME=external/local_jdk ./$(location @com_github_jetbrains_kotlin//:kotlinc) -cp $${CP} -d $(OUTS) $${ARGS} $(SRCS)
""") % (name,args)

def kotlin_worker_lib(name, srcs, args = [], deps=[], runtime_deps=[], neverlink_deps=[]):
    dep_label = name + "_deps"
    jar_file_label =  name + "_file"
    jar_name = name+".jar"
    jar_sources_file_label = jar_file_label + "_sources"
    jar_sources_name = name + "-sources.jar"

    native.filegroup(
        name = dep_label,
        srcs = deps + neverlink_deps,
        visibility = ["//visibility:private"]
    )
    native.genrule(
        name = jar_file_label,
        tools = [
            "@com_github_jetbrains_kotlin//:home",
            "@com_github_jetbrains_kotlin//:kotlinc",
            "@local_jdk//:jdk",
            dep_label
        ],
        srcs = srcs,
        outs = [jar_name],
        cmd = _gen_cmd(dep_label, " ".join(args)),
        visibility = ["//visibility:private"]
    )
    native.genrule(
        name = jar_sources_file_label,
        tools = [
            "@local_jdk//:jdk",
        ],
        srcs = srcs,
        outs = [jar_sources_name],
        cmd = "jar cf $(OUTS) $(SRCS)",
        visibility = ["//visibility:private"]
    )

    # Use kt_jvm_import so that ijarification doesn't ruin the worker lib.
    kt_jvm_import(
        name = name,
        jars = [jar_name],
        srcjar = jar_sources_name,
        tags = ["no-ide"],
        runtime_deps = (depset(runtime_deps) + deps).to_list(),
        visibility = [
            "//tests:__subpackages__",
            "//kotlin:__subpackages__"
        ]
    )