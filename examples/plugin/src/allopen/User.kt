package plugin.allopen;

import java.util.*

@OpenForTesting
data class User(
        val userId: UUID,
        val emails: String
)
