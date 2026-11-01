package com.braveberry.presentation.voice.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.braveberry.presentation.voice.viewmodel.EngineStatus
import com.braveberry.presentation.voice.viewmodel.VoiceUiState

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onBack: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("안녕하세요. Pleos 음성 안내 테스트입니다.") }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Pleos 음성 SDK 테스트", style = MaterialTheme.typography.headlineSmall)
        Text("Speech Service / ON_DEVICE / 한국어")
        Text("TTS 상태: ${state.ttsStatus.label()}" + if (state.speaking) " · 재생 중" else "")
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = { text = it },
            label = { Text("음성으로 읽을 문장") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSpeak(text) },
                enabled = state.ttsStatus == EngineStatus.READY && text.isNotBlank() && !state.speaking
            ) { Text("TTS 재생") }
            Button(onClick = onStopSpeaking, enabled = state.speaking) { Text("재생 중지") }
        }
        Text("STT 상태: ${state.sttStatus.label()}" + if (state.listening) " · 듣는 중" else "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStartListening,
                enabled = state.sttStatus == EngineStatus.READY && !state.listening
            ) { Text("STT 시작") }
            Button(onClick = onStopListening, enabled = state.listening) { Text("인식 중지") }
        }
        Text("인식 결과: ${state.transcript.ifBlank { "아직 결과 없음" }}")
        if (state.transcriptCompleted) Text("최종 인식 결과")
        state.error?.let { Text("오류: $it", color = MaterialTheme.colorScheme.error) }
        Text("마이크는 Pleos 차량 에뮬레이터 설정에서 활성화해야 함")
        Button(onClick = onBack) { Text("차량 화면으로") }
    }
}

private fun EngineStatus.label(): String = when (this) {
    EngineStatus.INITIALIZING -> "초기화 중"
    EngineStatus.READY -> "준비 완료"
    EngineStatus.ERROR -> "초기화 실패"
}
