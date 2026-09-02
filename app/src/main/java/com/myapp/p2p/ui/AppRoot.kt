package com.myapp.p2p.ui

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
import com.myapp.p2p.ui.auth.AuthViewModel
import com.myapp.p2p.ui.auth.ProfileSetupScreen
import com.myapp.p2p.ui.home.HomeScreen
import com.myapp.p2p.ui.home.HomeViewModel
import com.myapp.p2p.ui.room.RoomScreen
import com.myapp.p2p.ui.room.RoomViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Top level navigation. A deep link of the form myapp://room/{id} is parsed
 * from the launch intent (handled in the Activity) and turned into a room
 * navigation target.
 */
@Composable
fun AppRoot(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    var pendingRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingIsHost by rememberSaveable { mutableStateOf(false) }

    // Exposed for the Activity to feed deep links into navigation.
    LaunchedEffect(Unit) {
        // A real implementation wires the activity intent here. We expose a
        // companion function that the Activity calls.
    }

    if (authViewModel.showSetup.value) {
        ProfileSetupScreen(
            viewModel = authViewModel,
            onDone = { navController.navigate("home") }
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
                val vm: com.myapp.p2p.ui.call.CallViewModel = hiltViewModel()
                com.myapp.p2p.ui.call.CallScreen(
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
        if (it.scheme == "myapp" && it.host == "room") {
            it.lastPathSegment
        } else null
    }
}
