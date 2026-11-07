package com.braveberry.domain.usecase.vehicle

import com.braveberry.domain.repository.VehicleRepository
import javax.inject.Inject

class ObserveDriverDoorUseCase @Inject constructor(private val repository: VehicleRepository) {
    operator fun invoke() = repository.observeDriverDoorPosition()
}
