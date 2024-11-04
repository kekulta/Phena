package tech.kekulta.phena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import tech.kekulta.phena.ui.theme.Green
import tech.kekulta.phena.ui.theme.Grey
import tech.kekulta.phena.ui.theme.White

@Composable
fun AnimationCanvasScreen(
    state: AnimationCanvasState,
    onSaveAnimation: (Anim) -> Unit,
    onPlayAnimation: (Anim) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    var currentMenu by remember { mutableStateOf(Menu.NO_MENU) }
    var showDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var animName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false },
            title = { Text("Save Animation as...") },
            text = {
                TextField(
                    animName,
                    onValueChange = { c -> animName = c },
                    singleLine = true,
                    keyboardActions = KeyboardActions(onDone = {
                        onSaveAnimation(
                            Anim(
                                name = animName,
                                frames = state.frames.toList(),
                                aspectRatio = state.aspectRatio,
                                frameRate = state.frameRate
                            )
                        )
                        showDialog = false
                    })
                )
            },

            confirmButton = {
                TextButton(onClick = {
                    onSaveAnimation(
                        Anim(
                            name = animName,
                            frames = state.frames.toList(),
                            aspectRatio = state.aspectRatio,
                            frameRate = state.frameRate
                        )
                    )
                    showDialog = false
                }, enabled = animName.isNotBlank()) { Text("Ok") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween
        ) {
            Row {

                IconButton(onClick = {
                    state.undoneBlows.add(
                        state.blows.removeAt(
                            state.blows.lastIndex
                        )
                    )
                }, enabled = state.blows.isNotEmpty()) {
                    Icon(
                        painterResource(R.drawable.outline_right_arrow_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Undo Tool",
                        tint = if (state.blows.isNotEmpty()) White else Grey
                    )
                }
                IconButton(onClick = {
                    state.blows.add(
                        state.undoneBlows.removeAt(
                            state.undoneBlows.lastIndex
                        )
                    )
                }, enabled = state.undoneBlows.isNotEmpty()) {
                    Icon(
                        painterResource(R.drawable.outline_left_arrow_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Redo Tool",
                        tint = if (state.undoneBlows.isNotEmpty()) White else Grey
                    )
                }
            }

            Row {

                IconButton(onClick = {
                    state.saveFrame()
                }) {
                    Icon(
                        painterResource(R.drawable.outline_create_new_folder_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "New Frame",
                        tint = White
                    )
                }
                IconButton(
                    onClick = {
                        state.undoFrame()
                    },
                    enabled = state.frames.isNotEmpty()
                ) {
                    Icon(
                        painterResource(R.drawable.outline_delete_outline_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Delete Last Frame",
                        tint = if (state.frames.isNotEmpty()) White else Grey
                    )
                }

                if (state.currentFrame != -1) {
                    IconButton(onClick = {
                        state.abortEditing()
                    }) {
                        Icon(
                            painterResource(R.drawable.outline_cancel_24),
                            modifier = Modifier.size(32.dp),
                            contentDescription = "Cancel Editing",
                            tint = White
                        )
                    }
                }
            }
            Row {

                IconButton(onClick = {
                    onPlayAnimation(state.getAnim())
                }, enabled = state.frames.isNotEmpty()) {
                    Icon(
                        painterResource(R.drawable.baseline_play_arrow_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Open Player",
                        tint = if (state.frames.isNotEmpty()) White else Grey
                    )
                }


                Box {
                    IconButton(onClick = {
                        showMenu = true
                    }) {
                        Icon(
                            painterResource(R.drawable.baseline_menu_24),
                            modifier = Modifier.size(32.dp),
                            contentDescription = "Open Menu",
                        )
                    }

                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            { Text("Default") },
                            enabled = state.scale != 1f || state.offset != Offset.Zero,
                            onClick = {
                                state.scale = 1f
                                state.offset = Offset.Zero

                            })

                        DropdownMenuItem(
                            { Text("Delete All") },
                            enabled = state.frames.isNotEmpty() || state.blows.isNotEmpty(),
                            onClick = {
                                state.clear()
                            })

                        DropdownMenuItem(
                            { Text("Open Library") },
                            onClick = {
                                onOpenLibrary()
                            })

                        DropdownMenuItem(
                            { Text("Save Animation") },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.outline_save_24),
                                    modifier = Modifier.size(32.dp),
                                    contentDescription = "Save Animation",
                                )
                            },
                            enabled = state.frames.isNotEmpty(),
                            onClick = {
                                animName = state.name ?: "Animation-${System.currentTimeMillis()}"
                                showDialog = true
                            })
                    }
                }
            }
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
                    ColorPicker(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        listOf(Color.White, Color.Black, Color.Blue, Color.Red, Color.Cyan),
                        state.strokeColor
                    ) { color ->
                        state.strokeColor = color
                    }
                }

                Menu.BACKGROUND -> {
                    ColorPicker(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        listOf(Color.White, Color.Black, Color.Blue, Color.Red, Color.Cyan),
                        state.backgroundColor
                    ) { color ->
                        state.backgroundColor = color
                    }
                }

                Menu.FRAMERATE -> {
                    FrameRatePicker(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        frameRates = listOf(4f, 24f, 30f, 60f),
                        currentFrameRate = state.frameRate
                    ) { rate -> state.frameRate = rate }
                }

                Menu.RATIO -> {
                    RatioPicker(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        ratios = listOf(
                            Ratio(1f, 1f),
                            Ratio(4f, 3f),
                            Ratio(16f, 9f),
                            Ratio(9f, 16f),
                        ),
                        currentRatio = state.aspectRatio,
                    ) { ratio -> state.aspectRatio = ratio }
                }

                Menu.FRAMES -> {
                    LazyRow(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(100.dp)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(state.frames) { index, frame ->
                            Canvas(modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .aspectRatio(state.aspectRatio.calculate())
                                .background(frame.background)
                                .border(
                                    selectedBorder(index == state.currentFrame),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    state.editFrame(index)
                                }) {
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                IconButton(onClick = { state.drawingTool = DrawingTool.POINTER }) {
                    Icon(
                        painterResource(R.drawable.outline_back_hand_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Pointer Tool",
                        tint = if (state.drawingTool == DrawingTool.POINTER) Green else White

                    )
                }
                IconButton(onClick = { state.drawingTool = DrawingTool.ERASER }) {
                    Icon(
                        painterResource(R.drawable.outline_eraser_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Eraser Tool",
                        tint = if (state.drawingTool == DrawingTool.ERASER) Green else White

                    )
                }
                IconButton(onClick = { state.drawingTool = DrawingTool.PENCIL }) {
                    Icon(
                        painterResource(R.drawable.outline_pencil_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Pencil Tool",
                        tint = if (state.drawingTool == DrawingTool.PENCIL) Green else White

                    )
                }
            }

            Row {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(state.strokeColor)
                        .border(selectedBorder(currentMenu == Menu.COLOR), CircleShape)
                        .clickable {
                            currentMenu = if (currentMenu == Menu.COLOR) {
                                Menu.NO_MENU
                            } else {
                                Menu.COLOR
                            }
                        },
                ) {}

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(state.backgroundColor)
                        .border(
                            selectedBorder(currentMenu == Menu.BACKGROUND),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            currentMenu = if (currentMenu == Menu.BACKGROUND) {
                                Menu.NO_MENU
                            } else {
                                Menu.BACKGROUND
                            }
                        },
                ) {}
            }

            Row {
                IconButton(onClick = {
                    currentMenu = if (currentMenu == Menu.RATIO) {
                        Menu.NO_MENU
                    } else {
                        Menu.RATIO
                    }
                }) {
                    Icon(
                        painterResource(R.drawable.outline_aspect_ratio_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Set Aspect ratio",
                        tint = if (currentMenu == Menu.RATIO) Green else White
                    )
                }

                IconButton(onClick = {
                    currentMenu = if (currentMenu == Menu.FRAMERATE) {
                        Menu.NO_MENU
                    } else {
                        Menu.FRAMERATE
                    }
                }) {
                    Icon(
                        painterResource(R.drawable.baseline_30fps_select_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Set Frame Rate",
                        tint = if (currentMenu == Menu.FRAMERATE) Green else White
                    )
                }

                IconButton(onClick = {
                    currentMenu = if (currentMenu == Menu.FRAMES) {
                        Menu.NO_MENU
                    } else {
                        Menu.FRAMES
                    }
                }) {
                    Icon(
                        painterResource(R.drawable.baseline_video_library_24),
                        modifier = Modifier.size(32.dp),
                        contentDescription = "Show Frames",
                        tint = if (currentMenu == Menu.FRAMES) Green else White
                    )
                }
            }
        }
    }
}