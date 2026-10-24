package com.contest.dongheng

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.contest.dongheng.ui.App
import com.contest.dongheng.ui.AppState

class MainActivity : ComponentActivity() {
    private lateinit var state: AppState

    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshLocation()
    }

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        state.micGranted = ok
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = AppState(applicationContext)
        state.micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        state.requestMic = { micPermission.launch(Manifest.permission.RECORD_AUDIO) }
        refreshLocation()
        setContent { App(state, requestLocation = ::askLocation) }
    }

    private fun askLocation() {
        locationPermission.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun refreshLocation() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        state.locationGranted = granted
        if (granted) state.startLocation()
    }

    override fun onDestroy() {
        state.release()
        super.onDestroy()
    }
}
