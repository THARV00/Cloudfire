package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale

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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SAVED_UID = "saved_uid"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_NAME = "saved_name"
        private const val KEY_SAVED_IS_DEV = "saved_is_developer"

        fun normalizeUid(email: String): String {
            val cleanEmail = email.trim().lowercase(Locale.ROOT)
            if (cleanEmail == DEVELOPER_EMAIL.lowercase(Locale.ROOT)) {
                return "dev_tharv_07"
            }
            return "user_" + cleanEmail.replace("[^a-z0-9]".toRegex(), "_")
        }
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

        // Restore saved session permanently across app launches
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val savedEmail = prefs.getString(KEY_SAVED_EMAIL, null)
        val savedUid = prefs.getString(KEY_SAVED_UID, null)
        val savedName = prefs.getString(KEY_SAVED_NAME, null)

        if ((isLoggedIn || savedEmail != null) && !savedEmail.isNullOrBlank()) {
            val isDev = prefs.getBoolean(KEY_SAVED_IS_DEV, false) ||
                    savedEmail.equals(DEVELOPER_EMAIL, ignoreCase = true)
            val uid = savedUid?.ifBlank { null } ?: normalizeUid(savedEmail)
            _currentUser.value = UserProfile(
                uid = uid,
                email = savedEmail,
                displayName = savedName?.ifBlank { null } ?: savedEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                storageLimitBytes = if (isDev) Long.MAX_VALUE else 10L * 1024 * 1024 * 1024,
                isDeveloper = isDev
            )
        } else {
            // Check if Firebase Auth has an active cached user
            val fbUser = firebaseAuth?.currentUser
            if (fbUser != null && fbUser.email != null) {
                val profile = UserProfile(
                    uid = normalizeUid(fbUser.email!!),
                    email = fbUser.email!!,
                    displayName = fbUser.displayName ?: fbUser.email!!.substringBefore("@").replaceFirstChar { it.uppercase() }
                )
                saveSession(profile)
                _currentUser.value = profile
            }
        }
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()
        val cleanEmail = trimmedEmail.lowercase(Locale.ROOT)

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (trimmedPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Developer Superuser Account
        if (cleanEmail == DEVELOPER_EMAIL.lowercase(Locale.ROOT)) {
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

        // Check local registered credentials
        val savedPass = prefs.getString("pwd_$cleanEmail", null)
        val savedName = prefs.getString("name_$cleanEmail", null)
        if (savedPass != null && savedPass != trimmedPass) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        // Try Firebase Auth if available
        val fbAuth = firebaseAuth
        if (fbAuth != null) {
            try {
                val authResult = fbAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
                val user = authResult.user
                if (user != null) {
                    val profile = UserProfile(
                        uid = normalizeUid(user.email ?: trimmedEmail),
                        email = user.email ?: trimmedEmail,
                        displayName = savedName ?: user.displayName ?: trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                    )
                    saveSession(profile)
                    _currentUser.value = profile
                    return Result.success(profile)
                }
            } catch (e: Exception) {
                Log.d("AuthRepository", "Firebase signIn fallback: ${e.message}")
            }
        }

        // Standard direct sign in session with deterministic UID
        val uid = normalizeUid(trimmedEmail)
        val displayName = savedName ?: trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val profile = UserProfile(
            uid = uid,
            email = trimmedEmail,
            displayName = displayName
        )

        // Store account locally so user can always log in
        prefs.edit()
            .putString("pwd_$cleanEmail", trimmedPass)
            .putString("name_$cleanEmail", displayName)
            .apply()

        saveSession(profile)
        _currentUser.value = profile
        return Result.success(profile)
    }

    suspend fun signUp(name: String, email: String, password: String): Result<UserProfile> {
        val trimmedName = name.trim().ifEmpty { "CloudFire User" }
        val trimmedEmail = email.trim()
        val trimmedPass = password.trim()
        val cleanEmail = trimmedEmail.lowercase(Locale.ROOT)

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (trimmedPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        // Developer Superuser Account check
        if (cleanEmail == DEVELOPER_EMAIL.lowercase(Locale.ROOT)) {
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
                        uid = normalizeUid(user.email ?: trimmedEmail),
                        email = user.email ?: trimmedEmail,
                        displayName = trimmedName
                    )
                    prefs.edit()
                        .putString("pwd_$cleanEmail", trimmedPass)
                        .putString("name_$cleanEmail", trimmedName)
                        .apply()
                    saveSession(profile)
                    _currentUser.value = profile
                    return Result.success(profile)
                }
            } catch (e: Exception) {
                Log.d("AuthRepository", "Firebase signUp fallback: ${e.message}")
            }
        }

        val uid = normalizeUid(trimmedEmail)
        val profile = UserProfile(
            uid = uid,
            email = trimmedEmail,
            displayName = trimmedName
        )

        // Store account locally
        prefs.edit()
            .putString("pwd_$cleanEmail", trimmedPass)
            .putString("name_$cleanEmail", trimmedName)
            .apply()

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
            uid = "guest_user",
            email = "guest@cloudfire.io",
            displayName = "Guest User",
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

        // Clear active session keys while preserving registered credentials
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_SAVED_UID)
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_SAVED_NAME)
            .remove(KEY_SAVED_IS_DEV)
            .commit()

        _currentUser.value = null
    }

    private fun saveSession(profile: UserProfile) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_SAVED_UID, profile.uid)
            .putString(KEY_SAVED_EMAIL, profile.email)
            .putString(KEY_SAVED_NAME, profile.displayName)
            .putBoolean(KEY_SAVED_IS_DEV, profile.isDeveloper)
            .commit() // Commit synchronously to ensure it persists immediately to disk
    }
}
