package com.braveberry.presentation.vehicle.screen

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveberry.presentation.vehicle.viewmodel.VehicleViewModel

/** 상태 수집, 의존성 주입, 네비게이션 이벤트 전달을 Screen 렌더링에서 분리함. */
@Composable
fun VehicleRoute(
    speedPermissionGranted: Boolean,
    onRequestSpeedPermission: () -> Unit,
    onShowAbout: () -> Unit,
    onShowVoice: () -> Unit,
    viewModel: VehicleViewModel = hiltViewModel()
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(speedPermissionGranted) {
        viewModel.onSpeedPermissionChanged(speedPermissionGranted)
    }
    Scaffold { innerPadding ->
        VehicleScreen(
            state = state,
            speedPermissionGranted = speedPermissionGranted,
            onRequestSpeedPermission = onRequestSpeedPermission,
            onRetrySpeed = viewModel::retrySpeed,
            onRetryDoor = viewModel::retryDoor,
            onShowAbout = onShowAbout,
            onShowVoice = onShowVoice,
            padding = innerPadding
        )
    }
}
