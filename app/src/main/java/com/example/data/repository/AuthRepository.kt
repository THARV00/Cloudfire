package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val storageLimitBytes: Long = 10L * 1024 * 1024 * 1024, // 10 GB Free MediaFire Tier
    val isAnonymous: Boolean = false,
    val isDeveloper: Boolean = false
)

class AuthRepository(private val context: Context) {
    companion object {
        const val DEVELOPER_EMAIL = "devlopertharv@gmail.com"
        const val DEVELOPER_PASSWORD = "tharvthala07"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cloudfire_auth", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Firebase not initialized or missing config, fallback to local auth", e)
        }

        // Restore saved session
        val savedUid = prefs.getString("saved_uid", null)
        val savedEmail = prefs.getString("saved_email", null)
        val savedName = prefs.getString("saved_name", null)
        val savedIsDev = prefs.getBoolean("saved_is_developer", false) || savedEmail?.equals(DEVELOPER_EMAIL, ignoreCase = true) == true

        if (savedUid != null && savedEmail != null) {
            _currentUser.value = UserProfile(
                uid = savedUid,
                email = savedEmail,
                displayName = savedName ?: savedEmail.substringBefore("@"),
                storageLimitBytes = if (savedIsDev) Long.MAX_VALUE else 10L * 1024 * 1024 * 1024,
                isDeveloper = savedIsDev
            )
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (trimmedPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Developer Superuser Account
        if (trimmedEmail.equals(DEVELOPER_EMAIL, ignoreCase = true)) {
            if (trimmedPass == DEVELOPER_PASSWORD) {
                val devProfile = UserProfile(
                    uid = "dev_tharv_07",
                    email = DEVELOPER_EMAIL,
                    displayName = "Tharv (Developer)",
                    storageLimitBytes = Long.MAX_VALUE,
                    isDeveloper = true
                )
                saveSession(devProfile)
                _currentUser.value = devProfile
                return Result.success(devProfile)
            } else {
                return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
            }
        }

        // Try Firebase Auth if available
        val fbAuth = firebaseAuth
        if (fbAuth != null) {
            try {
                val authResult = fbAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                val user = authResult.user
                if (user != null) {
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: trimmedEmail,
                        displayName = user.displayName ?: trimmedEmail.substringBefore("@")
                    )
                    saveSession(profile)
                    _currentUser.value = profile
                    return Result.success(profile)
                }
            } catch (e: Exception) {
                Log.d("AuthRepository", "Firebase signIn failed or unconfigured, using account credentials: ${e.message}")
            }
        }

        // Standard direct sign in session
        val uid = "user_${trimmedEmail.replace("[^a-zA-Z0-9]".toRegex(), "")}"
        val profile = UserProfile(
            uid = uid,
            email = trimmedEmail,
            displayName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        )
        saveSession(profile)
        _currentUser.value = profile
        return Result.success(profile)
    }

    suspend fun signUp(name: String, email: String, password: String): Result<UserProfile> {
        val trimmedName = name.trim().ifEmpty { "CloudFire User" }
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (trimmedPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Developer Superuser Account check
        if (trimmedEmail.equals(DEVELOPER_EMAIL, ignoreCase = true)) {
            if (trimmedPass == DEVELOPER_PASSWORD) {
                val devProfile = UserProfile(
                    uid = "dev_tharv_07",
                    email = DEVELOPER_EMAIL,
                    displayName = if (trimmedName != "CloudFire User") trimmedName else "Tharv (Developer)",
                    storageLimitBytes = Long.MAX_VALUE,
                    isDeveloper = true
                )
                saveSession(devProfile)
                _currentUser.value = devProfile
                return Result.success(devProfile)
            } else {
                return Result.failure(IllegalArgumentException("This email address is already registered. Please sign in."))
            }
        }

        val fbAuth = firebaseAuth
        if (fbAuth != null) {
            try {
                val authResult = fbAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                val user = authResult.user
                if (user != null) {
                    val profile = UserProfile(
                        uid = user.uid,
                        email = user.email ?: trimmedEmail,
                        displayName = trimmedName
                    )
                    saveSession(profile)
                    _currentUser.value = profile
                    return Result.success(profile)
                }
            } catch (e: Exception) {
                Log.d("AuthRepository", "Firebase signUp error: ${e.message}")
            }
        }

        val uid = "user_${System.currentTimeMillis()}"
        val profile = UserProfile(
            uid = uid,
            email = trimmedEmail,
            displayName = trimmedName
        )
        saveSession(profile)
        _currentUser.value = profile
        return Result.success(profile)
    }

    fun signInAsDeveloper(): UserProfile {
        val devProfile = UserProfile(
            uid = "dev_tharv_07",
            email = DEVELOPER_EMAIL,
            displayName = "Tharv (Developer)",
            storageLimitBytes = Long.MAX_VALUE,
            isAnonymous = false,
            isDeveloper = true
        )
        saveSession(devProfile)
        _currentUser.value = devProfile
        return devProfile
    }

    fun signInAsGuest(): UserProfile {
        val profile = UserProfile(
            uid = "guest_${System.currentTimeMillis().toString().takeLast(6)}",
            email = "guest@cloudfire.io",
            displayName = "Demo User",
            isAnonymous = true
        )
        saveSession(profile)
        _currentUser.value = profile
        return profile
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (ignored: Exception) {}
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    private fun saveSession(profile: UserProfile) {
        prefs.edit()
            .putString("saved_uid", profile.uid)
            .putString("saved_email", profile.email)
            .putString("saved_name", profile.displayName)
            .putBoolean("saved_is_developer", profile.isDeveloper)
            .apply()
    }
}
