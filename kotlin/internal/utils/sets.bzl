#
# Copyright 2018 The Bazel Authors. All rights reserved.
# Copyright 2018 Square, Inc. All rights reserved.
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
#
# Originally written for bazel_maven_repository, inspired by skylib
#
"""
A set of utilities that provide set-like behavior, using a dict (specifically its keys) as the underlying
implementation.  Generally only use dictionaries created by sets.new() because values are normalized. Using
dictionaries from other sources may result in equality failing, and other odd behavior.
"""

_UNDEFINED = "__UNDEFINED__"
_EMPTY = "__EMPTY__"  # Check when changing this to keep in sync with sets.bzl

def _contains(set, item):
    """Returns true if the set contains the supplied item"""
    return not (set.get(item, _UNDEFINED) == _UNDEFINED)

def _add(set, item):
    """Adds an item to the set and returns the set"""
    set[item] = _EMPTY
    return set

def _add_all_as_list(set, items):
    "Implementation for the add_* family of functions."
    for item in items:
        sets.add(set, item)
    return set

def _add_all(set, items):
    """Adds all items in the list or all keys in the dictionary to the set and returns the set"""
    item_type = type(items)
    if item_type == type({}):
        _add_all_as_list(set, list(items))
    elif item_type == type([]):
        _add_all_as_list(set, items)
    else:
        fail("Error, invalid %s argument passed to set operation." % item_type)
    return set

def _new(*items):
    """Creates a new set, from a variable array of parameters. """
    return {} if not bool(items) else sets.add_all({}, list(items))

def _copy_of(items):
    """Creates a new set from a given list. """
    return {} if not bool(items) else sets.add_all({}, list(items))

def _difference(a, b):
    """Returns the elements that reflect the set difference (items in a that are not in b)"""
    return sets.copy_of([x for x in list(a) if not sets.contains(b, x)])

def _intersection(a, b):
    """Returns the elements that exist in both A and B"""
    return sets.difference(a, sets.difference(a, b))

sets = struct(
    difference = _difference,
    intersection = _intersection,
    contains = _contains,
    add = _add,
    add_all = _add_all,
    new = _new,
    copy_of = _copy_of,
)
