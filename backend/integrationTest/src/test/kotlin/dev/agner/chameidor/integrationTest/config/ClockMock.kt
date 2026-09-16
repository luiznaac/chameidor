package dev.agner.chameidor.integrationTest.config

import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.ZoneId

@Component
object ClockMock {

    private val delegate = Clock.systemDefaultZone()

    val clock = mockk<Clock> {
        every { zone } returns ZoneId.systemDefault()
        every { instant() } answers { delegate.instant() }
        every { millis() } answers { delegate.millis() }
    }

    @Bean
    @Primary
    fun clock() = clock
}
