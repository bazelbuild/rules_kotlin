package examples.deps.libktandroid1
import examples.deps.libktandroid2.KtDummy2

class KtDummy1{

  companion object {

    private val resourceId = R.string.dummy1;
  }

  // Fails with jdeps:
  //    --@io_bazel_rules_kotlin//kotlin/internal/jvm:experimental_prune_transitive_deps=True
  //    --@io_bazel_rules_kotlin//kotlin/internal/jvm:kotlin_deps=True
  // Passes without jdeps:
  //    --@io_bazel_rules_kotlin//kotlin/internal/jvm:experimental_prune_transitive_deps=True
  //    --@io_bazel_rules_kotlin//kotlin/internal/jvm:kotlin_deps=False
  fun returnSomeType():KtDummy2 = KtDummy2()

}