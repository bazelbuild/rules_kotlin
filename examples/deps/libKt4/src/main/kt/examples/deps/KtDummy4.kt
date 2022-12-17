package examples.deps.libKt4

class KtDummy4 {

  companion object {

    // Modify line below to create ABI change
    const val abiChangeVariable: Int = 1
  }

  fun dummy() {
    System.out.println("dummy")
  }
}
