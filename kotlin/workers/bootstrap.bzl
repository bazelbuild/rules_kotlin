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
load("//kotlin:kotlin.bzl", _for_ide = "kotlin_library")

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




def kotlin_worker_lib(srcs =[], args = [], deps=[], runtime_deps=[], exports=[]):
    name = "worker"
    dep_label = name + "_deps"
    jar_file_label =  name + "_file"
    jar_name = name+".jar"
    libary_label =  name + "_lib"

    # Dummy target loaded by `kotlin_library` so intellij can understand the sources.
    _for_ide(
        name = "for_ide",
        srcs = srcs,
        deps = deps,
        runtime_deps = runtime_deps,
        exports = exports,
        visibility=["//visibility:private"],
    )

    native.filegroup(
        name = dep_label,
        srcs = deps,
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
    native.java_import(
        name = libary_label,
        jars = [jar_name],
        exports = exports,
        runtime_deps = (depset(runtime_deps) + exports + deps).to_list(),
        visibility = ["//visibility:private"],
        tags = ["no-ide"]
    )