package tech.kekulta.phena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import tech.kekulta.phena.ScreenState.Canvas
import tech.kekulta.phena.ScreenState.Player
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
            withLayerTransform({ scale(size.width, size.width, Offset.Zero) }) {
                drawFrame(frame)
            }
        }
    } ?: run {
        Text("No Animation Selected")
    }
}

enum class Menu {
    NO_MENU, COLOR, BACKGROUND, FRAMERATE, RATIO, FRAMES
}

@Composable
fun AnimationCanvasScreen(
    state: AnimationCanvasState,
    onSaveAnimation: (Anim) -> Unit,
    onPlayAnimation: (Anim) -> Unit,
) {
    var currentMenu by remember { mutableStateOf(Menu.NO_MENU) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .horizontalScroll(rememberScrollState())
        ) {
            Button(enabled = state.blows.isNotEmpty(), onClick = {
                state.undoneBlows.add(
                    state.blows.removeAt(
                        state.blows.lastIndex
                    )
                )
            }) { Text("Undo") }
            Button(enabled = state.undoneBlows.isNotEmpty(),
                onClick = {
                    state.blows.add(
                        state.undoneBlows.removeAt(
                            state.undoneBlows.lastIndex
                        )
                    )
                }) { Text("Redo") }
            Button(onClick = {
                state.saveFrame()
            }) { Text("Save") }

            Button(onClick = {
                state.undoFrame()
            }, enabled = state.frames.isNotEmpty()) { Text("Last Frame") }

            Button(onClick = {
                state.scale = 1f
                state.offset = Offset.Zero
            }, enabled = state.scale != 1f || state.offset != Offset.Zero) { Text("Default") }

            Button(onClick = {
                onPlayAnimation(state.getAnim())
            }, enabled = state.frames.isNotEmpty()) { Text("Play") }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .transformable(state)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer(
                        scaleX = state.scale,
                        scaleY = state.scale,
                        translationX = state.offset.x,
                        translationY = state.offset.y
                    )
            ) {
                AnimationCanvas(state = state)
            }

            when (currentMenu) {
                Menu.NO_MENU -> Unit

                Menu.COLOR -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(80.dp))
                            .background(Color.DarkGray)
                            .padding(8.dp)
                    ) {
                        Button(onClick = {
                            state.strokeColor = Color.Black
                        }) { Text("Black") }
                        Button(onClick = {
                            state.strokeColor = Color.Blue
                        }) { Text("Blue") }
                        Button(onClick = {
                            state.strokeColor = Color.Red
                        }) { Text("Red") }
                        Button(onClick = {
                            state.strokeColor = Color.Cyan
                        }) { Text("Cyan") }
                    }
                }

                Menu.BACKGROUND -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(80.dp))
                            .background(Color.DarkGray)
                            .padding(8.dp)
                    ) {
                        Button(onClick = {
                            state.backgroundColor = Color.Black
                        }) { Text("Black") }
                        Button(onClick = {
                            state.backgroundColor = Color.Blue
                        }) { Text("Blue") }
                        Button(onClick = {
                            state.backgroundColor = Color.Red
                        }) { Text("Red") }
                        Button(onClick = {
                            state.backgroundColor = Color.Cyan
                        }) { Text("Cyan") }
                    }
                }

                Menu.FRAMERATE -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(80.dp))
                            .background(Color.DarkGray)
                            .padding(8.dp)
                    ) {
                        Button(onClick = {
                            state.frameRate = 4f
                        }) { Text("4f") }
                        Button(onClick = {
                            state.frameRate = 24f
                        }) { Text("24f") }
                        Button(onClick = {
                            state.frameRate = 30f
                        }) { Text("30f") }
                        Button(onClick = {
                            state.frameRate = 60f
                        }) { Text("60f") }
                    }
                }

                Menu.RATIO -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(80.dp))
                            .background(Color.DarkGray)
                            .padding(8.dp)
                    ) {
                        Button(onClick = {
                            state.aspectRatio = 1f
                        }) { Text("1/1") }
                        Button(onClick = {
                            state.aspectRatio = 4 / 3f
                        }) { Text("4/3") }
                        Button(onClick = {
                            state.aspectRatio = 16 / 9f
                        }) { Text("16/9") }
                        Button(onClick = {
                            state.aspectRatio = 9 / 16f
                        }) { Text("9/16") }
                    }
                }

                Menu.FRAMES -> {
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(100.dp)
                            .fillMaxWidth()
                    ) {
                        items(state.frames) { frame ->
                            Canvas(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .aspectRatio(state.aspectRatio)
                                    .background(frame.background)
                            ) {
                                withLayerTransform({
                                    scale(
                                        size.width,
                                        size.width,
                                        Offset.Zero,
                                    )
                                }) {
                                    frame.blows.forEach(this::drawBlow)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Button(
                onClick = {
                    state.drawingTool = DrawingTool.POINTER
                },
                border = selectedBorder(state.drawingTool == DrawingTool.POINTER)
            ) { Text("M") }

            Button(
                onClick = {
                    state.drawingTool = DrawingTool.ERASER
                },
                border = selectedBorder(state.drawingTool == DrawingTool.ERASER)
            ) { Text("E") }

            Button(
                onClick = {
                    state.drawingTool = DrawingTool.PENCIL
                },
                border = selectedBorder(state.drawingTool == DrawingTool.PENCIL)
            ) { Text("P") }

            Button(
                onClick = {
                    currentMenu = if (currentMenu == Menu.COLOR) {
                        Menu.NO_MENU
                    } else {
                        Menu.COLOR
                    }
                },
                border = selectedBorder(currentMenu == Menu.COLOR)
            ) { Text("Color") }

            Button(
                onClick = {
                    currentMenu = if (currentMenu == Menu.BACKGROUND) {
                        Menu.NO_MENU
                    } else {
                        Menu.BACKGROUND
                    }
                },
                border = selectedBorder(currentMenu == Menu.BACKGROUND)
            ) { Text("Back") }

            Button(
                onClick = {
                    currentMenu = if (currentMenu == Menu.RATIO) {
                        Menu.NO_MENU
                    } else {
                        Menu.RATIO
                    }
                },
                border = selectedBorder(currentMenu == Menu.RATIO)
            ) { Text("Ratio") }

            Button(
                onClick = {
                    currentMenu = if (currentMenu == Menu.FRAMERATE) {
                        Menu.NO_MENU
                    } else {
                        Menu.FRAMERATE
                    }
                },
                border = selectedBorder(currentMenu == Menu.FRAMERATE)
            ) { Text("Framerate") }

            Button(
                onClick = {
                    currentMenu = if (currentMenu == Menu.FRAMES) {
                        Menu.NO_MENU
                    } else {
                        Menu.FRAMES
                    }
                },
                border = selectedBorder(currentMenu == Menu.FRAMES)
            ) { Text("Frames") }
        }
    }
}


fun selectedBorder(isSelected: Boolean): BorderStroke {
    return if (isSelected) {
        BorderStroke(4.dp, Color.Green)
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }
}

enum class DrawingTool {
    PENCIL,
    ERASER,
    POINTER
}

@Stable
class AnimationCanvasState {
    val frames = mutableStateListOf<Frame>()
    val blows = mutableStateListOf<Blow>()
    val undoneBlows = mutableStateListOf<Blow>()
    var drawingTool by mutableStateOf<DrawingTool>(DrawingTool.PENCIL)

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

@Composable
fun AnimationCanvas(modifier: Modifier = Modifier, state: AnimationCanvasState) {
    Canvas(modifier = modifier
        .padding(16.dp)
        .aspectRatio(state.aspectRatio)
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
            state.frames.lastOrNull()?.blows?.forEach { blow -> drawBlow(blow, 0.1f) }
        }

        withLayerTransform({ scale(size.width, size.width, Offset.Zero) }) {
            state.blows.forEach(this::drawBlow)
        }
    }
}

@Composable
fun AnimationPlayerScreen(
    state: AnimationPlayerState = rememberAnimationPlayerState(),
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    Column {
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

inline fun DrawScope.withLayerTransform(
    transformBlock: DrawTransform.() -> Unit, drawBlock: DrawScope.() -> Unit
) = with(drawContext) {
    val previousSize = size
    canvas.nativeCanvas.saveLayer(null, null)
    try {
        transformBlock(transform)
        drawBlock()
    } finally {
        canvas.nativeCanvas.restore()
        size = previousSize
    }
}

enum class ScreenState {
    Player, Canvas
}

inline fun <reified T : Enum<T>> T.next(): T {
    return enumEntries<T>()[(this.ordinal + 1) % enumEntries<T>().size]
}

fun AnimationCanvasState.undoFrame() {
    undoneBlows.clear()
    blows.clear()
    blows.addAll(frames.last().blows)
    backgroundColor = frames.last().background
    frames.removeAt(frames.lastIndex)
}

fun AnimationCanvasState.saveFrame() {
    undoneBlows.clear()
    frames.add(Frame(blows.toList(), backgroundColor))
    blows.clear()
}

fun AnimationCanvasState.getAnim(): Anim {
    return Anim(frames.toList(), aspectRatio, frameRate)
}

fun AnimationPlayerState.set(anim: Anim) {
    animation = anim
    frameNum = 0
    isPlaying = false
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
                    var screenState by remember { mutableStateOf(Canvas) }

                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (screenState) {
                            Player -> AnimationPlayerScreen(animationPlayerState, {
                                screenState = Canvas
                            })

                            Canvas -> AnimationCanvasScreen(animationCanvasState, {}, { anim ->
                                animationPlayerState.set(anim)
                                screenState = Player
                            })
                        }
                    }
                }
            }
        }
    }
}