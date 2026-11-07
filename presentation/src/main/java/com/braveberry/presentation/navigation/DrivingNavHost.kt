package com.braveberry.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.braveberry.presentation.vehicle.screen.VehicleRoute
import com.braveberry.presentation.voice.screen.VoiceRoute

private object Routes {
    const val DASHBOARD = "vehicle/dashboard"
    const val ABOUT = "vehicle/about"
    const val VOICE = "voice/demo"
}

@Composable
fun DrivingNavHost(
    speedPermissionGranted: Boolean,
    onRequestSpeedPermission: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            VehicleRoute(
                speedPermissionGranted = speedPermissionGranted,
                onRequestSpeedPermission = onRequestSpeedPermission,
                onShowAbout = { navController.navigate(Routes.ABOUT) },
                onShowVoice = { navController.navigate(Routes.VOICE) }
            )
        }
        composable(Routes.VOICE) {
            VoiceRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("속도: AAOS PERF_VEHICLE_SPEED (m/s → km/h)")
                Text("도어: Pleos Vehicle SDK 2.3.2 / 운전석 위치")
                Text("지원 여부는 차량 및 앱 권한에 따라 달라짐")
                Button(onClick = { navController.popBackStack() }) { Text("뒤로") }
            }
        }
    }
}
