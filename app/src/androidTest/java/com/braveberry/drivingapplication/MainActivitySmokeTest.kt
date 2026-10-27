package com.braveberry.drivingapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Hilt 앱 그래프를 조립하고 실제 Activity의 Compose 화면을 검증하는 계측 테스트. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: TestRule = RuleChain.outerRule(hiltRule).around(composeRule)

    @Test fun dashboardIsVisible() {
        composeRule.onNodeWithText("차량 상태").assertIsDisplayed()
    }
}
