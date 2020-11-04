package plugin.allopennoarg

import java.util.*

@OpenForTesting
@NoArgConstructor
data class User(
        val userId: UUID,
        val emails: String
)
