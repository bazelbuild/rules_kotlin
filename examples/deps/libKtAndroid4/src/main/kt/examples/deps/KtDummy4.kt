package examples.deps.libktandroid4

class KtDummy4 {

  companion object {

    // Modify line below to create ABI change
    const val abiChangeVariable: Int = 0

    private val resourceId = R.string.dummy4;
  }

  fun dummy() {
    System.out.println("dummy")
  }
}
