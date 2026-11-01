package com.braveberry.domain.repository

import com.braveberry.domain.model.vehicle.DoorPosition
import com.braveberry.data_resource.DataResource
import kotlinx.coroutines.flow.Flow

/** Vehicle SDK / AAOS 타입을 외부로 노출하지 않는 계약. */
interface VehicleRepository {
    fun observeSpeedMetersPerSecond(): Flow<DataResource<Float>>
    fun observeDriverDoorPosition(): Flow<DataResource<DoorPosition>>
}
