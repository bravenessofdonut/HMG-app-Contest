package com.braveberry.voice.data.di

import com.braveberry.voice.data.impl.RemoteTtsRepositoryImpl
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.lifecycle.RetainedLifecycle
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Test

class RemoteTtsRepositoryModuleTest {

    @Test
    fun releasesOnlyWhenRetainedLifecycleIsCleared() {
        val impl = mockk<RemoteTtsRepositoryImpl>(relaxUnitFun = true)
        val lifecycle = mockk<ActivityRetainedLifecycle>()
        val listener = slot<RetainedLifecycle.OnClearedListener>()
        every { lifecycle.addOnClearedListener(capture(listener)) } returns Unit

        val provided = RemoteTtsRepositoryModule.provideRemoteTtsRepository(impl, lifecycle)

        assertSame(impl, provided)
        // 화면 이탈·재진입 동안에는 해제되지 않아야 함.
        verify(exactly = 0) { impl.release() }

        listener.captured.onCleared()
        verify(exactly = 1) { impl.release() }
    }
}
