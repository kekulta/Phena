package tech.kekulta.phena

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import tech.kekulta.phena.ui.theme.PhenaTheme
import kotlin.enums.enumEntries

data class Blow(val path: Path, val width: Dp, val color: Color, val isErase: Boolean = false)
data class Frame(val blows: List<Blow>, val background: Color = Color.White)

fun DrawScope.drawFrame(frame: Frame) {
    frame.blows.forEach(this::drawBlow)
}

fun DrawScope.drawBlow(blow: Blow) {
    if (blow.isErase) {
        drawPath(
            blow.path,
            color = Color.Transparent,
            style = Stroke(width = blow.width.toPx(), cap = StrokeCap.Square),
            blendMode = BlendMode.Clear
        )
    } else {
        drawPath(
            blow.path,
            color = blow.color,
            style = Stroke(width = blow.width.toPx(), cap = StrokeCap.Square),
        )
    }
}

@Stable
class AnimationPlayerState(
    frameState: Int = 0,
    frameTimeState: Long = 0L,
    val frames: SnapshotStateList<Frame> = mutableStateListOf()
) {
    constructor(
        frameState: Int = 0, frameTimeState: Long = 0L, frames: List<Frame> = emptyList()
    ) : this(frameState, frameTimeState, mutableStateListOf(*frames.toTypedArray()))

    var frameState by mutableIntStateOf(frameState)
    var frameTimeState by mutableLongStateOf(frameTimeState)
}

@Composable
fun rememberAnimationPlayerState(
    frameState: Int = 0, frameTimeState: Long = 300L, frames: List<Frame> = emptyList()
) = remember { AnimationPlayerState(frameState, frameTimeState, frames) }

@Composable
fun AnimationPlayer(modifier: Modifier = Modifier, state: AnimationPlayerState) {
    LaunchedEffect(true) {
        state.frameState = 0
        var start = 0L

        try {
            while (isActive) {
                // convert to millis
                val nextFrame = awaitFrame() / 1_000_000
                if (start == 0L) {
                    start = nextFrame
                }
                state.frameState =
                    (((nextFrame - start) / state.frameTimeState) % state.frames.size).toInt()
            }
        } catch (e: CancellationException) {
            state.frameState = 0
            throw e
        }

        state.frameState = 0
    }

    val frame = state.frames[state.frameState]

    Canvas(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .clip(shape = RoundedCornerShape(20.dp))
            .background(frame.background)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        canvas.saveLayer(null, null)
        drawFrame(frame)
        canvas.restore()
    }
}

@Stable
class AnimationCanvasState {
    val frames = mutableStateListOf<Frame>()
    val blows = mutableStateListOf<Blow>()
    val undoneBlows = mutableStateListOf<Blow>()
    var isErase by mutableStateOf(false)
    var isMoving by mutableStateOf(false)

    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var strokeColor by mutableStateOf(Color.Black)
    var strokeWidth by mutableStateOf(10.dp)
    var backgroundColor by mutableStateOf(Color.White)

    var skip by mutableStateOf(false)
    var lastUpdate by mutableLongStateOf(0L)
}

@Composable
fun rememberAnimationCanvasState() = remember { AnimationCanvasState() }

fun AnimationCanvasState.recompose() {
    lastUpdate = System.currentTimeMillis()
}

fun Modifier.transformable(state: AnimationCanvasState): Modifier {
    return this.composed {
        if (state.isMoving) {
            Modifier.pointerInput(true) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    state.scale *= zoom
                    state.scale =
                        state.scale.coerceIn(1f, 20f)
                    state.offset += (pan * state.scale)
                }
            }
        } else {
            Modifier
        }
    }
}

@Composable
fun AnimationCanvas(modifier: Modifier = Modifier, state: AnimationCanvasState) {
    Canvas(modifier = modifier
        .padding(16.dp)
        .fillMaxSize()
        .clip(shape = RoundedCornerShape(20.dp))
        .background(state.backgroundColor)
        .composed {
            if (state.isMoving) {
                Modifier
            } else {
                Modifier.pointerInput(true) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            state.undoneBlows.clear()
                            state.blows.add(
                                Blow(
                                    Path().apply {
                                        moveTo(
                                            offset.x, offset.y
                                        )
                                    }, state.strokeWidth, state.strokeColor, state.isErase
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
                                change.previousPosition.x,
                                change.previousPosition.y,
                                (change.previousPosition.x + change.position.x) / 2f,
                                (change.previousPosition.y + change.position.y) / 2f,
                            )
                            state.recompose()
                        }
                    }
                }
            }
        }

    ) {
        Log.d("TAG", state.lastUpdate.toString())
        val canvas = drawContext.canvas.nativeCanvas
        canvas.saveLayer(null, null)
        state.blows.forEach(this::drawBlow)
        canvas.restore()
    }
}

enum class ScreenState {
    Player,
    Canvas
}

inline fun <reified T : Enum<T>> T.next(): T {
    return enumEntries<T>()[(this.ordinal + 1) % enumEntries<T>().size]
}

fun AnimationCanvasState.nextFrame() {
    frames.add(Frame(blows.toList(), backgroundColor))
    blows.clear()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            rememberTextFieldState()
            PhenaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val animationPlayerState = rememberAnimationPlayerState()
                    val animationCanvasState = rememberAnimationCanvasState()

                    var screenState by remember { mutableStateOf(ScreenState.Canvas) }

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        Text("Frames: ${animationCanvasState.frames.size}, blows: ${animationCanvasState.blows.size}")
                        Row {
                            Button(
                                onClick = {
                                    animationPlayerState.frames.addAll(animationCanvasState.frames)
                                    screenState = screenState.next()
                                },
                                enabled = animationCanvasState.frames.size > 0
                            ) { Text(screenState.name) }

                            Button(onClick = {
                                animationCanvasState.isMoving = !animationCanvasState.isMoving
                            }) { Text(if (animationCanvasState.isMoving) "Move" else "Draw") }

                            Button(onClick = {
                                animationCanvasState.scale = 1f
                                animationCanvasState.offset = Offset.Zero
                            }) { Text("Default") }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .transformable(animationCanvasState)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clipToBounds()
                                    .graphicsLayer(
                                        scaleX = animationCanvasState.scale,
                                        scaleY = animationCanvasState.scale,
                                        translationX = animationCanvasState.offset.x,
                                        translationY = animationCanvasState.offset.y
                                    )
                            ) {
                                when (screenState) {
                                    ScreenState.Canvas -> AnimationCanvas(state = animationCanvasState)
                                    ScreenState.Player -> AnimationPlayer(state = animationPlayerState)
                                }
                            }
                        }

                        Row(modifier = Modifier.wrapContentSize()) {
                            Button(
                                enabled = animationCanvasState.blows.isNotEmpty(),
                                onClick = {
                                    animationCanvasState.undoneBlows.add(
                                        animationCanvasState.blows.removeAt(
                                            animationCanvasState.blows.lastIndex
                                        )
                                    )
                                }) { Text("Undo") }
                            Button(
                                enabled = animationCanvasState.undoneBlows.isNotEmpty(),
                                onClick = {
                                    animationCanvasState.blows.add(
                                        animationCanvasState.undoneBlows.removeAt(
                                            animationCanvasState.undoneBlows.lastIndex
                                        )
                                    )
                                }) { Text("Redo") }
                            Button(onClick = {
                                animationCanvasState.isErase = !animationCanvasState.isErase
                            }) { Text(if (animationCanvasState.isErase) "D" else "E") }
                            Button(onClick = {
                                animationCanvasState.undoneBlows.clear()
                                animationCanvasState.nextFrame()
                                animationCanvasState.recompose()
                            }) { Text("Save") }
                        }
                    }
                }
            }
        }
    }
}