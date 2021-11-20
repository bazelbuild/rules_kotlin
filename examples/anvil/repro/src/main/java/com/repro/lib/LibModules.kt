package com.repro.lib 

import dagger.Module

class LibModules {

    @Module
    abstract class NoOpModule {

    }
}

@Module(
    includes = [
        LibModules.NoOpModule::class
    ]
)
object LibModule 

