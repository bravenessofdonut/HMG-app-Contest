package com.braveberry.data_resource

sealed class DataResource<out T> {
    class Success<T>(val data: T) : DataResource<T>()
    class Error(val failure: ResourceFailure) : DataResource<Nothing>()
    class Loading<T>(val data: T? = null) : DataResource<T>()

    companion object {
        fun <T> success(data: T) =
            Success(data)

        fun error(failure: ResourceFailure) =
            Error(failure)

        fun <T> loading(data: T? = null): Loading<T> =
            Loading(data)
    }
}