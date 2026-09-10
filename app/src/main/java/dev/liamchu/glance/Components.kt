package dev.liamchu.glance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.net.URI
import java.net.URISyntaxException

/** A dashed rule drawn as blocks, so it reads as pixels rather than a hairline. */
@Composable
fun PixelRule(colour: Color = GlanceDimGrey) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        repeat(41) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (index % 2 == 0) colour else Color.Transparent)
            )
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = GlanceGrey,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            singleLine = true,
            shape = RectangleShape,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            colors = fieldColours(),
        )
    }
}

/** One heading over a row of unit boxes that add up. */
@Composable
fun Duration(label: String, cells: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = GlanceGrey,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = cells,
        )
    }
}

@Composable
fun NumberCell(
    unit: String,
    value: String,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            singleLine = true,
            shape = RectangleShape,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColours(),
        )
        Text(unit, style = MaterialTheme.typography.labelSmall, color = GlanceGrey)
    }
}

@Composable
fun fieldColours() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlanceWhite,
    unfocusedTextColor = GlanceWhite,
    focusedBorderColor = GlanceWhite,
    unfocusedBorderColor = GlanceDimGrey,
    focusedContainerColor = GlanceNearBlack,
    unfocusedContainerColor = GlanceNearBlack,
    cursorColor = GlanceWhite,
)

/** "15 MIN", "4 H", "1 D 2 H" — the parts that are not zero, in order. */
fun Watcher.intervalLabel(): String {
    val parts = mutableListOf<String>()
    if (checkDays > 0) parts.add(checkDays.toString() + " D")
    if (checkHours > 0) parts.add(checkHours.toString() + " H")
    if (checkMinutes > 0) parts.add(checkMinutes.toString() + " MIN")
    return if (parts.isEmpty()) "0" else parts.joinToString(" ")
}

fun Watcher.expiryLabel(): String {
    val parts = mutableListOf<String>()
    if (expiryMinutes > 0) parts.add(expiryMinutes.toString() + " MIN")
    if (expirySeconds > 0) parts.add(expirySeconds.toString() + " S")
    return if (parts.isEmpty()) "0" else parts.joinToString(" ")
}

/** Just the host, because a full URL crowds the list and says less. */
fun Watcher.hostLabel(): String {
    val host = try {
        URI(url).host
    } catch (e: URISyntaxException) {
        null
    }
    return host ?: "NO URL"
}
