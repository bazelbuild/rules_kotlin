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
    "//kotlin/internal/utils:utils.bzl",
    _utils = "utils",
)

KtJvmPluginInfo = provider(
    doc = "This provider contains the plugin info for the JVM aspect",
    fields = {
        "annotation_processors": "depset of structs containing annotation processor definitions",
        "transitive_runtime_jars": "depset of transitive_runtime_jars for this plugin and deps",
    },
)

_EMPTY_PLUGIN_INFO = [KtJvmPluginInfo(annotation_processors = depset(), transitive_runtime_jars = depset())]

# Mapping functions for args.add_all.
# These preserve the transitive depsets until needed.
def _kt_plugin_to_processor(processor):
    return processor.processor_class

def _kt_plugin_to_processorpath(processor):
    return [j.path for j in processor.classpath.to_list()]

def _targets_to_annotation_processors(targets):
    return depset(transitive = [t[KtJvmPluginInfo].annotation_processors for t in targets if KtJvmPluginInfo in t])

def _targets_to_annotation_processors_java_info(targets):
    return [t[JavaInfo] for t in targets if KtJvmPluginInfo in t]

def _targets_to_transitive_runtime_jars(targets):
    return depset(transitive = [t[KtJvmPluginInfo].transitive_runtime_jars for t in targets if KtJvmPluginInfo in t])

mappers = struct(
    targets_to_annotation_processors = _targets_to_annotation_processors,
    targets_to_annotation_processors_java_info = _targets_to_annotation_processors_java_info,
    targets_to_transitive_runtime_jars = _targets_to_transitive_runtime_jars,
    kt_plugin_to_processor = _kt_plugin_to_processor,
    kt_plugin_to_processorpath = _kt_plugin_to_processorpath,
)

def merge_plugin_infos(attrs):
    """Merge all of the plugin infos found in the provided sequence of attributes.
    Returns:
        A KtJvmPluginInfo provider, Each of the entries is serializable."""
    return KtJvmPluginInfo(
        annotation_processors = _targets_to_annotation_processors(attrs),
        transitive_runtime_jars = _targets_to_transitive_runtime_jars(attrs),
    )

def _kt_jvm_plugin_aspect_impl(target, ctx):
    if ctx.rule.kind == "java_plugin":
        processor = ctx.rule.attr
        merged_deps = java_common.merge([j[JavaInfo] for j in processor.deps])
        return [KtJvmPluginInfo(
            annotation_processors = depset([
                struct(
                    label = _utils.restore_label(ctx.label),
                    processor_class = processor.processor_class,
                    classpath = merged_deps.transitive_runtime_jars,
                    generates_api = processor.generates_api,
                ),
            ]),
            transitive_runtime_jars = depset(transitive = [merged_deps.transitive_runtime_jars]),
        )]
    elif ctx.rule.kind == "java_library":
        return [merge_plugin_infos(ctx.rule.attr.exported_plugins)]
    else:
        return _EMPTY_PLUGIN_INFO

kt_jvm_plugin_aspect = aspect(
    doc = """This aspect collects Java Plugins info and other Kotlin compiler plugin configurations from the graph.""",
    attr_aspects = [
        "plugins",
        "exported_plugins",
    ],
    provides = [KtJvmPluginInfo],
    implementation = _kt_jvm_plugin_aspect_impl,
)
