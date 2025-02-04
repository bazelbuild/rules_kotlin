package test

internal const val DEFAULT_FRIEND = "muchacho"

class Service internal constructor(
  internal val value: String = "hello world"
) {
  internal fun iSayHolla(friend: String) {
    println("holla $friend!")
  }
}
