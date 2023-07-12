load(
    "//kotlin/internal/jvm:kt_android_local_test.bzl",
    _kt_android_local_test = "kt_android_local_test",
)
load(
    "//kotlin/internal/jvm:kt_android_library.bzl",
    _kt_android_library = "kt_android_library",
)

kt_android_library = _kt_android_library
kt_android_local_test = _kt_android_local_test
