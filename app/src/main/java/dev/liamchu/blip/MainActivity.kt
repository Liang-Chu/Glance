package dev.liamchu.blip

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The five fields of DESIGN.md "What the user can change" and nothing else
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
    var frequency by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
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
        frequency = stored.frequencyHours.toString()
        expiry = stored.expiryMinutes.toString()
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
        Field("Check every / hours", frequency, KeyboardType.Number) { frequency = it }
        Field("Expires after / minutes", expiry, KeyboardType.Number) { expiry = it }
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
                val problem = firstProblem(url, frequency, expiry, maxLength)
                if (problem != null) {
                    message = problem
                    return@Button
                }
                scope.launch {
                    val hours = frequency.trim().toInt()
                    SettingsStore.save(
                        context = context,
                        url = url.trim(),
                        credential = credential,
                        frequencyHours = hours,
                        expiryMinutes = expiry.trim().toInt(),
                        maxLength = maxLength.trim().toInt(),
                    )
                    Scheduler.schedule(context, hours)
                    message = "SAVED. NEXT CHECK WITHIN " + hours + "H."
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
    frequency: String,
    expiry: String,
    maxLength: String,
): String? {
    if (!isUsableUrl(url.trim())) return "BACKEND URL MUST BE A FULL HTTP:// OR HTTPS:// ADDRESS."
    if (!isPositive(frequency)) return "CHECK EVERY: WHOLE HOURS, AT LEAST 1."
    if (!isPositive(expiry)) return "EXPIRES AFTER: WHOLE MINUTES, AT LEAST 1."
    if (!isPositive(maxLength)) return "MAX LENGTH: WHOLE CHARACTERS, AT LEAST 1."
    return null
}

private fun isPositive(field: String): Boolean {
    val parsed = field.trim().toIntOrNull()
    return parsed != null && parsed >= 1
}
