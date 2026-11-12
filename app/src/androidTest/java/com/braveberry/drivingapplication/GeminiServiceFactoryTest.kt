package com.braveberry.drivingapplication

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.braveberry.ai.remote.BuildConfig
import com.braveberry.ai.remote.api.FirebaseGeminiApiService
import com.braveberry.ai.remote.factory.GeminiServiceFactory
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(PublicPreviewAPI::class)
class GeminiServiceFactoryTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        // Firebase 초기화 확인
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build()

            FirebaseApp.initializeApp(context, options)
        }

        // 핵심: App Check Debug Provider 등록
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
    }


    @Test
    fun `Factory가 GeminiService를 생성한다`() {
        val service = GeminiServiceFactory.create()

        assertTrue(service is FirebaseGeminiApiService)
    }





    @Test
    fun `Firebase Gemini 스트리밍 통신 테스트`() = runBlocking {
        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).templateGenerativeModel()

        val fullText = StringBuilder()
        var chunkCount = 0

        model.generateContentStream(
            templateId = "guide-v1",
            inputs = mapOf(
                "placeName" to "경복궁",
                "region" to "대한민국 서울 종로구",
                "language" to "English"
            )
        ).collect { response ->
            val chunk = response.text.orEmpty()

            if (chunk.isNotEmpty()) {
                chunkCount++
                fullText.append(chunk)

                Log.d("GEMINI_TEST", "Chunk $chunkCount: $chunk")
            }
        }

        Log.d("GEMINI_TEST", "전체 응답: $fullText")
        Log.d("GEMINI_TEST", "총 청크 수: $chunkCount")

        assertTrue(fullText.isNotBlank())
        assertTrue(chunkCount > 0)
    }
}