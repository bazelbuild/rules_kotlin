package service.impl

import service.api.Greeter
import service.api.Logger
import javax.inject.Inject

internal class DefaultGreeter @Inject constructor(
    private val logger: Logger,
) : Greeter {
    override fun greet(): String {
        val message = "Hello from DefaultGreeter!"
        logger.log(message)
        return message
    }
}
