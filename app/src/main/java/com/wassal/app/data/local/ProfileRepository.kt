package com.wassal.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wassal.app.data.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the local profile in Room plus lightweight, non-sensitive flags in
 * EncryptedSharedPreferences. Nothing is ever uploaded anywhere.
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: ProfileDao
) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "wassal_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val profile: Flow<UserProfile?> = profileDao.observeProfile()

    suspend fun getProfile(): UserProfile? = profileDao.getProfile()

    suspend fun saveProfile(name: String, avatarBase64: String?) {
        profileDao.upsert(UserProfile(displayName = name, avatarBase64 = avatarBase64))
    }

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    fun markOnboarded() = prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()

    private companion object {
        const val KEY_ONBOARDED = "onboarded"
    }
}
