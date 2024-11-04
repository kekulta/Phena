package tech.kekulta.phena

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.kekulta.phena.ui.theme.Green

fun selectedBorder(isSelected: Boolean): BorderStroke {
    return if (isSelected) {
        BorderStroke(4.dp, Green)
    } else {
        BorderStroke(0.dp, Color.Transparent)
    }
}
