package tech.kekulta.phena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp

@Composable
fun AnimationCanvasScreen(
    state: AnimationCanvasState,
    onSaveAnimation: (Anim) -> Unit,
    onPlayAnimation: (Anim) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    var currentMenu by remember { mutableStateOf(Menu.NO_MENU) }
    var showDialog by remember { mutableStateOf(false) }
    var animName by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                animName = state.name ?: "Animation-${System.currentTimeMillis()}"
                showDialog = true
            }, enabled = state.frames.isNotEmpty()) { Text("Save Animation") }

            if (state.currentFrame != -1) {
                Button(onClick = {
                    state.abortEditing()
                }) { Text("Cancel") }
            }

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

            Button(
                onClick = {
                    state.clear()
                },
                enabled = state.frames.isNotEmpty() || state.blows.isNotEmpty()
            ) { Text("Delete All") }

            Button(
                onClick = { onOpenLibrary() },
            ) { Text("Lib") }
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
                        itemsIndexed(state.frames) { index, frame ->
                            Canvas(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .aspectRatio(state.aspectRatio)
                                    .background(frame.background)
                                    .border(
                                        selectedBorder(index == state.currentFrame),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        state.editFrame(index)
                                    }
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
