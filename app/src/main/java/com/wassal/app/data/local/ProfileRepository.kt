package com.wassal.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wassal.app.data.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the local profile in Room plus lightweight, non-sensitive flags in
 * SharedPreferences. Nothing is ever uploaded anywhere.
 *
 * The encrypted store is preferred, but if the device cannot initialise a
 * master key (rare OEM/keystore failures) we fall back to a plain private
 * preferences file instead of crashing on first launch.
 */
@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: ProfileDao
) {

    private val prefs: SharedPreferences by lazy {
        try {
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
        } catch (t: Throwable) {
            android.util.Log.w("ProfileRepository", "encrypted prefs unavailable, using plain", t)
            context.getSharedPreferences("wassal_plain", Context.MODE_PRIVATE)
        }
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
