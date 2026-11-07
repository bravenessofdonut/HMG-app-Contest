package com.braveberry.presentation.vehicle.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.braveberry.domain.model.vehicle.DoorPosition
import com.braveberry.domain.usecase.voice.Playback
import com.braveberry.presentation.vehicle.viewmodel.FailureReason
import com.braveberry.presentation.vehicle.viewmodel.Reading
import com.braveberry.presentation.vehicle.viewmodel.VehicleUiState
import java.util.Locale

@Composable
fun VehicleScreen(
    state: VehicleUiState,
    speedPermissionGranted: Boolean,
    onRequestSpeedPermission: () -> Unit,
    onRetrySpeed: () -> Unit,
    onRetryDoor: () -> Unit,
    onShowAbout: () -> Unit,
    onShowVoice: () -> Unit,
    padding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("차량 상태", style = MaterialTheme.typography.headlineMedium)
        Text("실시간 신호 / 미수신 값을 임의 숫자로 대체하지 않음")
        ReadingCard("차량 속도 (AAOS)", if (!speedPermissionGranted) "차량 속도 권한 필요" else when (val r = state.speedMetersPerSecond) {
            is Reading.Loading -> "속도 데이터 대기 중"
            is Reading.Ready -> String.format(Locale.KOREA, "%.1f km/h", r.data * 3.6f)
            is Reading.Failed -> r.reason.label()
        })
        if (!speedPermissionGranted) {
            Button(onClick = onRequestSpeedPermission) { Text("차량 속도 권한 요청") }
        } else if ((state.speedMetersPerSecond as? Reading.Failed)?.reason != FailureReason.UNSUPPORTED) {
            Button(onClick = onRetrySpeed) { Text("속도 재연결") }
        }

        ReadingCard("운전석 도어 (Pleos SDK)", when (val r = state.driverDoorPosition) {
            is Reading.Loading -> "도어 데이터 대기 중"
            is Reading.Ready -> when (val position = r.data) {
                DoorPosition.Unavailable -> "현재값 미수신"
                is DoorPosition.Unrecognized -> "미확인 도어 코드: ${position.rawCode}"
            }
            is Reading.Failed -> r.reason.label()
        })
        if ((state.driverDoorPosition as? Reading.Failed)?.reason != FailureReason.UNSUPPORTED) {
            Button(onClick = onRetryDoor) { Text("도어 재연결") }
        }
        val playback = state.playback
        if (playback is Playback.Speaking) {
            Text(playback.role?.let { "진행자 $it 재생 중" } ?: "음성 재생 중")
        }
        Button(onClick = onShowVoice) { Text("Pleos TTS/STT 테스트") }
        Button(onClick = onShowAbout) { Text("데이터 출처 및 설명") }
    }
}

@Composable
private fun ReadingCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun FailureReason.label(): String = when (this) {
    FailureReason.PERMISSION -> "차량 데이터 접근 권한을 확인해 주세요"
    FailureReason.UNSUPPORTED -> "이 환경에서 지원하지 않는 차량 신호입니다"
    FailureReason.TEMPORARY -> "차량 서비스 연결이 끊겼습니다. 다시 연결해 주세요"
}
