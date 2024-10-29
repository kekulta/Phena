package tech.kekulta.phena

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

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
