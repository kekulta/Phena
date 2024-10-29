package tech.kekulta.phena

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun selectedBorder(isSelected: Boolean): BorderStroke {
    return if (isSelected) {
        BorderStroke(4.dp, Color.Green)
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }
}
