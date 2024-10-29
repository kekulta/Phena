package tech.kekulta.phena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import tech.kekulta.phena.ScreenState.Canvas
import tech.kekulta.phena.ScreenState.Library
import tech.kekulta.phena.ScreenState.Player
import tech.kekulta.phena.ui.theme.PhenaTheme

@Stable
class AnimationLibraryState {
    val animations = mutableStateMapOf<String, Anim>()
}

@Composable
fun rememberAnimationLibraryState(): AnimationLibraryState {
    return remember { AnimationLibraryState() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimationLibraryScreen(
    state: AnimationLibraryState,
    onPlayAnim: (Anim) -> Unit,
    onEditAnim: (Anim) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    LazyColumn {
        itemsIndexed(state.animations.values.toList()) { index, anim ->
            Text(modifier = Modifier.combinedClickable(onLongClick = {
                onEditAnim(anim)
            }) {
                onPlayAnim(anim)
            }, text = anim.name)
        }
    }
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
                    val animationLibraryState = rememberAnimationLibraryState()
                    var screenState by remember { mutableStateOf(Canvas) }
                    var prevScreen by remember { mutableStateOf(Canvas) }

                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (screenState) {
                            Player -> AnimationPlayerScreen(animationPlayerState, {
                                screenState = prevScreen
                            })

                            Canvas -> AnimationCanvasScreen(animationCanvasState, { anim ->
                                animationLibraryState.animations[anim.name] = anim
                            }, { anim ->
                                animationPlayerState.set(anim)
                                prevScreen = Canvas
                                screenState = Player
                            }, {
                                screenState = Library
                            })

                            Library -> AnimationLibraryScreen(animationLibraryState, { anim ->
                                animationPlayerState.set(anim)
                                prevScreen = Library
                                screenState = Player
                            }, { anim ->
                                animationCanvasState.set(anim)
                                screenState = Canvas
                            }, {
                                screenState = Canvas
                            })
                        }
                    }
                }
            }
        }
    }
}