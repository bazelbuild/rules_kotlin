package io.bazel.kotlin.builder.jobs.jvm

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.Base64

object Base64 {
  private val encoder by lazy(Base64::getEncoder)

  fun encode(options: Map<String, String>): String {
    return encoder.encodeToString(
      ByteArrayOutputStream().apply {
        use { stream ->
          ObjectOutputStream(stream).apply {
            writeInt(options.size)
            options.entries.forEach { (key, value) ->
              writeUTF(key)
              writeUTF(value)
            }
          }
        }
      }.toByteArray()
    )
  }
}
