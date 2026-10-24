package com.braveberry.data_resource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onStart


suspend fun <T> Flow<DataResource<T>>.collectDataResource(
    onSuccess: suspend (T) -> Unit,
    onError: (Throwable) -> Unit,
    onLoading: (T?) -> Unit = {},
) {
    this.catch { onError(it) }
        .collect {
            when (it) {
                is DataResource.Success -> onSuccess(it.data)
                is DataResource.Error -> onError(it.throwable)
                is DataResource.Loading -> onLoading.invoke(it.data)
            }
        }
}

fun <T> Flow<DataResource<T>>.onSuccess(action: suspend (T) -> Unit): Flow<DataResource<T>> =
    onEach { resource ->
        if (resource is DataResource.Success<T>) {
            action(resource.data)
        }
    }

fun <T> Flow<DataResource<T>>.onError(action: suspend (Throwable) -> Unit): Flow<DataResource<T>> =
    onEach { resource ->
        if (resource is DataResource.Error) {
            action(resource.throwable)
        }
    }

suspend fun <T> Flow<DataResource<T>>.awaitOrThrow(): T =
    transform { resource ->
        when (resource) {
            is DataResource.Success -> emit(resource.data)
            is DataResource.Error -> throw resource.throwable
            is DataResource.Loading -> return@transform
        }
    }.first()


fun <Source, Destination> Flow<DataResource<Source>>.mapDataResource(mapper: (Source) -> Destination)
        : Flow<DataResource<Destination>> =
    map {
        when (it) {
            is DataResource.Success -> DataResource.success(mapper(it.data))
            is DataResource.Error -> DataResource.error(it.throwable)
            is DataResource.Loading -> {
                val newData = it.data?.let { source -> mapper(source) }
                DataResource.loading(newData)
            }
        }
    }

fun <Source, Destination> Flow<DataResource<List<Source>>>.mapListDataResource(mapper: (Source) -> Destination)
        : Flow<DataResource<List<Destination>>> =
    mapDataResource { list -> list.map(mapper) }


fun <T, R> Flow<DataResource<T>>.flatMapDataResource(operation: (T) -> Flow<DataResource<R>>): Flow<DataResource<R>> =
    transform { resource ->
        when (resource) {
            is DataResource.Success -> emitAll(operation(resource.data))
            is DataResource.Error -> emit(DataResource.error(resource.throwable))
            is DataResource.Loading -> {
                val data = resource.data?.let(operation) ?: return@transform
                emitAll(data)
            }
        }
    }

fun <T> Flow<T>.asDataResourceFlow(): Flow<DataResource<T>> =
    transform { value ->
    emit(DataResource.success(value) as DataResource<T>)
}.onStart {
    emit(DataResource.loading())
}.catch { e ->
    emit(DataResource.error(e))
}
