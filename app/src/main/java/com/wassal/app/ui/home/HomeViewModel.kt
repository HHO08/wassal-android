package com.wassal.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wassal.app.data.local.ProfileRepository
import com.wassal.app.data.local.RoomDao
import com.wassal.app.data.model.Room
import com.wassal.app.util.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val roomDao: RoomDao
) : ViewModel() {

    /** Generate a cryptographically random room id and its deep link. */
    fun createRoom(onCreated: (Room) -> Unit) {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        val roomId = bytes.joinToString("") { "%02x".format(it) }
        val deepLink = "${AppConstants.DEEP_LINK_SCHEME}://${AppConstants.DEEP_LINK_HOST}/$roomId"
        val room = Room(id = roomId, deepLink = deepLink)
        viewModelScope.launch {
            roomDao.insert(room)
            onCreated(room)
        }
    }

    fun shareRoom(deepLink: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, deepLink)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Invite to Wassal"))
    }
}
