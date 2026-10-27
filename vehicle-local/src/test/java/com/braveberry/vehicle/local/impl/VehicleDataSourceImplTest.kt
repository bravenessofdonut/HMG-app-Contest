package com.braveberry.vehicle.local.impl

import com.braveberry.vehicle.local.source.aaos.AaosSpeedSource
import com.braveberry.vehicle.local.source.pleos.PleosDoorSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** SDK 하드웨어에 접근하지 않고 local의 조합/위임 계약을 검증함. */
class VehicleDataSourceImplTest {
    private val speed = mockk<AaosSpeedSource>()
    private val door = mockk<PleosDoorSource>()

    @Test fun forwardsSpeedReadingsWithoutInventingValues() = runTest {
        every { speed.observe() } returns flowOf(0f, 3.5f)
        val source = VehicleDataSourceImpl(speed, door)
        assertEquals(listOf(0f, 3.5f), source.speedMetersPerSecond().toList())
    }

    @Test fun forwardsMissingDoorValueAsNull() = runTest {
        every { door.observe() } returns flowOf(null, 1)
        val source = VehicleDataSourceImpl(speed, door)
        assertEquals(listOf(null, 1), source.driverDoorPosition().toList())
    }
}
