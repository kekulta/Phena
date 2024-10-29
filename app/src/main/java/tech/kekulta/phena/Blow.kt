package tech.kekulta.phena

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

data class Blow(val path: Path, val width: Dp, val color: Color, val isErase: Boolean = false)