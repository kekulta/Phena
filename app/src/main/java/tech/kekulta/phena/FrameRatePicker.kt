package tech.kekulta.phena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun FrameRatePicker(
    modifier: Modifier = Modifier,
    frameRates: List<Float>,
    currentFrameRate: Float,
    onPicked: (Float) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var frameRate by remember { mutableStateOf("") }

    if (showPicker) {
        AlertDialog(onDismissRequest = { showPicker = false },
            title = { Text("Set Frame Rate to..") },
            text = {
                TextField(
                    frameRate,
                    onValueChange = { c -> frameRate = c },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    keyboardActions = KeyboardActions(onDone = {
                        frameRate.toFloatOrNull()?.let { f -> onPicked(f) }
                        showPicker = false
                    })
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    frameRate.toFloatOrNull()?.let { f -> onPicked(f) }
                    showPicker = false
                }) { Text("Ok") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } })
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(80.dp))
                .background(Color.DarkGray)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = {
                frameRate = currentFrameRate.toString()
                showPicker = true
            }, border = selectedBorder(currentFrameRate !in frameRates)) { Text(currentFrameRate.toString()) }

            SingleChoiceSegmentedButtonRow {
                frameRates.forEachIndexed { index, rate ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = frameRates.size
                        ),
                        onClick = { onPicked(rate) },
                        selected = currentFrameRate == rate
                    ) {
                        Text(rate.toInt().toString())
                    }
                }
            }
        }
    }
}