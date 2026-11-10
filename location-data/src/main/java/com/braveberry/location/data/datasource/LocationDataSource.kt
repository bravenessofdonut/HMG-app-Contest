package com.braveberry.location.data.datasource

import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityReading
import com.braveberry.location.data.model.ProximityRequest
import kotlinx.coroutines.flow.Flow

/** 순수 JVM 계약. Pleos SDK/Android 타입은 local 밖으로 노출하지 않음. */
interface LocationDataSource {
    fun isSupported(): Boolean
    fun observeLocation(): Flow<LocationReading?>
    fun lastKnownLocation(): Flow<LocationReading?>
    fun observeProximity(request: ProximityRequest): Flow<ProximityReading>
}
