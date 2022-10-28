package examples.deps.libktandroid2
import examples.deps.libktandroid3.KtDummy3

class KtDummy2 : KtDummy3() {

  companion object {

    private val resourceId = R.string.dummy2;
  }

  override fun dummy() {
    System.out.println("dummy")
  }

  fun foo() { }
}
