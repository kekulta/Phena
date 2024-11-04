package tech.kekulta.phena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ColorPicker(
    modifier: Modifier,
    colors: List<Color>,
    currentColor: Color,
    onColorPicked: (Color) -> Unit
) {
    LazyRow(
        modifier = modifier
            .clip(RoundedCornerShape(80.dp))
            .background(Color.DarkGray)
            .padding(8.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(selectedBorder(color == currentColor), CircleShape)
                    .clickable {
                        onColorPicked(color)
                    },
            ) {}
        }
    }
}
