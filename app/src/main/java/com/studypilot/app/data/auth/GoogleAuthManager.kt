package com.studypilot.app.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await

data class GoogleUserData(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)

sealed class GoogleAuthResult {
    data class Success(val user: GoogleUserData) : GoogleAuthResult()
    data class Cancelled(val message: String = "Google Sign-In was cancelled by the user.") : GoogleAuthResult()
    data class Failure(val errorMessage: String) : GoogleAuthResult()
}

class GoogleAuthManager(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun getLastSignedInAccount(): GoogleUserData? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val email = account.email ?: return null
        return GoogleUserData(
            id = account.id ?: email,
            email = email,
            displayName = account.displayName ?: email.substringBefore("@"),
            photoUrl = account.photoUrl?.toString()
        )
    }

    fun handleSignInResult(data: Intent?): GoogleAuthResult {
        if (data == null) {
            return GoogleAuthResult.Failure("No authentication data received from Google.")
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email
            if (email.isNullOrBlank()) {
                GoogleAuthResult.Failure("Unable to retrieve email from Google Account.")
            } else {
                GoogleAuthResult.Success(
                    GoogleUserData(
                        id = account.id ?: email,
                        email = email,
                        displayName = account.displayName ?: email.substringBefore("@"),
                        photoUrl = account.photoUrl?.toString()
                    )
                )
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                    GoogleAuthResult.Cancelled("Sign-in cancelled.")
                GoogleSignInStatusCodes.NETWORK_ERROR ->
                    GoogleAuthResult.Failure("Network error encountered during Google sign-in. Check your connection.")
                GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                    GoogleAuthResult.Failure("Authentication configuration error (${e.statusCode}).")
                else ->
                    GoogleAuthResult.Failure("Google Sign-In failed: ${e.localizedMessage ?: "Code ${e.statusCode}"}")
            }
        } catch (e: Exception) {
            GoogleAuthResult.Failure("Unexpected error during Google Sign-In: ${e.localizedMessage}")
        }
    }

    suspend fun signOut(): Boolean {
        return try {
            googleSignInClient.signOut().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun revokeAccess(): Boolean {
        return try {
            googleSignInClient.revokeAccess().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
