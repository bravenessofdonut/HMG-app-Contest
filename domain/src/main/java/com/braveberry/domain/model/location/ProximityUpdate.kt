package com.braveberry.domain.model.location

sealed interface ProximityUpdate {
    /** SDK 등록 완료 콜백을 받은 뒤부터 근접 알림이 유효함. */
    data class Registered(val alertId: String) : ProximityUpdate
    data class Entered(val alertId: String) : ProximityUpdate
    data class Exited(val alertId: String) : ProximityUpdate
}
