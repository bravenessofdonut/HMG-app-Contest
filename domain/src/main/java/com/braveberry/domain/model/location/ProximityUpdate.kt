package com.braveberry.domain.model.location

sealed interface ProximityUpdate {
    /** SDK가 등록 완료를 통지한 시점부터 실제 감지가 유효함. */
    data class Registered(val alertId: String) : ProximityUpdate
    data class Entered(val alertId: String) : ProximityUpdate
    data class Exited(val alertId: String) : ProximityUpdate
}
