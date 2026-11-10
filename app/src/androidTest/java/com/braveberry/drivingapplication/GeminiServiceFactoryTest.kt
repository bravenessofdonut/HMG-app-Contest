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
import junit.framework.TestCase.assertFalse
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
    fun `실제 테스트 패키지 확인`() {
        val instrumentation =
            InstrumentationRegistry.getInstrumentation()

        Log.d(
            "PACKAGE_TEST",
            "Target Package: ${instrumentation.targetContext.packageName}"
        )

        Log.d(
            "PACKAGE_TEST",
            "Test Package: ${instrumentation.context.packageName}"
        )

        Log.d(
            "PACKAGE_TEST",
            "Firebase App ID: ${FirebaseApp.getInstance().options.applicationId}"
        )
    }


    @Test
    fun `App Check 토큰 발급 테스트`() {
        val firebaseApp = FirebaseApp.getInstance()
        val app = FirebaseApp.getInstance()

        Log.d(
            "FIREBASE_TEST",
            "API Key 끝 4자리: ${app.options.apiKey.takeLast(4)}"
        )
        val appCheck = FirebaseAppCheck.getInstance(firebaseApp)

        // Provider 등록
        appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        Log.d("FIREBASE_TEST", "Provider 등록 완료")

        // 동일한 AppCheck 객체에서 토큰 요청
        val result = Tasks.await(
            appCheck.getAppCheckToken(true)
        )

        assertTrue(result.token.isNotBlank())

        Log.d("FIREBASE_TEST", "토큰 발급 성공")
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