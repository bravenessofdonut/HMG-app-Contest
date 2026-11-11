package com.braveberry.domain.usecase.location

import com.braveberry.domain.repository.LocationRepository
import javax.inject.Inject

class ObserveLocationUseCase @Inject constructor(private val repository: LocationRepository) {
    operator fun invoke() = repository.observeLocation()
}
