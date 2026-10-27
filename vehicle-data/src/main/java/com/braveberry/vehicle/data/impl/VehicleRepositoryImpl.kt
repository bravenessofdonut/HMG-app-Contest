package com.braveberry.vehicle.data.impl

import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.repository.VehicleRepository
import com.braveberry.vehicle.data.datasource.VehicleDataSource
import javax.inject.Inject

class VehicleRepositoryImpl @Inject constructor(
    private val source: VehicleDataSource
) : VehicleRepository {
    override fun observeSpeedMetersPerSecond() =
        source.speedMetersPerSecond().asDataResourceFlow()

    override fun observeDriverDoorPosition() =
        source.driverDoorPosition().asDataResourceFlow()
}
