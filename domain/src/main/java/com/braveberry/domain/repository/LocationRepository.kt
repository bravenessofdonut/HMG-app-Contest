package com.braveberry.domain.repository

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.location.GeoLocation
import com.braveberry.domain.model.location.ProximityUpdate
import com.braveberry.domain.model.location.ProximityZone
import kotlinx.coroutines.flow.Flow

/** 권한 요청은 UI 책임. 호출 전에 FUSED_LOCATION 런타임 권한 승인이 필요함. */
interface LocationRepository {
    fun checkSupport(): Flow<DataResource<Boolean>>
    fun observeLocation(): Flow<DataResource<GeoLocation?>>
    /** 캐시 부재(null)는 오류가 아닌 정상 응답. */
    fun getLastKnownLocation(): Flow<DataResource<GeoLocation?>>
    /** 구독 취소 시 알림 해제 및 SDK 자원을 회수함. */
    fun observeProximity(zone: ProximityZone): Flow<DataResource<ProximityUpdate>>
}
