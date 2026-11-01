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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import com.braveberry.domain.model.voice.VoiceEvent
import com.braveberry.presentation.voice.viewmodel.EngineStatus
import com.braveberry.presentation.voice.viewmodel.ListeningPhase
import com.braveberry.presentation.voice.viewmodel.VoiceUiState

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onSpeak: (String, String) -> Unit,
    onPreviewDialogue: (String, String, String, String) -> Unit,
    onStopSpeaking: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onBack: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("안녕하세요. Pleos TTS 음성 테스트입니다.") }
    var aText by rememberSaveable { mutableStateOf("안녕하세요, 오늘은 미래의 자동차에 대해 이야기해 볼게요.") }
    var bText by rememberSaveable { mutableStateOf("좋아요. 자동차가 이동 수단을 넘어 공간이 되고 있다는 점이 흥미롭네요.") }
    var rawMain by rememberSaveable { mutableStateOf("") }
    var rawA by rememberSaveable { mutableStateOf("") }
    var rawB by rememberSaveable { mutableStateOf("") }
    // 기본 음성은 setSpeakerType()을 호출하지 않음.
    val values = state.availableSpeakers
    val mainSpeaker = rawMain.takeIf { it in values }.orEmpty()
    val aSpeaker = rawA.takeIf { it in values } ?: values.firstOrNull().orEmpty()
    val bSpeaker = rawB.takeIf { it in values } ?: values.getOrNull(1).orEmpty()
    val choiceAvailable = values.isNotEmpty() &&
        (state.speakerKind == VoiceEvent.SpeakerKind.MANUAL_STRING ||
            state.speakerKind == VoiceEvent.SpeakerKind.ENUM_VALUES)
    val canPlay = state.ttsStatus == EngineStatus.READY && !state.listening && !state.speaking
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pleos 음성 실험실", style = MaterialTheme.typography.headlineSmall)
        Text("Pleos Speech Service 3.3.1 · ON_DEVICE · ko-KR")
        Text("TTS: ${state.ttsStatus.label()}" + if (state.speaking) " · 재생 중" else "")
        Text("화자 타입: ${state.speakerParameter}")
        when (state.speakerKind) {
            VoiceEvent.SpeakerKind.ENUM_VALUES -> Text("SDK에서 제공하는 화자 목록 사용 중")
            VoiceEvent.SpeakerKind.MANUAL_STRING -> Text("공식 문서에 안내된 화자 10명 · 언어별 선택 가능")
            VoiceEvent.SpeakerKind.MANUAL_NUMBER -> Text("현재 SDK의 화자 인자 형식이 문서와 달라 목록 선택을 제공하지 않음")
            VoiceEvent.SpeakerKind.UNSUPPORTED -> Text("이 환경에서 호출 가능한 화자 변경 시그니처를 찾지 못했음")
        }
        if (choiceAvailable) {
            SpeakerPicker("단일 TTS 화자", state, mainSpeaker, { rawMain = it }, allowDefault = true)
            Text("기본 음성은 화자 설정을 생략함. 변경 후 원래 음색으로 돌아가려면 앱 재시작이 필요할 수 있음")
            Text("최근 화자 변경 요청: ${state.requestedSpeaker ?: "없음"} (실제 음색은 청취 확인 필요)")
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(), value = text,
            onValueChange = { text = it }, label = { Text("읽을 문장") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSpeak(text, if (choiceAvailable) mainSpeaker else "") },
                enabled = canPlay && text.isNotBlank()) {
                Text("TTS 재생")
            }
            Button(onClick = onStopSpeaking, enabled = state.speaking || state.previewRunning) { Text("중지") }
        }
        HorizontalDivider()
        Text("2인 팟캐스트 대화 시연", style = MaterialTheme.typography.titleMedium)
        Text("현재는 준비된 문장 두 개를 순서대로 읽는 테스트임. AI 대본 생성 기능은 포함하지 않음")
        if (choiceAvailable) {
            SpeakerPicker("진행자 A", state, aSpeaker, { rawA = it })
            SpeakerPicker("진행자 B", state, bSpeaker, { rawB = it })
            Text("선택한 화자 언어에 따라 TTS locale도 변경됨. 각 언어의 문장으로 시험하는 것을 권장함")
        }
        OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = aText,
            onValueChange = { aText = it }, label = { Text("A 대사") })
        OutlinedTextField(modifier = Modifier.fillMaxWidth(), value = bText,
            onValueChange = { bText = it }, label = { Text("B 대사") })
        Button(onClick = { onPreviewDialogue(aText, bText, aSpeaker, bSpeaker) },
            enabled = canPlay && choiceAvailable && aSpeaker.isNotBlank() && bSpeaker.isNotBlank() &&
                aSpeaker != bSpeaker && aText.isNotBlank() && bText.isNotBlank()) {
            Text("A → B 순차 재생")
        }
        if (state.previewRunning) Text("진행자 ${state.previewTurn} 재생 중 · onDone 후 다음 발화")
        if (!choiceAvailable) Text("화자 변경 불가: 현재 SDK에서 두 화자 전환 시연 비활성화")
        HorizontalDivider()
        Text("STT 실시간 상태", style = MaterialTheme.typography.titleMedium)
        Text("서비스: ${state.sttStatus.label()} · 단계: ${state.listeningPhase.name}")
        Text("마지막 SDK 이벤트: ${state.lastSttEvent}")
        Text("입력 RMS (SDK 원본 단위): ${state.sttRms?.toString() ?: "대기 중"}")
        Text("발화 구간(on/off): ${state.sttEndpoint?.toString() ?: "대기 중"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartListening,
                enabled = state.sttStatus == EngineStatus.READY && !state.listening && !state.speaking &&
                    !state.previewRunning && state.listeningPhase != ListeningPhase.REQUESTED) {
                Text("STT 시작")
            }
            Button(onClick = onStopListening,
                enabled = state.listening || state.listeningPhase == ListeningPhase.REQUESTED) { Text("STT 중지") }
        }
        Text("인식 결과: ${state.transcript.ifBlank { "아직 없음" }}")
        Text(if (state.transcriptCompleted) "최종 결과" else "중간 결과 또는 대기 중")
        state.error?.let { Text("오류: $it", color = MaterialTheme.colorScheme.error) }
        Text("실제 STT 테스트는 Pleos 에뮬레이터 마이크 접근을 설정해야 함")
        Button(onClick = onBack) { Text("차량 화면으로") }
    }
}

/** 공식 Pleos Speech Service 3.3.1 문서에 안내된 화자 이름. 대소문자 유지함. */
private val documentedSpeakers = listOf(
    "한국어 (ko-KR)" to listOf("seowoon", "eunwoo", "haeun", "sanghyeok"),
    "영어 (en-US)" to listOf("lyra", "finley"),
    "스페인어 (es-ES)" to listOf("iris", "arlo"),
    "프랑스어 (fr-FR)" to listOf("sabine", "noah")
)

@Composable
private fun SpeakerPicker(
    title: String,
    state: VoiceUiState,
    value: String,
    onChange: (String) -> Unit,
    allowDefault: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val groups = if (state.speakerKind == VoiceEvent.SpeakerKind.MANUAL_STRING) {
        documentedSpeakers.map { (language, names) ->
            language to names.filter { it in state.availableSpeakers }
        }.filter { it.second.isNotEmpty() }
    } else {
        listOf("SDK 제공 화자" to state.availableSpeakers)
    }

    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Button(onClick = { expanded = true }) {
            Text(value.ifBlank { if (allowDefault) "기본 음성 (설정 생략)" else "화자 선택" })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowDefault) {
                DropdownMenuItem(
                    text = { Text("기본 음성 (화자 미지정)") },
                    onClick = { onChange(""); expanded = false }
                )
                HorizontalDivider()
            }
            groups.forEach { (language, names) ->
                Text(
                    language,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                names.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { onChange(name); expanded = false }
                    )
                }
            }
        }
    }
}

private fun EngineStatus.label(): String = when (this) {
    EngineStatus.INITIALIZING -> "초기화 중"
    EngineStatus.READY -> "준비 완료"
    EngineStatus.ERROR -> "초기화 실패"
}
