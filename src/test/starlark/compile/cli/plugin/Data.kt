package plugin.serialization

import kotlinx.serialization.Serializable

@Serializable
data class Data(val stringValue: String, val intValue: Int)
