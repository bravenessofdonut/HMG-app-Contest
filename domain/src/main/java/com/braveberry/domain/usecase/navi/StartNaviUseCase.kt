package com.braveberry.domain.usecase.navi

import com.braveberry.domain.repository.NaviRepository
import javax.inject.Inject

class StartNaviUseCase @Inject constructor(private val repository: NaviRepository) {
    operator fun invoke() = repository.start()
}
