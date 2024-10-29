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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
data class Anim(val frames: List<Frame>, val aspectRatio: Float = 1f, val frameRate: Float = 4f)

val EmptyAnim = Anim(emptyList())

fun DrawScope.drawFrame(frame: Frame) {
    frame.blows.forEach(this::drawBlow)
}

fun DrawScope.drawBlow(blow: Blow, alfa: Float = 1f) {
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
            color = blow.color.copy(alpha = alfa),
            style = Stroke(width = blow.width.toPx(), cap = StrokeCap.Square),
        )
    }
}

@Stable
class AnimationPlayerState {
    var frameNum by mutableIntStateOf(0)
    var isPlaying by mutableStateOf(false)
    var animation by mutableStateOf<Anim?>(null)
}

@Composable
fun rememberAnimationPlayerState() = remember { AnimationPlayerState() }

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
                .aspectRatio(anim.aspectRatio)
                .clip(shape = RoundedCornerShape(20.dp))
                .background(frame.background)
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            canvas.saveLayer(null, null)
            drawFrame(frame)
            canvas.restore()
        }
    }
}

@Stable
class AnimationCanvasState {
    val frames = mutableStateListOf<Frame>()
    val blows = mutableStateListOf<Blow>()
    val undoneBlows = mutableStateListOf<Blow>()
    var isErase by mutableStateOf(false)
    var isMoving by mutableStateOf(false)

    var size by mutableStateOf(Size(1f, 1f))
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var strokeColor by mutableStateOf(Color.Cyan)
    var strokeWidth by mutableStateOf(10.dp)
    var backgroundColor by mutableStateOf(Color.White)
    var aspectRatio by mutableFloatStateOf(1f)
    var frameRate by mutableFloatStateOf(4f)

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
        .aspectRatio(state.aspectRatio)
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
                                        moveTo(offset.x, offset.y)
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
        state.size = size

        Log.d("TAG", state.lastUpdate.toString())
        val canvas = drawContext.canvas.nativeCanvas

        canvas.saveLayer(null, null)
        state.frames.lastOrNull()?.blows?.forEach { blow -> drawBlow(blow, 0.1f) }
        canvas.restore()

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

fun AnimationCanvasState.undoFrame() {
    undoneBlows.clear()
    blows.clear()
    blows.addAll(frames.last().blows)
    backgroundColor =
        frames.last().background
    frames.removeAt(frames.lastIndex)
}

fun AnimationCanvasState.saveFrame() {
    frames.add(Frame(blows.toList(), backgroundColor))
    blows.clear()
}

fun AnimationCanvasState.getAnim(): Anim {
    return Anim(frames.toList(), aspectRatio, frameRate)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
                                    animationPlayerState.animation = animationCanvasState.getAnim()
                                    animationPlayerState.frameNum = 0
                                    animationPlayerState.isPlaying = false
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

                            Button(
                                onClick = {

                                    animationCanvasState.undoFrame()
                                },
                                enabled = animationCanvasState.frames.isNotEmpty()
                            ) { Text("Last Frame") }
                        }
                        Row {
                            Button(
                                onClick = {
                                    animationPlayerState.isPlaying = !animationPlayerState.isPlaying
                                },
                            ) { Text("Play/Pause") }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .transformable(animationCanvasState)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                            ) {
                                LazyRow(
                                    modifier = Modifier
                                        .height(100.dp)
                                        .fillMaxWidth()
                                ) {
                                    items(animationCanvasState.frames) { frame ->
                                        Canvas(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .aspectRatio(animationCanvasState.aspectRatio)
                                                .background(animationCanvasState.backgroundColor)
                                        ) {
                                            withTransform({
                                                scale(
                                                    size.width / animationCanvasState.size.width,
                                                    size.width / animationCanvasState.size.width,
                                                    Offset.Zero,
                                                )
                                            }) {
                                                frame.blows.forEach(this::drawBlow)
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(80.dp))
                                        .background(Color.DarkGray)
                                        .padding(8.dp)
                                ) {
                                    Button(onClick = {
                                        animationCanvasState.backgroundColor = Color.Black
                                    }) { Text("Black") }
                                    Button(onClick = {
                                        animationCanvasState.backgroundColor = Color.Blue
                                    }) { Text("Blue") }
                                    Button(onClick = {
                                        animationCanvasState.backgroundColor = Color.Red
                                    }) { Text("Red") }
                                    Button(onClick = {
                                        animationCanvasState.backgroundColor = Color.Cyan
                                    }) { Text("Cyan") }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(80.dp))
                                        .background(Color.DarkGray)
                                        .padding(8.dp)
                                ) {
                                    Button(onClick = {
                                        animationCanvasState.strokeColor = Color.Black
                                    }) { Text("Black") }
                                    Button(onClick = {
                                        animationCanvasState.strokeColor = Color.Blue
                                    }) { Text("Blue") }
                                    Button(onClick = {
                                        animationCanvasState.strokeColor = Color.Red
                                    }) { Text("Red") }
                                    Button(onClick = {
                                        animationCanvasState.strokeColor = Color.Cyan
                                    }) { Text("Cyan") }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(80.dp))
                                        .background(Color.DarkGray)
                                        .padding(8.dp)
                                ) {
                                    Button(onClick = {
                                        animationCanvasState.aspectRatio = 1f
                                    }) { Text("1/1") }
                                    Button(onClick = {
                                        animationCanvasState.aspectRatio = 4 / 3f
                                    }) { Text("4/3") }
                                    Button(onClick = {
                                        animationCanvasState.aspectRatio = 16 / 9f
                                    }) { Text("16/9") }
                                    Button(onClick = {
                                        animationCanvasState.aspectRatio = 9 / 16f
                                    }) { Text("9/16") }
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(80.dp))
                                        .background(Color.DarkGray)
                                        .padding(8.dp)
                                ) {
                                    Button(onClick = {
                                        animationCanvasState.frameRate = 4f
                                    }) { Text("4f") }
                                    Button(onClick = {
                                        animationCanvasState.frameRate = 24f
                                    }) { Text("24f") }
                                    Button(onClick = {
                                        animationCanvasState.frameRate = 30f
                                    }) { Text("30f") }
                                    Button(onClick = {
                                        animationCanvasState.frameRate = 60f
                                    }) { Text("60f") }
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
                                animationCanvasState.saveFrame()
                                animationCanvasState.recompose()
                            }) { Text("Save") }
                        }
                    }
                }
            }
        }
    }
}