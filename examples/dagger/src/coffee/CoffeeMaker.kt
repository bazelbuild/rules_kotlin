/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coffee

import dagger.Lazy
import heating.Heater
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pumping.Pump

class CoffeeMaker @Inject internal constructor(
    // Create a possibly costly heater only when we use it.
    private val heater: Lazy<Heater>,
    private val pump: Pump
) {

    suspend fun brew() {
        // this function is async to verify intellij support for coroutines.
        withContext(Dispatchers.Default) {
            heater.get().on()
            pump.pump()
            println(" [_]P coffee! [_]P ")
            heater.get().off()
        }
        withContext(Dispatchers.Default) {
            if (heater.get().isOn) throw IllegalStateException("Heater should be off")
        }
    }
}
