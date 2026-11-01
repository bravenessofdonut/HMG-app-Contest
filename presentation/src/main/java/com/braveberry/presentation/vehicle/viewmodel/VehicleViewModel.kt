package com.braveberry.presentation.vehicle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveberry.data_resource.DataResource
import com.braveberry.domain.usecase.vehicle.ObserveDriverDoorUseCase
import com.braveberry.domain.usecase.vehicle.ObserveSpeedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val observeSpeed: ObserveSpeedUseCase,
    private val observeDriverDoor: ObserveDriverDoorUseCase
) : ViewModel(), ContainerHost<VehicleUiState, Nothing> {
    override val container = container<VehicleUiState, Nothing>(VehicleUiState())
    private var speedPermissionGranted = false
    private var speedJob: Job? = null
    private var doorJob: Job? = null

    init {
        retryDoor()
    }

    fun onSpeedPermissionChanged(granted: Boolean) {
        if (granted == speedPermissionGranted) return
        speedPermissionGranted = granted
        if (granted) {
            retrySpeed()
        } else {
            speedJob?.cancel()
            speedJob = null
            intent { reduce { state.copy(speedMetersPerSecond = Reading.Loading) } }
        }
    }

    fun retrySpeed() {
        if (!speedPermissionGranted) return
        speedJob?.cancel()
        speedJob = viewModelScope.launch {
            observeSpeed().collect { resource ->
                intent {
                    reduce {
                        state.copy(speedMetersPerSecond = when (resource) {
                            is DataResource.Loading -> Reading.Loading
                            is DataResource.Success -> Reading.Ready(resource.data)
                            is DataResource.Error -> Reading.Failed(resource.throwable.message ?: "속도 수신 오류")
                        })
                    }
                }
            }
        }
    }

    fun retryDoor() {
        doorJob?.cancel()
        doorJob = viewModelScope.launch {
            observeDriverDoor().collect { resource ->
                intent {
                    reduce {
                        state.copy(driverDoorPosition = when (resource) {
                            is DataResource.Loading -> Reading.Loading
                            is DataResource.Success -> Reading.Ready(resource.data)
                            is DataResource.Error -> Reading.Failed(resource.throwable.message ?: "도어 수신 오류")
                        })
                    }
                }
            }
        }
    }
}
