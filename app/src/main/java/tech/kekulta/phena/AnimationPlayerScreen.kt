package tech.kekulta.phena

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AnimationPlayerScreen(
    state: AnimationPlayerState = rememberAnimationPlayerState(),
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(state.animation?.name ?: "No Name")

        Box(
            modifier = Modifier.weight(1f)
        ) {
            AnimationPlayer(modifier = Modifier.align(Alignment.Center), state = state)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { state.isPlaying = !state.isPlaying }) {
                if (state.isPlaying) {
                    Text("Pause")
                } else {
                    Text("Play")
                }
            }

            Button(onClick = {
                state.isPlaying = false
                state.frameNum = 0
            }, enabled = state.frameNum != 0 || state.isPlaying) { Text("Stop") }
        }
    }
}
