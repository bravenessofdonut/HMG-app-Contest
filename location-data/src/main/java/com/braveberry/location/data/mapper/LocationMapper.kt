package com.braveberry.location.data.mapper

import com.braveberry.domain.model.location.GeoLocation
import com.braveberry.domain.model.location.ProximityUpdate
import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityReading
import com.braveberry.location.data.model.ProximityRequest
import java.io.IOException

internal fun LocationReading.toDomain(): GeoLocation {
    if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
        !longitude.isFinite() || longitude !in -180.0..180.0
    ) throw IOException("Fused Location returned invalid coordinates")
    return GeoLocation(latitude, longitude, altitude, heading, confidence, timestampMillis)
}

internal fun ProximityZone.toRequest() = ProximityRequest(latitude, longitude, radiusMeters)

internal fun ProximityReading.toDomain(): ProximityUpdate = when (this) {
    is ProximityReading.Registered -> ProximityUpdate.Registered(alertId)
    is ProximityReading.Entered -> ProximityUpdate.Entered(alertId)
    is ProximityReading.Exited -> ProximityUpdate.Exited(alertId)
}
