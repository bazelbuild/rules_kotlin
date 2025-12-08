package coffee

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoffeeAppModel(val id: String)
