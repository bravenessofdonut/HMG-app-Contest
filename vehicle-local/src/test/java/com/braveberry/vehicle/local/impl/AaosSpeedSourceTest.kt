package com.braveberry.vehicle.local.impl

import android.content.Context
import android.content.pm.PackageManager
import com.braveberry.vehicle.local.source.aaos.AaosSpeedSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AaosSpeedSourceTest {
    @Test fun deniedSpeedPermissionFailsBeforeTouchingVehicleService() = runTest {
        val context = mockk<Context>()
        every { context.checkSelfPermission("android.car.permission.CAR_SPEED") } returns
            PackageManager.PERMISSION_DENIED

        val result = runCatching { AaosSpeedSource(context).observe().first() }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("CAR_SPEED"))
    }
}
