package service.di

import dagger.Component
import service.api.Greeter
import javax.inject.Singleton

@Singleton
@Component(modules = [ServiceModule::class])
interface ServiceComponent {
    fun greeter(): Greeter
}
