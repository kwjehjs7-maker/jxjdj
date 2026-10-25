package com.so2matchtracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.so2matchtracker.TrackerColors
import com.so2matchtracker.data.BackupFormat
import com.so2matchtracker.data.MatchDao
import com.so2matchtracker.data.MatchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupDialog(
    matches: List<MatchEntity>,
    dao: MatchDao,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<List<MatchEntity>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Не удалось создать файл.")
                output.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(BackupFormat.encode(matches))
                }
            }
            result.onSuccess {
                errorMessage = null
                onMessage("Резервная копия сохранена")
            }.onFailure {
                errorMessage = "Не удалось сохранить файл. Попробуй выбрать другое место."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val parsed = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Не удалось открыть файл.")
                    val bytes = input.use { stream -> stream.readNBytes(20 * 1024 * 1024 + 1) }
                    require(bytes.size <= 20 * 1024 * 1024) { "Файл больше 20 МБ." }
                    BackupFormat.decode(String(bytes, Charsets.UTF_8))
                }
                parsed
            }
            result.onSuccess {
                errorMessage = null
                pendingImport = it
            }.onFailure { failure ->
                errorMessage = failure.message ?: "Не удалось прочитать резервную копию."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrackerColors.surface,
        title = { Text("Резервная копия", color = TrackerColors.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Файл JSON можно сохранить в память телефона или перенести на другое устройство. Матчи никуда не отправляются.",
                    color = TrackerColors.muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                errorMessage?.let {
                    Text(it, color = TrackerColors.red, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        errorMessage = null
                        exportLauncher.launch("so2-match-backup.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.lime,
                        contentColor = TrackerColors.background,
                    ),
                ) {
                    Text("Экспортировать JSON", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        errorMessage = null
                        importLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerColors.surfaceRaised,
                        contentColor = TrackerColors.text,
                    ),
                ) {
                    Text("Импортировать JSON")
                }
                Spacer(Modifier.height(1.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Закрыть", color = TrackerColors.muted)
                }
            }
        },
    )

    pendingImport?.let { imported ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            containerColor = TrackerColors.surface,
            title = { Text("Проверить импорт", color = TrackerColors.text) },
            text = {
                Text(
                    "Формат проверен. В файле ${imported.size} записей. Совпадения по идентификатору будут пропущены; существующие матчи не изменятся и не удалятся.",
                    color = TrackerColors.muted,
                )
                errorMessage?.let {
                    Text(it, color = TrackerColors.red, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val count = dao.insertAllIgnore(imported).count { it != -1L }
                                pendingImport = null
                                onMessage("Добавлено матчей: $count. Старые записи сохранены.")
                            } catch (_: Exception) {
                                errorMessage = "Импорт не завершён. Существующие записи не изменены."
                            }
                        }
                    },
                ) {
                    Text("Добавить новые", color = TrackerColors.lime)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text("Отмена", color = TrackerColors.muted)
                }
            },
        )
    }
}
