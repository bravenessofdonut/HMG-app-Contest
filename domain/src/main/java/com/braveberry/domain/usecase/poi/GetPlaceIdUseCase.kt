package com.braveberry.domain.usecase.poi

import com.braveberry.data_resource.DataResource
import com.braveberry.domain.model.poi.PoiInfo
import com.braveberry.domain.repository.PoiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPoiUseCase @Inject constructor(
    private val repository: PoiRepository
) {
    operator fun invoke(
        placeName: String
    ): Flow<DataResource<PoiInfo?>> =
        repository.getPoi(placeName)
}