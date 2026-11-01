package com.braveberry.presentation.voice.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveberry.presentation.voice.viewmodel.VoiceViewModel

@Composable
fun VoiceRoute(onBack: () -> Unit, viewModel: VoiceViewModel = hiltViewModel()) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()
    VoiceScreen(
        state = state,
        onSpeak = viewModel::speak,
        onPreviewDialogue = viewModel::previewDialogue,
        onStopSpeaking = viewModel::stopSpeaking,
        onStartListening = viewModel::startListening,
        onStopListening = viewModel::stopListening,
        onReconnect = viewModel::connect,
        onBack = onBack
    )
}
