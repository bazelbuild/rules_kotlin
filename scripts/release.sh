#!/bin/bash
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

# Builds and tests a release archive against the example directories.

TMPDIR="$1"
shift
if [[ -z $TMPDIR ]]; then
  TMPDIR="/tmp"
fi
BUILD_ARGS="$@"
HOME="$PWD"

function fail() {
  echo "$1"
  cd "$HOME" || exit
  exit 1
}

# find workspace
while [[ ! -f "WORKSPACE" ]]; do
  cd ..
  if test "$PWD" = "/"; then
    fail "No workspace found."
  fi
done

# lint
if ! scripts/reflow_skylark; then
  fail "Lint errors"
fi

# validate
if test ! -d examples; then
  fail "unable to find example directory: $PWD"
fi

# generate stardoc
bazel build //kotlin:stardoc || fail "docs did not generate"

cp -f bazel-bin/kotlin/kotlin.md docs/ || fail "couldn't copy"

bazel test //...:all || fail "tests failed"

# build release
bazel build //:rules_kotlin_release || fail "release archive failed"

# unpack to for repository overriding
ARCHIVE_DIR="$TMPDIR/rules_kotlin_release"

RELEASE_ARCHIVE=$(realpath bazel-bin/rules_kotlin_release.tgz)

shasum -a 256 bazel-bin/rules_kotlin_release.tgz >bazel-bin/rules_kotlin_release.tgz.sha256

# iterate through the examples and build them
for ex in examples/*/; do
  if [[ -f "$ex/WORKSPACE" ]] && ! [[ -f "$ex/ignore.me" ]]; then
    (
      cd "$ex"
      # shellcheck disable=SC1007
      export RULES_KOTLIN_ARCHIVE="$RELEASE_ARCHIVE"
      bazel build ${BUILD_ARGS} //...:all
    ) || fail "$ex failed to build"
  fi
done

# clean up
rm -rf $ARCHIVE_DIR


echo "Release artifact is good:"
echo "    bazel-bin/rules_kotlin_release.tgz"
echo "    bazel-bin/rules_kotlin_release.tgz.sha256"
echo "sha256: $(cat bazel-bin/rules_kotlin_release.tgz.sha256)"
