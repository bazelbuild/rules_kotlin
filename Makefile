test:
	bazel test all_tests

test.no_worker:
	bazel clean
	bazel shutdown
	bazel test --strategy=KotlinCompile=local //:all_tests

reformat:
	buildifier -mode=fix -v kotlin/*.bzl
	buildifier -mode=fix -v kotlin/internal/*.bzl

docs.regen:
	bazel build //docs
	unzip -o bazel-bin/docs/docs-skydoc.zip -d docs

docs.preview_local:
	bazel build //docs --define local=1
	unzip -o bazel-bin/docs/docs-skydoc.zip -d /tmp/rules_kotlin
	open /tmp/rules_kotlin/index.html
