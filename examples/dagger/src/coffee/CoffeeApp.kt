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

import dagger.Component
import kotlinx.coroutines.runBlocking
import tea.TeaPot
import chai.ChaiCup
import javax.inject.Singleton

class CoffeeApp {
  @Singleton
  @Component(modules = [(DripCoffeeModule::class)])
  interface CoffeeShop {
    fun maker(): CoffeeMaker
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      if (TeaPot.isEmpty() && ChaiCup.isEmpty()) {
        val coffeeShop = DaggerCoffeeApp_CoffeeShop.builder().build()
        runBlocking {
          coffeeShop.maker().brew()
        }
      }
    }
  }
}
