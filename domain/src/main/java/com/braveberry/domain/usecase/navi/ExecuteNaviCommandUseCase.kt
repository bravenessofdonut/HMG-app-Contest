package com.braveberry.domain.usecase.navi

import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.repository.NaviRepository
import javax.inject.Inject

class ExecuteNaviCommandUseCase @Inject constructor(private val repository: NaviRepository) {
    operator fun invoke(command: NaviCommand) = repository.execute(command)
}
