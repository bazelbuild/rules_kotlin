package examples.android.lib

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DataJsonModel(val data: String)
