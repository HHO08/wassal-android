package com.wassal.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Landing screen. Lets the user create a new room (host) and share its deep
 * link, or join an existing one via a pasted link.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenRoom: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wassal", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Private peer to peer calls. No servers, no storage, 100% you.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Button(
            onClick = {
                viewModel.createRoom { room ->
                    viewModel.shareRoom(room.deepLink)
                    onOpenRoom(room.id, true)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create a room")
        }
    }
}
