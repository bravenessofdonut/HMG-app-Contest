package com.braveberry.location.data.datasource

import com.braveberry.location.data.model.LocationReading
import com.braveberry.location.data.model.ProximityReading
import com.braveberry.location.data.model.ProximityRequest
import kotlinx.coroutines.flow.Flow

/** JVM에서 사용하는 계약임. Pleos SDK와 Android 타입은 local 밖으로 노출하지 않음. */
interface LocationDataSource {
    fun isSupported(): Boolean
    fun observeLocation(): Flow<LocationReading?>
    fun lastKnownLocation(): Flow<LocationReading?>
    fun observeProximity(request: ProximityRequest): Flow<ProximityReading>
}
