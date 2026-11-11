package com.braveberry.location.data.impl

import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.domain.repository.LocationRepository
import com.braveberry.location.data.datasource.LocationDataSource
import com.braveberry.location.data.mapper.toDomain
import com.braveberry.location.data.mapper.toRequest
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class LocationRepositoryImpl @Inject constructor(
    private val source: LocationDataSource,
) : LocationRepository {
    override fun checkSupport() = flow { emit(source.isSupported()) }
        .asDataResourceFlow(::classifyLocationFailure)

    override fun observeLocation() = source.observeLocation()
        .map { it?.toDomain() }
        .asDataResourceFlow(::classifyLocationFailure)

    override fun getLastKnownLocation() = source.lastKnownLocation()
        .map { it?.toDomain() }
        .asDataResourceFlow(::classifyLocationFailure)

    override fun observeProximity(zone: ProximityZone) =
        source.observeProximity(zone.toRequest())
            .map { it.toDomain() }
            .asDataResourceFlow(::classifyLocationFailure)
}

private fun classifyLocationFailure(cause: Exception): ResourceFailure = when (cause) {
    is SecurityException -> ResourceFailure.PermissionRequired(cause)
    is UnsupportedOperationException -> ResourceFailure.Unsupported(cause)
    is IOException -> ResourceFailure.ServiceUnavailable(cause)
    else -> ResourceFailure.ServiceUnavailable(cause)
}
