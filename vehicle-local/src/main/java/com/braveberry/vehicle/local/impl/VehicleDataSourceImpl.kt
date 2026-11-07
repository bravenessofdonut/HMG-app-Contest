package com.braveberry.vehicle.local.impl

import com.braveberry.vehicle.data.datasource.VehicleDataSource
import com.braveberry.vehicle.local.source.aaos.AaosSpeedSource
import com.braveberry.vehicle.local.source.pleos.PleosDoorSource
import javax.inject.Inject

class VehicleDataSourceImpl @Inject constructor(
    private val speed: AaosSpeedSource,
    private val door: PleosDoorSource
) : VehicleDataSource {
    override fun speedMetersPerSecond() = speed.observe()
    override fun driverDoorPosition() = door.observe()
}
