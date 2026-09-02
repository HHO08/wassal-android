package com.wassal.app.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wassal.app.ui.auth.AuthViewModel
import com.wassal.app.ui.auth.ProfileSetupScreen
import com.wassal.app.ui.home.HomeScreen
import com.wassal.app.ui.home.HomeViewModel
import com.wassal.app.ui.room.RoomScreen
import com.wassal.app.ui.room.RoomViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Top level navigation. A deep link of the form wassal://room/{id} is parsed
 * from the launch intent (handled in the Activity) and turned into a room
 * navigation target.
 */
@Composable
fun AppRoot(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    var pendingRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingIsHost by rememberSaveable { mutableStateOf(false) }

    if (authViewModel.showSetup.value) {
        // Profile not finished yet. The transition to the main app is driven
        // purely by showSetup flipping to false inside saveProfile. We must
        // NOT call navController.navigate here: the NavHost graph below is not
        // composed yet, so navigating would throw IllegalStateException and
        // crash the app.
        ProfileSetupScreen(
            viewModel = authViewModel,
            onDone = { /* no-op: state change re-renders into the NavHost */ }
        )
    } else {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                val vm: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = vm,
                    onOpenRoom = { roomId, isHost ->
                        pendingRoomId = roomId
                        pendingIsHost = isHost
                        navController.navigate("room")
                    }
                )
            }
            composable("room") {
                val vm: RoomViewModel = hiltViewModel()
                RoomScreen(
                    viewModel = vm,
                    roomId = pendingRoomId ?: "",
                    isHost = pendingIsHost,
                    onStartCall = {
                        navController.navigate("call")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("call") {
                val vm: com.wassal.app.ui.call.CallViewModel = hiltViewModel()
                com.wassal.app.ui.call.CallScreen(
                    viewModel = vm,
                    roomId = pendingRoomId ?: "",
                    onEnd = {
                        navController.popBackStack("home", inclusive = false)
                    }
                )
            }
        }
    }
}

/** Parse a deep link and extract the room id, if any. */
fun extractRoomId(uri: Uri?): String? {
    return uri?.let {
        if (it.scheme == "wassal" && it.host == "room") {
            it.lastPathSegment
        } else null
    }
}
