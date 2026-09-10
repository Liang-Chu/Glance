package dev.liamchu.glance

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
 * The settings of DESIGN.md "What the user can change" and nothing else
 * (criterion settings-1), in the monochrome of DESIGN.md "How it looks".
 * Durations are one box per unit, added together.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlanceTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = GlanceBlack) {
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
    var checkDays by remember { mutableStateOf("") }
    var checkHours by remember { mutableStateOf("") }
    var checkMinutes by remember { mutableStateOf("") }
    var expiryMinutes by remember { mutableStateOf("") }
    var expirySeconds by remember { mutableStateOf("") }
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
        checkDays = stored.checkDays.toString()
        checkHours = stored.checkHours.toString()
        checkMinutes = stored.checkMinutes.toString()
        expiryMinutes = stored.expiryMinutes.toString()
        expirySeconds = stored.expirySeconds.toString()
        maxLength = stored.maxLength.toString()
        notificationsAllowed = Notifier.canPost(context)
    }

    // Saving is the same work from either button; only what follows it differs.
    fun saveThen(action: (Settings) -> Unit, note: (Settings) -> String) {
        val problem = firstProblem(url, checkDays, checkHours, checkMinutes,
            expiryMinutes, expirySeconds, maxLength)
        if (problem != null) {
            message = problem
            return
        }
        scope.launch {
            SettingsStore.save(
                context = context,
                url = url.trim(),
                credential = credential,
                checkDays = partOrNull(checkDays)!!,
                checkHours = partOrNull(checkHours)!!,
                checkMinutes = partOrNull(checkMinutes)!!,
                expiryMinutes = partOrNull(expiryMinutes)!!,
                expirySeconds = partOrNull(expirySeconds)!!,
                maxLength = maxLength.trim().toInt(),
            )
            val saved = SettingsStore.read(context)
            action(saved)
            message = note(saved)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("GLANCE", style = MaterialTheme.typography.headlineSmall, color = GlanceWhite)
        PixelRule()
        Text(
            "BACKEND -> NOTIFICATION -> GONE",
            style = MaterialTheme.typography.labelSmall,
            color = GlanceGrey,
        )

        Field("Backend URL", url, KeyboardType.Uri) { url = it }
        Field("Credential / optional", credential, KeyboardType.Password) { credential = it }

        Duration("Check every") {
            NumberCell("DAYS", checkDays, Modifier.weight(1f)) { checkDays = it }
            NumberCell("HOURS", checkHours, Modifier.weight(1f)) { checkHours = it }
            NumberCell("MINS", checkMinutes, Modifier.weight(1f)) { checkMinutes = it }
        }
        Text(
            "> AT LEAST 15 MINUTES IN TOTAL.",
            style = MaterialTheme.typography.bodySmall,
            color = GlanceGrey,
        )

        Duration("Expires after") {
            NumberCell("MINS", expiryMinutes, Modifier.weight(1f)) { expiryMinutes = it }
            NumberCell("SECS", expirySeconds, Modifier.weight(1f)) { expirySeconds = it }
            Box(Modifier.weight(1f))
        }
        Text(
            "> AT LEAST 3 SECONDS, SO THE GLASSES RECEIVE IT FIRST.",
            style = MaterialTheme.typography.bodySmall,
            color = GlanceGrey,
        )

        Field("Max length / characters", maxLength, KeyboardType.Number) { maxLength = it }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = GlanceWhite,
                contentColor = GlanceBlack,
            ),
            onClick = {
                saveThen({ Scheduler.schedule(context, it.intervalMinutes) }) { saved ->
                    "SAVED. CHECKING EVERY " + saved.intervalMinutes + " MIN."
                }
            },
        ) {
            Text("SAVE AND SCHEDULE", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RectangleShape,
            border = BorderStroke(1.dp, GlanceWhite),
            colors = ButtonDefaults.buttonColors(
                containerColor = GlanceBlack,
                contentColor = GlanceWhite,
            ),
            onClick = {
                saveThen({ Scheduler.checkNow(context) }) { "SAVED. CHECKING NOW." }
            },
        ) {
            Text("SAVE AND CHECK NOW", style = MaterialTheme.typography.labelLarge)
        }

        if (message.isNotEmpty()) {
            Text(
                "> " + message,
                style = MaterialTheme.typography.bodySmall,
                color = GlanceGrey,
            )
        }

        if (!notificationsAllowed) {
            PixelRule(GlanceWhite)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlanceWhite)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "NOTIFICATIONS OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlanceBlack,
                )
                Text(
                    "Nothing can be shown. Allow them, then add Glance in the " +
                        "Even Realities app under Notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlanceBlack,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlanceBlack,
                            contentColor = GlanceWhite,
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
private fun PixelRule(colour: Color = GlanceDimGrey) {
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

/** One heading over a row of unit boxes that add up. */
@Composable
private fun Duration(label: String, cells: @Composable RowScope.() -> Unit) {
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
private fun NumberCell(
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

@Composable
private fun fieldColours() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlanceWhite,
    unfocusedTextColor = GlanceWhite,
    focusedBorderColor = GlanceWhite,
    unfocusedBorderColor = GlanceDimGrey,
    focusedContainerColor = GlanceNearBlack,
    unfocusedContainerColor = GlanceNearBlack,
    cursorColor = GlanceWhite,
)

/**
 * Criterion settings-3: refused where it is typed. Nothing is coerced or
 * defaulted on the way through — a field that does not parse stops the save.
 */
private fun firstProblem(
    url: String,
    checkDays: String,
    checkHours: String,
    checkMinutes: String,
    expiryMinutes: String,
    expirySeconds: String,
    maxLength: String,
): String? {
    if (!isUsableUrl(url.trim())) return "BACKEND URL MUST BE A FULL HTTP:// OR HTTPS:// ADDRESS."
    return intervalProblem(checkDays, checkHours, checkMinutes)
        ?: expiryProblem(expiryMinutes, expirySeconds)
        ?: maxLengthProblem(maxLength)
}
