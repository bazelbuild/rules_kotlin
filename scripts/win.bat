:: script for developing with virtualbox. Rsyncs files using Robocopy and then runs any builds/tests.
SET REAL_SRC="z:\github.com\bazelbuild\rules_kotlin"

pushd %~dp0
    SET TARGET_DIR=%CD%\..
popd

robocopy %REAL_SRC% %TARGET_DIR% /mir /xo /xd bazel-* /NFL /NDL

 bazel ^
    --output_base "c:\tmp\bazel" ^
    --output_user_root "c:\tmp\bazel_rules_kotlin" ^
    build //src/main/kotlin:build

 bazel ^
    --output_base "c:\tmp\bazel" ^
    --output_user_root "c:\tmp\bazel_rules_kotlin" ^
    test -- //src/test/kotlin/io/bazel/kotlin/builder:builder_tests
