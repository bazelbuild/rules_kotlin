This folder contains simple libraries that depends on each other (lib1->lib2->lib3->lib4) for multiple type : java, android, kt, kt android. The purpose is to test compilation avoidance for project following strict deps: in this case, we want ABI changes to leaf library (lib4) to recompilation only lib4, and direct consumer (lib3).

To compile a given library type :
- `bazel build libJava1:src_main` for regular java jvm lib
- `bazel build liblibAndroid1Java1:src_main` for regular java android lib
- `bazel build libKt1:src_main` for regular kotlin jvm lib
- `bazel build libKtAndroid1:src_main libKtAndroid1:src_main_kt` for android kotlin lib

For convenience, a simple script 'run.sh' is also provided, which performs an incremental build on all library type after applying an ABI change to all leaves, and output the # of targets that got rebuilt for each library type. This currently output a value of 4 for all types, while we ideally want a value of 2.