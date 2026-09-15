package com.studypilot.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.studypilot.app.data.auth.GoogleAuthResult
import com.studypilot.app.ui.navigation.Screen
import com.studypilot.app.ui.navigation.StudyPilotNavGraph
import com.studypilot.app.ui.screens.LockScreen
import com.studypilot.app.ui.theme.CamelPrimary
import com.studypilot.app.ui.theme.IvoryBackground
import com.studypilot.app.ui.theme.StudyPilotTheme
import com.studypilot.app.ui.viewmodel.StudyPilotViewModel
import com.studypilot.app.ui.viewmodel.StudyPilotViewModelFactory

class MainActivity : FragmentActivity() {

    private val app by lazy { application as StudyPilotApplication }

    val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val authResult = app.googleAuthManager.handleSignInResult(result.data)
        when (authResult) {
            is GoogleAuthResult.Success -> {
                viewModel.updateGoogleAccount(authResult.user)
            }
            is GoogleAuthResult.Cancelled -> {
                // User cancelled sign in
            }
            is GoogleAuthResult.Failure -> {
                // Failure handled safely without crashing
            }
        }
    }

    val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled; app continues working normally regardless
    }

    private val viewModel: StudyPilotViewModel by viewModels {
        StudyPilotViewModelFactory(
            repository = app.repository,
            appLockManager = app.appLockManager,
            googleAuthManager = app.googleAuthManager,
            notificationManager = app.notificationManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+ (graceful, non-intrusive)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!viewModel.hasNotificationPermission()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            StudyPilotTheme {
                val isLoading by viewModel.isLoading.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val isAppLocked by viewModel.isAppLocked.collectAsState()
                val appLockSettings by viewModel.appLockSettings.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IvoryBackground
                ) {
                    if (isAppLocked) {
                        LockScreen(
                            isBiometricAvailable = viewModel.isBiometricHardwareAvailable(),
                            isBiometricEnabled = appLockSettings.isBiometricEnabled,
                            onVerifyPin = { pin -> viewModel.verifyAppLockPin(pin) },
                            onBiometricClick = {
                                viewModel.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = { viewModel.unlockApp() },
                                    onError = { /* fallback to PIN */ }
                                )
                            },
                            onUnlocked = { viewModel.unlockApp() }
                        )
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CamelPrimary)
                        }
                    } else {
                        val navController = rememberNavController()
                        val startDestination = if (userProfile != null && userProfile!!.isOnboarded) {
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }

                        StudyPilotNavGraph(
                            navController = navController,
                            viewModel = viewModel,
                            startDestination = startDestination,
                            onLaunchGoogleSignIn = {
                                googleSignInLauncher.launch(app.googleAuthManager.getSignInIntent())
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.appLockManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        app.appLockManager.onAppBackgrounded()
    }
}
