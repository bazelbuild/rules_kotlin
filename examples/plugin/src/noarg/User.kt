package plugin.noarg;

import java.util.*

@NoArgConstructor
data class User(
        val userId: UUID,
        val emails: String
)
