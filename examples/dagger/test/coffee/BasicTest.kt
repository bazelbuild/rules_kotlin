package coffee

import com.google.common.truth.Truth.assertThat
import heating.ElectricHeater
import org.junit.Test
import time.Delayer

class BasicTest {
    @Test fun `test that internal member is transitively visible`() {
        val heater = ElectricHeater(object : Delayer {
            override fun delay() {
                println("fake delay")
            }
        })
        assertThat(isHeaterOn(heater)).isFalse()
        heater.on()
        assertThat(isHeaterOn(heater)).isTrue()
        heater.off()
        assertThat(heater.isOn).isFalse()
    }
}
