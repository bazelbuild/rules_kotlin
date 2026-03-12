package com.example.optionsrecorder

import com.google.common.truth.Truth.assertThat
import generated.RecordedOptions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class OptionsRecorderTest {
    @Test
    fun kspOptionsArePassedToProcessor() {
        assertThat(RecordedOptions.options).containsEntry("option_a", "value_a")
        assertThat(RecordedOptions.options).containsEntry("option_b", "value_b")
        assertThat(RecordedOptions.options).hasSize(2)
    }
}
