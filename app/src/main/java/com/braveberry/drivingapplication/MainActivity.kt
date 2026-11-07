package com.braveberry.drivingapplication

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.braveberry.drivingapplication.ui.theme.DrivingApplicationTheme
import com.braveberry.presentation.navigation.DrivingNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var speedPermissionGranted by mutableStateOf(false)

    private val requestSpeedPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> speedPermissionGranted = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speedPermissionGranted = hasSpeedPermission()
        enableEdgeToEdge()
        setContent {
            DrivingApplicationTheme {
                DrivingNavHost(
                    speedPermissionGranted = speedPermissionGranted,
                    onRequestSpeedPermission = {
                        if (hasSpeedPermission()) {
                            speedPermissionGranted = true
                        } else {
                            requestSpeedPermission.launch(SPEED_PERMISSION)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        speedPermissionGranted = hasSpeedPermission()
    }

    private fun hasSpeedPermission(): Boolean =
        checkSelfPermission(SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
    }
}
