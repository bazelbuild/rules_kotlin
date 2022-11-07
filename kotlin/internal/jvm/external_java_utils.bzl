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

"""Various Java utils that were copied from pre-alpha branch of rules_android repo

   See:
   https://github.com/bazelbuild/rules_android/blob/pre-alpha/rules/java.bzl
"""

# TODO(djwhang): Get the path separator in a platform agnostic manner.
_PATH_SEP = "/"

def _is_absolute(path):
    # TODO(djwhang): This is not cross platform safe. Windows absolute paths
    # do not start with "//", rather "C:\".
    return path.startswith(_PATH_SEP)

def _segment_idx(path_segments):
    """Finds the index of the segment in the path that preceeds the source root.
    Args:
      path_segments: A list of strings, where each string is the segment of a
        filesystem path.
    Returns:
      An index to the path segment that represents the Java segment or -1 if
      none found.
    """
    if _is_absolute(path_segments[0]):
        fail("ERROR: path must not be absolute: %s" % "/".join(path_segments))

    root_idx = -1
    for idx, segment in enumerate(path_segments):
        if segment in ["java", "javatests", "src", "testsrc"]:
            root_idx = idx
            break
    if root_idx < 0:
        return root_idx

    is_src = path_segments[root_idx] == "src"
    check_maven_idx = root_idx if is_src else -1
    if root_idx == 0 or is_src:
        # Check for a nested root directory.
        for idx in range(root_idx + 1, len(path_segments) - 2):
            segment = path_segments[idx]
            if segment == "src" or (is_src and segment in ["java", "javatests"]):
                next_segment = path_segments[idx + 1]
                if next_segment in ["com", "org", "net"]:
                    root_idx = idx
                elif segment == "src":
                    check_maven_idx = idx
                break

    if check_maven_idx >= 0 and check_maven_idx + 2 < len(path_segments):
        next_segment = path_segments[check_maven_idx + 1]
        if next_segment in ["main", "test"]:
            next_segment = path_segments[check_maven_idx + 2]
            if next_segment in ["java", "resources"]:
                root_idx = check_maven_idx + 2
    return root_idx

def _resolve_package(path):
    """Determines the Java package name from the given path.
    Examples:
        "{workspace}/java/foo/bar/wiz" -> "foo.bar.wiz"
        "{workspace}/javatests/foo/bar/wiz" -> "foo.bar.wiz"
    Args:
      path: A string, representing a file path.
    Returns:
      A string representing a Java package name or None if could not be
      determined.
    """
    path_segments = path.partition(":")[0].split("/")
    java_idx = _segment_idx(path_segments)
    if java_idx < 0:
        return None
    else:
        return ".".join(path_segments[java_idx + 1:])

def resolve_package_from_label(
        label,
        custom_package = None,
        fallback = True):
    """Resolves the Java package from a Label.
    When no legal Java package can be resolved from the label, None will be
    returned unless fallback is specified.
    When a fallback is requested, a not safe for Java compilation package will
    be returned. The fallback value will be derrived by taking the label.package
    and replacing all path separators with ".".
    """
    if custom_package:
        return custom_package

    java_package = _resolve_package(str(label))
    if java_package != None:  # "" is a valid result.
        return java_package

    if fallback:
        return label.package.replace("/", ".")

    return None
