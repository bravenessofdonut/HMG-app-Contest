package com.braveberry.domain.usecase.navi

import com.braveberry.domain.repository.NaviRepository
import javax.inject.Inject

class ObserveNaviEventsUseCase @Inject constructor(private val repository: NaviRepository) {
    operator fun invoke() = repository.observeEvents()
}
