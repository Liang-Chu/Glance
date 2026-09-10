package dev.liamchu.blip

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The settings of DESIGN.md "What the user can change" and nothing else
 * (criterion settings-1), in the monochrome of DESIGN.md "How it looks".
 * Once they are filled in there is no reason to come back here.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlipTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = BlipBlack) {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf("") }
    var intervalUnit by remember { mutableStateOf(SettingsStore.STARTING_INTERVAL_UNIT) }
    var expiry by remember { mutableStateOf("") }
    var expiryUnit by remember { mutableStateOf(SettingsStore.STARTING_EXPIRY_UNIT) }
    var maxLength by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var notificationsAllowed by remember { mutableStateOf(true) }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsAllowed = Notifier.canPost(context) }

    LaunchedEffect(Unit) {
        val stored = SettingsStore.read(context)
        url = stored.url
        credential = stored.credential
        interval = stored.intervalValue.toString()
        intervalUnit = stored.intervalUnit
        expiry = stored.expiryValue.toString()
        expiryUnit = stored.expiryUnit
        maxLength = stored.maxLength.toString()
        notificationsAllowed = Notifier.canPost(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("BLIP", style = MaterialTheme.typography.headlineSmall, color = BlipWhite)
        PixelRule()
        Text(
            "BACKEND -> NOTIFICATION -> GONE",
            style = MaterialTheme.typography.labelSmall,
            color = BlipGrey,
        )

        Field("Backend URL", url, KeyboardType.Uri) { url = it }
        Field("Credential / optional", credential, KeyboardType.Password) { credential = it }

        Field("Check every", interval, KeyboardType.Number) { interval = it }
        UnitPicker(IntervalUnit.entries, intervalUnit, { it.label }) { intervalUnit = it }

        Field("Expires after", expiry, KeyboardType.Number) { expiry = it }
        UnitPicker(ExpiryUnit.entries, expiryUnit, { it.label }) { expiryUnit = it }
        if (expiry.trim() == "0") {
            Text(
                "> 0 = GONE FROM THE PHONE AT ONCE. THE GLASSES STILL GET IT.",
                style = MaterialTheme.typography.bodySmall,
                color = BlipGrey,
            )
        }

        Field("Max length / characters", maxLength, KeyboardType.Number) { maxLength = it }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = BlipWhite,
                contentColor = BlipBlack,
            ),
            onClick = {
                val problem = firstProblem(url, interval, intervalUnit, expiry, maxLength)
                if (problem != null) {
                    message = problem
                    return@Button
                }
                scope.launch {
                    val value = interval.trim().toInt()
                    SettingsStore.save(
                        context = context,
                        url = url.trim(),
                        credential = credential,
                        intervalValue = value,
                        intervalUnit = intervalUnit,
                        expiryValue = expiry.trim().toInt(),
                        expiryUnit = expiryUnit,
                        maxLength = maxLength.trim().toInt(),
                    )
                    Scheduler.schedule(context, value * intervalUnit.minutesEach)
                    message = "SAVED. CHECKING EVERY " + value + " " + intervalUnit.label + "."
                }
            },
        ) {
            Text("SAVE AND SCHEDULE", style = MaterialTheme.typography.labelLarge)
        }

        if (message.isNotEmpty()) {
            Text(
                "> " + message,
                style = MaterialTheme.typography.bodySmall,
                color = BlipGrey,
            )
        }

        if (!notificationsAllowed) {
            PixelRule(BlipWhite)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlipWhite)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "NOTIFICATIONS OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = BlipBlack,
                )
                Text(
                    "Nothing can be shown. Allow them, then add Blip in the " +
                        "Even Realities app under Notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BlipBlack,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlipBlack,
                            contentColor = BlipWhite,
                        ),
                        onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    ) {
                        Text("ALLOW", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** A dashed rule drawn as blocks, so it reads as pixels rather than a hairline. */
@Composable
private fun PixelRule(colour: Color = BlipDimGrey) {
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

/** Square blocks, selected one inverted. No dropdown, and no colour. */
@Composable
private fun <T> UnitPicker(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val chosen = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(if (chosen) BlipWhite else BlipNearBlack)
                    .border(1.dp, if (chosen) BlipWhite else BlipDimGrey)
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chosen) BlipBlack else BlipGrey,
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = BlipGrey,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onChange,
            singleLine = true,
            shape = RectangleShape,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BlipWhite,
                unfocusedTextColor = BlipWhite,
                focusedBorderColor = BlipWhite,
                unfocusedBorderColor = BlipDimGrey,
                focusedContainerColor = BlipNearBlack,
                unfocusedContainerColor = BlipNearBlack,
                cursorColor = BlipWhite,
            ),
        )
    }
}

/**
 * Criterion settings-3: refused where it is typed. Nothing is coerced or
 * defaulted on the way through — a field that does not parse stops the save.
 */
private fun firstProblem(
    url: String,
    interval: String,
    intervalUnit: IntervalUnit,
    expiry: String,
    maxLength: String,
): String? {
    if (!isUsableUrl(url.trim())) return "BACKEND URL MUST BE A FULL HTTP:// OR HTTPS:// ADDRESS."
    return intervalProblem(interval, intervalUnit)
        ?: expiryProblem(expiry)
        ?: maxLengthProblem(maxLength)
}
