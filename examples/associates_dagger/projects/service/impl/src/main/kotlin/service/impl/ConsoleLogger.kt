package service.impl

import service.api.Logger
import javax.inject.Inject

internal class ConsoleLogger @Inject constructor() : Logger {
    override fun log(message: String) {
        println("[LOG] $message")
    }
}
