test:
	bazel test all_tests

test.local:
	bazel test all_local_tests

ktlint.check:
	bazel test //... --test_tag_filters="ktlint" --test_output=errors
ktlint.fix:
	bazel run //src/main/kotlin/io/bazel/kotlin/builder/cmd:build_lib_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/builder/cmd:merge_jdeps_lib_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/builder/tasks:tasks_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/builder/toolchain:toolchain_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/builder/utils:utils_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/builder/utils/jars:jars_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/compiler:compiler_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/plugin:skip-code-gen-lib_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/kotlin/plugin/jdeps:jdeps-gen-lib_ktlint_fix
	bazel run //src/main/kotlin/io/bazel/worker:worker_ktlint_fix

test.no_worker:
	bazel clean
	bazel shutdown
	bazel test --strategy=KotlinCompile=local //:all_tests

sky.reflow:
	scripts/reflow_skylark

deps.regen:
	scripts/regen_deps

docs.regen:
	bazel build //kotlin:stardoc
	cp bazel-bin/kotlin/kotlin.md docs/kotlin.md

proto.regen:
	scripts/gen_proto_jars

install.tools:
	go get github.com/bazelbuild/buildtools/buildifier
