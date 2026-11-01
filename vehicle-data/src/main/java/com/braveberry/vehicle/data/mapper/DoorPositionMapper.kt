package com.braveberry.vehicle.data.mapper

import com.braveberry.domain.model.vehicle.DoorPosition

// 의미가 확인된 코드만 여기에 매핑함.
internal fun Int?.toDoorPosition(): DoorPosition =
    if (this == null) DoorPosition.Unavailable else DoorPosition.Unrecognized(this)
