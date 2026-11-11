package com.braveberry.domain.usecase.location

import com.braveberry.domain.repository.LocationRepository
import javax.inject.Inject

class CheckLocationSupportUseCase @Inject constructor(private val repository: LocationRepository) {
    operator fun invoke() = repository.checkSupport()
}
