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
load("//kotlin:kotlin.bzl", _for_ide = "kt_jvm_library")

_BOOTSTRAP_LIB_ARGS=["-jvm-target","1.8"]

def _resolve_dep_label(d):
    if d.startswith("//kotlin/builder/src/io/bazel/kotlin/builder") and not d.endswith("_for_ide"):
        prefix, _, target = d.rpartition(":")
        if target == None:
            # untested
            return d + "_for_ide"
        else:
            _ , _, target = d.rpartition("/")
            return d + ":" + target + "_for_ide"
    else:
        return d

def kt_bootstrap_library(name, srcs, deps=[], neverlink_deps=[], runtime_deps=[]):
    """
    Simple compilation of a kotlin library using a non-persistent worker. The target is a JavaInfo provider.

    The target is tagged `"no-ide"` as intellij can't compile it. A seperate private target is created which is suffixed
    with `_for_ide`. If the dep is under the package `//kotlin/builder/src/io/bazel/kotlin/builder/...` then it will be
    added to the `_for_ide` target by adding a `_for_ide` prefix.

    deps: the dependenices, the are setup as runtime_deps of the library.
    neverlink_deps: deps that won't be linked. These deps are added to the `"for_ide"` target.
    """
    jar_label = name + "_jar"
    dep_label = name + "_deps"
    native.filegroup(
        name = dep_label,
        srcs = deps + neverlink_deps,
        tags = ["no-ide"],
        visibility=["//visibility:private"]
    )
    command="""
KOTLIN_HOME=external/com_github_jetbrains_kotlin

function join_by { local IFS="$$1"; shift; echo "$$*"; }

NAME=%s
CP="$$(join_by : $(locations :%s))"
ARGS="%s"

java -Xmx256M -Xms32M -noverify \
  -cp $${KOTLIN_HOME}/lib/kotlin-preloader.jar org.jetbrains.kotlin.preloading.Preloader \
  -cp $${KOTLIN_HOME}/lib/kotlin-compiler.jar org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -cp $${CP} -d $${NAME}_temp.jar $${ARGS} $(SRCS)

$(location @bazel_tools//tools/jdk:singlejar) \
    --normalize \
    --compression \
    --sources $${NAME}_temp.jar \
    --output $(OUTS)

rm $${NAME}_temp.jar
""" % (name, dep_label, " ".join(_BOOTSTRAP_LIB_ARGS))
    native.genrule(
        name =  jar_label,
        tools = [
            "@com_github_jetbrains_kotlin//:home",
            "@local_jdk//:jdk",
            "@bazel_tools//tools/jdk:singlejar",
            dep_label
        ],
        srcs = srcs,
        outs = [name + ".jar"],
        tags = ["no-ide"],
        visibility = ["//visibility:private"],
        cmd = command
    )
    native.java_import(
        name = name,
        jars = [jar_label],
        tags = ["no-ide"],
        runtime_deps=deps + runtime_deps,
        visibility=["//visibility:public"]
    )
    # hsyed todo this part of the graph should not be wired up outside of development.
    _for_ide(
        name = name + "_for_ide",
        srcs = srcs,
        deps = [_resolve_dep_label(d) for d in deps] + neverlink_deps,
        visibility = ["//kotlin/builder:__subpackages__"]
    )
