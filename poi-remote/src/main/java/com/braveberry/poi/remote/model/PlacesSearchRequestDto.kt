package com.braveberry.poi.remote.model

data class PlacesSearchRequestDto(
    //관광지이름
    val textQuery: String,
    val pageSize: Int = 1,
    val languageCode: String = "ko",
    val regionCode: String = "KR"
)