package tech.kekulta.phena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun AnimationCanvas(modifier: Modifier = Modifier, state: AnimationCanvasState) {
    Canvas(modifier = modifier
        .padding(16.dp)
        .aspectRatio(state.aspectRatio.calculate())
        .clip(shape = RoundedCornerShape(20.dp))
        .background(state.backgroundColor)
        .composed {
            if (state.drawingTool == DrawingTool.POINTER) {
                Modifier
            } else {
                Modifier.pointerInput(true) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            state.undoneBlows.clear()
                            state.blows.add(
                                Blow(
                                    Path().apply {
                                        moveTo(offset.x / size.width, offset.y / size.width)
                                    },
                                    state.strokeWidth / size.width,
                                    state.strokeColor,
                                    state.drawingTool == DrawingTool.ERASER
                                )
                            )
                            state.skip = true
                        },
                    ) { change, dragAmount ->
                        // Skip first move event because we already added this point
                        if (state.skip) {
                            state.skip = false
                        } else {
                            state.blows.last().path.quadraticTo(
                                change.previousPosition.x / size.width,
                                change.previousPosition.y / size.width,
                                (change.previousPosition.x / size.width + change.position.x / size.width) / 2f,
                                (change.previousPosition.y / size.width + change.position.y / size.width) / 2f,
                            )
                            state.recompose()
                        }
                    }
                }
            }
        }

    ) {
        // Trigger recomposition
        state.lastUpdate

        withLayerTransform({ scale(size.width, size.width, Offset.Zero) }) {
            if (state.currentFrame == -1) {
                state.frames.lastOrNull()?.blows?.forEach { blow -> drawBlow(blow, 0.1f) }
            } else {
                state.frames.getOrNull(state.currentFrame - 1)?.blows?.forEach { blow ->
                    drawBlow(
                        blow,
                        0.1f
                    )
                }
            }
        }

        withLayerTransform({ scale(size.width, size.width, Offset.Zero) }) {
            state.blows.forEach(this::drawBlow)
        }
    }
}
