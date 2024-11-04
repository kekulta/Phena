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

data class Ratio(val width: Float, val height: Float)

@Composable
fun RatioPicker(
    modifier: Modifier = Modifier,
    ratios: List<Ratio>,
    currentRatio: Ratio,
    onPicked: (Ratio) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }

    if (showPicker) {
        AlertDialog(onDismissRequest = { showPicker = false },
            title = { Text("Set Frame Rate to..") },
            text = {
                Column {
                    TextField(
                        width,
                        label = { Text("Width") },
                        onValueChange = { c -> width = c },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        keyboardActions = KeyboardActions(onDone = {
                            width.toFloatOrNull()?.let { w ->
                                height.toFloatOrNull()?.let { h ->
                                    onPicked(Ratio(w, h))
                                }
                            }
                            showPicker = false
                        })
                    )
                    TextField(
                        height,
                        label = { Text("Height") },
                        onValueChange = { c -> height = c },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        keyboardActions = KeyboardActions(onDone = {
                            height.toFloatOrNull()?.let { w ->
                                height.toFloatOrNull()?.let { h ->
                                    onPicked(Ratio(w, h))
                                }
                            }
                            showPicker = false
                        })
                    )
                }
            },

            confirmButton = {
                TextButton(onClick = {
                    width.toFloatOrNull()?.let { w ->
                        height.toFloatOrNull()?.let { h ->
                            onPicked(Ratio(w, h))
                        }
                    }
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
                width = currentRatio.width.toString()
                height = currentRatio.height.toString()
                showPicker = true
            }, border = selectedBorder(currentRatio.calculate() !in ratios.map(Ratio::calculate))) { Text(currentRatio.format()) }

            SingleChoiceSegmentedButtonRow {
                ratios.forEachIndexed { index, ratio ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ratios.size
                        ),
                        onClick = { onPicked(ratio) },
                        selected = currentRatio.calculate() == ratio.calculate()
                    ) {
                        Text("${ratio.width.toInt()}/${ratio.height.toInt()}")
                    }
                }
            }
        }
    }
}

private fun Ratio.format(): String = "$width/$height"

fun Ratio.calculate() = width / height