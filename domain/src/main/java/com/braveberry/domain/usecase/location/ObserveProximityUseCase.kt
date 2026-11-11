package com.braveberry.domain.usecase.location

import com.braveberry.domain.model.location.ProximityZone
import com.braveberry.domain.repository.LocationRepository
import javax.inject.Inject

class ObserveProximityUseCase @Inject constructor(private val repository: LocationRepository) {
    operator fun invoke(zone: ProximityZone) = repository.observeProximity(zone)
}
