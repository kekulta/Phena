package tech.kekulta.phena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class AnimationPlayerState {
    var frameNum by mutableIntStateOf(0)
    var isPlaying by mutableStateOf(false)
    var animation by mutableStateOf<Anim?>(null)
}

fun AnimationPlayerState.set(anim: Anim) {
    animation = anim
    frameNum = 0
    isPlaying = false
}

@Composable
fun rememberAnimationPlayerState() = remember { AnimationPlayerState() }

