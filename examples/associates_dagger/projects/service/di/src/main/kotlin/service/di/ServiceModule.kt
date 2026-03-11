package service.di

import dagger.Binds
import dagger.Module
import service.api.Greeter
import service.api.Logger
import service.impl.ConsoleLogger
import service.impl.DefaultGreeter
import javax.inject.Singleton

@Module
internal abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindLogger(impl: ConsoleLogger): Logger

    @Binds
    @Singleton
    abstract fun bindGreeter(impl: DefaultGreeter): Greeter
}
