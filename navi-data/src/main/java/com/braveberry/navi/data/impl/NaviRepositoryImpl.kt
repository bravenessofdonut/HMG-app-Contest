package com.braveberry.navi.data.impl

import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.model.navi.NaviCommand
import com.braveberry.domain.repository.NaviRepository
import com.braveberry.navi.data.datasource.NaviDataSource
import com.braveberry.navi.data.mapper.toDomain
import com.braveberry.navi.data.mapper.toInstruction
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class NaviRepositoryImpl @Inject constructor(private val source: NaviDataSource) : NaviRepository {
    override fun start() = flow { source.start(); emit(Unit) }.asDataResourceFlow(::classifyNaviFailure)
    override fun stop() = flow { source.stop(); emit(Unit) }.asDataResourceFlow(::classifyNaviFailure)
    override fun execute(command: NaviCommand) = flow {
        source.execute(command.toInstruction())
        emit(Unit)
    }.asDataResourceFlow(::classifyNaviFailure)

    override fun observeEvents() = source.events()
        .map { it.toDomain() }
        .asDataResourceFlow(::classifyNaviFailure)
}

private fun classifyNaviFailure(e: Exception): ResourceFailure? = when (e) {
    is SecurityException -> ResourceFailure.PermissionRequired(e)
    is UnsupportedOperationException -> ResourceFailure.Unsupported(e)
    is IOException, is IllegalStateException -> ResourceFailure.ServiceUnavailable(e)
    else -> null
}
