This example workspace demonstrates use with https://github.com/johnynek/bazel-deps to manage external libraries (eg maven).

`dependencies.yaml` lists the external libraries to be imported.

To apply changes to `dependencies.yaml`:

  git clone https://github.com/johnynek/bazel-deps $BAZEL_DEPS
  cd $BAZEL_DEPS && bazel build src/scala/com/github/johnynek/bazel_deps/parseproject_deploy.jar
  $BAZEL_DEPS/gen_maven_deps.sh generate --repo-root $PWD --sha-file third_party/workspace.bzl --deps dependencies.yaml 

Until [recursive WORKSPACE](https://github.com/bazelbuild/proposals/blob/master/designs/2018-11-07-design-recursive-workspaces.md)s are supported this example needs to be opened as a separate intellij project.
