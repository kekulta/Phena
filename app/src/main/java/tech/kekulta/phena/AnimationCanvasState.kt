package tech.kekulta.phena

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Stable
class AnimationCanvasState {
    var name by mutableStateOf<String?>(null)
    val frames = mutableStateListOf<Frame>()
    val blows = mutableStateListOf<Blow>()
    val undoneBlows = mutableStateListOf<Blow>()
    var drawingTool by mutableStateOf<DrawingTool>(DrawingTool.PENCIL)

    // Number of the current frame in editing. -1 if this is new frame.
    var currentFrame by mutableIntStateOf(-1)

    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var strokeColor by mutableStateOf(Color.Black)
    var strokeWidth by mutableStateOf(10.dp)
    var backgroundColor by mutableStateOf(Color.White)
    var aspectRatio by mutableStateOf(Ratio(1f, 1f))
    var frameRate by mutableFloatStateOf(4f)

    // Ugly workaround
    var skip by mutableStateOf(false)

    // Recomposition trigger
    var lastUpdate by mutableLongStateOf(0L)
}

@Composable
fun rememberAnimationCanvasState() = remember { AnimationCanvasState() }

fun Modifier.transformable(state: AnimationCanvasState): Modifier {
    return this.composed {
        if (state.drawingTool == DrawingTool.POINTER) {
            Modifier.pointerInput(true) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    state.scale *= zoom
                    state.scale = state.scale.coerceIn(1f, 20f)
                    state.offset += pan
                }
            }
        } else {
            Modifier
        }
    }
}

fun AnimationCanvasState.set(anim: Anim) {
    blows.clear()
    undoneBlows.clear()
    frames.clear()
    currentFrame = -1
    scale = 1f
    offset = Offset.Zero

    aspectRatio = anim.aspectRatio
    frameRate = anim.frameRate
    frames.addAll(anim.frames)
    name = anim.name
}

fun AnimationCanvasState.clear() {
    blows.clear()
    undoneBlows.clear()
    frames.clear()
    currentFrame = -1
}

fun AnimationCanvasState.abortEditing() {
    currentFrame = -1
    undoneBlows.clear()
    blows.clear()
}

fun AnimationCanvasState.undoFrame() {
    undoneBlows.clear()
    blows.clear()
    if (frames.isEmpty()) return
    if (currentFrame == -1) {
        blows.addAll(frames.last().blows)
        frames.removeAt(frames.lastIndex)
    } else {
        frames.removeAt(currentFrame)
        currentFrame = -1
    }
    frames.lastOrNull()?.background?.let { b -> backgroundColor = b }
}

fun AnimationCanvasState.editFrame(num: Int) {
    currentFrame = num
    undoneBlows.clear()
    blows.clear()
    blows.addAll(frames[num].blows)
    backgroundColor = frames[num].background
}

fun AnimationCanvasState.saveFrame() {
    undoneBlows.clear()
    if (currentFrame == -1) {
        frames.add(Frame(blows.toList(), backgroundColor))
    } else {
        frames.removeAt(currentFrame)
        frames.add(currentFrame, Frame(blows.toList(), backgroundColor))
        backgroundColor = frames.last().background
        currentFrame = -1
    }

    blows.clear()
}

fun AnimationCanvasState.getAnim(): Anim {
    return Anim(name ?: "Scratch", frames.toList(), aspectRatio, frameRate)
}

fun AnimationCanvasState.recompose() {
    lastUpdate = System.currentTimeMillis()
}
