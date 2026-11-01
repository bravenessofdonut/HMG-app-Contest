package com.braveberry.vehicle.data.impl

import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.data_resource.mapDataResource
import com.braveberry.domain.repository.VehicleRepository
import com.braveberry.vehicle.data.datasource.VehicleDataSource
import com.braveberry.vehicle.data.mapper.toDoorPosition
import java.io.IOException
import javax.inject.Inject

class VehicleRepositoryImpl @Inject constructor(
    private val source: VehicleDataSource
) : VehicleRepository {
    override fun observeSpeedMetersPerSecond() =
        source.speedMetersPerSecond().asDataResourceFlow(::classifyVehicleFailure)

    override fun observeDriverDoorPosition() =
        source.driverDoorPosition().asDataResourceFlow(::classifyVehicleFailure)
            .mapDataResource { it.toDoorPosition() }
}

private fun classifyVehicleFailure(cause: Exception): ResourceFailure? = when (cause) {
    is SecurityException -> ResourceFailure.PermissionRequired(cause)
    is UnsupportedOperationException -> ResourceFailure.Unsupported(cause)
    is IOException -> ResourceFailure.ServiceUnavailable(cause)
    else -> null
}
