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
package heating

import javax.inject.Inject
import time.Delayer

class ElectricHeater
    @Inject constructor(private val delayer: Delayer) : Heater() {

    override var isHot: Boolean = false
    override var isOn: Boolean = false

    override fun on() {
        isOn = true
        println("~ ~ ~ heating ~ ~ ~")
        delayer.delay()
        this.isHot = true
    }

    override fun off() {
        this.isOn = false
        println("~ ~ ~ cooling ~ ~ ~")
        delayer.delay()
        this.isHot = false
    }
}
