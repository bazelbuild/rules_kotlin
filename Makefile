test:
	bazel test all_tests

test.local:
	bazel test all_local_tests

test.no_worker:
	bazel clean
	bazel shutdown
	bazel test --strategy=KotlinCompile=local //:all_tests

sky.reflow:
	scripts/reflow_skylark

deps.regen:
	scripts/regen_deps

proto.regen:
	scripts/gen_proto_jars

install.tools:
	go get github.com/bazelbuild/buildtools/buildifier
