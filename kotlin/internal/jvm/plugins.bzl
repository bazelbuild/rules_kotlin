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
_JavaPluginInfo = getattr(java_common, "JavaPluginInfo")

KtJvmPluginInfo = provider(
    doc = "This provider contains the plugin info for the JVM aspect",
    fields = {
        "annotation_processors": "depset of structs containing annotation processor definitions",
        "transitive_runtime_jars": "depset of transitive_runtime_jars for this plugin and deps",
    },
)

# Mapping functions for args.add_all.
# These preserve the transitive depsets until needed.
def _kt_plugin_to_processor(processor):
    return processor.processor_classes.to_list()

def _kt_plugin_to_processorpath(processor):
    return [j.path for j in processor.processor_jars.to_list()]

def _targets_to_annotation_processors(targets):
    plugins = []
    for t in targets:
        if _JavaPluginInfo in t:
            p = t[_JavaPluginInfo].plugins
            if p.processor_jars:
                plugins.append(p)
        elif JavaInfo in t:
            p = t[JavaInfo].plugins
            if p.processor_jars:
                plugins.append(p)
    return depset(plugins)

def _targets_to_annotation_processors_java_plugin_info(targets):
    return [t[_JavaPluginInfo] for t in targets if _JavaPluginInfo in t]

def _targets_to_transitive_runtime_jars(targets):
    return depset(
        transitive = [
            (t[_JavaPluginInfo] if _JavaPluginInfo in t else t[JavaInfo]).plugins.processor_jars
            for t in targets
            if _JavaPluginInfo in t or JavaInfo in t
        ],
    )

mappers = struct(
    targets_to_annotation_processors = _targets_to_annotation_processors,
    targets_to_annotation_processors_java_plugin_info = _targets_to_annotation_processors_java_plugin_info,
    targets_to_transitive_runtime_jars = _targets_to_transitive_runtime_jars,
    kt_plugin_to_processor = _kt_plugin_to_processor,
    kt_plugin_to_processorpath = _kt_plugin_to_processorpath,
)
