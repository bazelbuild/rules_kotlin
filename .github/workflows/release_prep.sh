#!/usr/bin/env bash

set -o errexit -o nounset -o pipefail

# Set by GH actions, see
# https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables
TAG=${GITHUB_REF_NAME}
# The prefix is chosen to match what GitHub generates for source archives
PREFIX="rules_kotlin-${TAG:1}"
ARCHIVE="rules_kotlin-$TAG.tar.gz"
bazel --bazelrc=.github/workflows/ci.bazelrc --bazelrc=.bazelrc build //:rules_kotlin_release
cp bazel-bin/rules_kotlin_release.tgz $ARCHIVE

# Write the release notes to release_notes.txt
cat > release_notes.txt << EOF
# Release notes for $TAG
## Using Bzlmod

Add to your \`MODULE.bazel\` file:

\`\`\`starlark
bazel_dep(name = "rules_kotlin", version = "${TAG:1}")
\`\`\`
EOF
