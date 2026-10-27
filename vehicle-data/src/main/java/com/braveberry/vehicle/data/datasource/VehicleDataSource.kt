package com.braveberry.vehicle.data.datasource

import kotlinx.coroutines.flow.Flow

/** local 구현체와 data 사이의 계약. 실제 SDK의 변경은 여기서 차단함. */
interface VehicleDataSource {
    fun speedMetersPerSecond(): Flow<Float>
    fun driverDoorPosition(): Flow<Int?>
}
