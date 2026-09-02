package com.wassal.app.ui.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wassal.app.data.local.ProfileRepository
import com.wassal.app.media.audio.VoiceMessageRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val voiceMessageRecorder: VoiceMessageRecorder
) : ViewModel() {

    val showSetup = mutableStateOf(true)
    val showMicPermission = mutableStateOf(false)

    init {
        viewModelScope.launch {
            val onboarded = profileRepository.isOnboarded()
            val hasProfile = profileRepository.getProfile() != null
            showSetup.value = !(onboarded && hasProfile)
        }
    }

    fun onMicPermissionResult(granted: Boolean) {
        if (granted) {
            showMicPermission.value = false
            completeOnboardingIfReady()
        } else {
            showMicPermission.value = true
        }
    }

    fun requestMicPermission() {
        // Controlled from the Activity because it needs the launcher.
        showMicPermission.value = false
    }

    fun saveProfile(name: String, avatarBase64: String?) {
        viewModelScope.launch {
            try {
                profileRepository.saveProfile(name, avatarBase64)
                profileRepository.markOnboarded()
                voiceMessageRecorder.setUserName(name)
            } catch (t: Throwable) {
                android.util.Log.e("AuthViewModel", "saveProfile failed", t)
            } finally {
                // Always leave the setup screen, even on a rare storage error.
                showSetup.value = false
            }
        }
    }

    private fun completeOnboardingIfReady() {
        viewModelScope.launch {
            profileRepository.markOnboarded()
            showMicPermission.value = false
        }
    }
}
