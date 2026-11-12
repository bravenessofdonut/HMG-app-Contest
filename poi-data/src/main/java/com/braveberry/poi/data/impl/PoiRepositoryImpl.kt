
package com.braveberry.poi.data.impl

import com.braveberry.data_resource.DataResource
import com.braveberry.data_resource.ResourceFailure
import com.braveberry.data_resource.asDataResourceFlow
import com.braveberry.domain.model.poi.PoiInfo
import com.braveberry.domain.repository.PoiRepository
import com.braveberry.poi.data.datasource.PoiRemoteDataSource
import com.braveberry.poi.data.mapper.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class PoiRepositoryImpl @Inject constructor(
    private val datasource: PoiRemoteDataSource
) : PoiRepository {

    override fun getPoi(
        placeName: String
    ): Flow<DataResource<PoiInfo?>> =
        flow {
            emit(datasource.getPoi(placeName))
        }
            .map { it?.toDomain() }
            .asDataResourceFlow(::classifyPoiFailure)
}

private fun classifyPoiFailure(cause: Exception): ResourceFailure? =
    when (cause) {
        is IOException ->
            ResourceFailure.ServiceUnavailable(cause)

        else -> null
    }
