package tech.kekulta.phena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun AnimationPlayer(modifier: Modifier = Modifier, state: AnimationPlayerState) {
    LaunchedEffect(state.animation, state.isPlaying) {
        if (!state.isPlaying) return@LaunchedEffect
        val anim = state.animation ?: return@LaunchedEffect

        val frameTime = 1000 / anim.frameRate
        val startFrame = state.frameNum
        var start = 0L

        try {
            while (isActive) {
                // convert to millis
                val nextFrame = awaitFrame() / 1_000_000
                if (start == 0L) {
                    start = nextFrame
                }
                state.frameNum =
                    (((nextFrame - start) / frameTime + startFrame) % anim.frames.size).toInt()
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    state.animation?.let { anim ->
        val frame = anim.frames[state.frameNum]

        Canvas(
            modifier = modifier
                .padding(16.dp)
                .aspectRatio(anim.aspectRatio.calculate())
                .clip(shape = RoundedCornerShape(20.dp))
                .background(frame.background)
        ) {
            withLayerTransform({ scale(size.width, size.width, Offset.Zero) }) {
                drawFrame(frame)
            }
        }
    } ?: run {
        Text("No Animation Selected")
    }
}
