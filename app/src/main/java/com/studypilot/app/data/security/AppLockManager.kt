package com.studypilot.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.studypilot.app.data.model.AppLockSettings
import com.studypilot.app.data.model.AutoLockTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class AppLockManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("studypilot_security_prefs", Context.MODE_PRIVATE)

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppLockSettings> = _settings.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    init {
        // If app lock is enabled and pin is set, start locked
        val initialSettings = loadSettings()
        if (initialSettings.isAppLockEnabled && initialSettings.hasPinSet) {
            _isLocked.value = true
        }
    }

    fun loadSettings(): AppLockSettings {
        val enabled = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        val biometric = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val timeoutName = prefs.getString(KEY_AUTO_LOCK_TIMEOUT, AutoLockTimeout.AFTER_1_MIN.name) ?: AutoLockTimeout.AFTER_1_MIN.name
        val timeout = try {
            AutoLockTimeout.valueOf(timeoutName)
        } catch (e: Exception) {
            AutoLockTimeout.AFTER_1_MIN
        }
        val hasPin = prefs.contains(KEY_PIN_HASH)

        return AppLockSettings(
            isAppLockEnabled = enabled,
            isBiometricEnabled = biometric,
            autoLockTimeout = timeout,
            hasPinSet = hasPin
        )
    }

    fun setPin(pin: String) {
        if (pin.length != 4) return
        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_LOCK_ENABLED, true)
            .apply()

        _settings.value = loadSettings()
        _isLocked.value = false
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false

        val inputHash = hashPin(inputPin, storedSalt)
        val isValid = (inputHash == storedHash)
        if (isValid) {
            _isLocked.value = false
        }
        return isValid
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun lockManually() {
        if (_settings.value.isAppLockEnabled && _settings.value.hasPinSet) {
            _isLocked.value = true
        }
    }

    fun toggleAppLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
        _settings.value = loadSettings()
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _settings.value = loadSettings()
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        prefs.edit().putString(KEY_AUTO_LOCK_TIMEOUT, timeout.name).apply()
        _settings.value = loadSettings()
    }

    fun isBiometricHardwareAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isBiometricHardwareAvailable()) {
            onError("Biometric authentication not supported on this device.")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _isLocked.value = false
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Please use your 4-digit PIN.")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("StudyPilot Security")
            .setSubtitle("Authenticate to unlock your academic workspace")
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val currentSettings = _settings.value
        if (!currentSettings.isAppLockEnabled || !currentSettings.hasPinSet) return

        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        if (lastBackgroundTimestamp > 0 && elapsed >= currentSettings.autoLockTimeout.millis) {
            _isLocked.value = true
        }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashedBytes)
    }

    companion object {
        private const val KEY_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "key_auto_lock_timeout"
        private const val KEY_PIN_HASH = "key_pin_sha256_hash"
        private const val KEY_PIN_SALT = "key_pin_salt"
    }
}
