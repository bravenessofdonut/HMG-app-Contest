package com.braveberry.domain.usecase.voice

import com.braveberry.domain.model.voice.PlaybackPhase
import com.braveberry.domain.model.voice.PlaybackPlan
import com.braveberry.domain.model.voice.VoiceSessionState
import com.braveberry.domain.model.voice.VoiceTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** SDK 없이 재생 정책 자체를 검증함. */
class VoiceSessionTest {
    private val first = VoiceTurn("첫 대사", "eunwoo", "A")
    private val second = VoiceTurn("둘째 대사", "haeun", "B")

    @Test fun onlyAcknowledgedPlayingCanAdvanceToNextTurn() {
        val starting = PlaybackPlan().begin(first, second)!!
        assertNull(starting.finished()) // onStart 없이 온 오래된 onDone은 성공 취급하지 않음.
        val playing = starting.started()!!
        val completed = playing.finished()!!
        assertEquals(second, completed.following)
        assertEquals(PlaybackPhase.Idle, completed.completed.phase)
    }

    @Test fun stoppingDiscardsThePendingTurnAndCannotAcceptAnotherStart() {
        val stopping = PlaybackPlan().begin(first, second)!!.started()!!.requestStop()!!
        assertNull(stopping.begin(second))
        assertNull(stopping.finished())
        assertNull(stopping.next)
        assertEquals(PlaybackPhase.Idle, stopping.stopped()!!.phase)
    }

    @Test fun failedStateCannotStartAnotherTurnWithoutNewSession() {
        val failed = PlaybackPlan().begin(first)!!.failed("START_TIMEOUT")
        assertTrue(failed.phase is PlaybackPhase.Failed)
        assertNull(failed.begin(second))
    }

    @Test fun koreanOnlyPodcastIsEnforcedBySessionState() {
        val state = VoiceSessionState(speakerLanguageTags = mapOf(
            "eunwoo" to "ko-KR", "haeun" to "ko-KR", "sabine" to "fr-CA"
        ))
        assertTrue(state.canPreview("eunwoo", "haeun"))
        assertFalse(state.canPreview("sabine", "eunwoo"))
        assertFalse(state.canPreview("eunwoo", "eunwoo"))
    }
}
