package com.so2matchtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.so2matchtracker.data.MatchDao
import com.so2matchtracker.data.MatchEntity
import com.so2matchtracker.data.TrackerDatabase
import com.so2matchtracker.ui.AnalyticsScreen
import com.so2matchtracker.ui.BackupDialog
import com.so2matchtracker.ui.DeleteMatchDialog
import com.so2matchtracker.ui.HistoryScreen
import com.so2matchtracker.ui.HomeScreen
import com.so2matchtracker.ui.MatchDetailsDialog
import com.so2matchtracker.ui.MatchEditDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrackerColors {
    val background = Color(0xFF10120F)
    val surface = Color(0xFF1A1D18)
    val surfaceRaised = Color(0xFF22261F)
    val lime = Color(0xFFC9F36B)
    val limeDim = Color(0xFF303A20)
    val text = Color(0xFFF0F3E9)
    val muted = Color(0xFF92998C)
    val border = Color(0xFF30352B)
    val red = Color(0xFFFF8585)
    val blue = Color(0xFF8FB8FF)
}

private enum class TrackerTab(val label: String) {
    RECORD("Запись"),
    HISTORY("История"),
    ANALYTICS("Аналитика"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = TrackerColors.background.toArgb()
        window.navigationBarColor = TrackerColors.background.toArgb()
        window.decorView.systemUiVisibility = 0
        val dao = TrackerDatabase.get(applicationContext).matchDao()
        setContent {
            TrackerTheme {
                MatchTrackerApp(dao = dao)
            }
        }
    }
}

@Composable
private fun TrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TrackerColors.lime,
            onPrimary = TrackerColors.background,
            secondary = TrackerColors.blue,
            background = TrackerColors.background,
            onBackground = TrackerColors.text,
            surface = TrackerColors.surface,
            onSurface = TrackerColors.text,
            surfaceVariant = TrackerColors.surfaceRaised,
            onSurfaceVariant = TrackerColors.muted,
            outline = TrackerColors.border,
            error = TrackerColors.red,
        ),
        content = content,
    )
}

@Composable
private fun MatchTrackerApp(dao: MatchDao) {
    val matches by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(TrackerTab.RECORD) }
    var showBackup by remember { mutableStateOf(false) }
    var selectedMatch by remember { mutableStateOf<MatchEntity?>(null) }
    var editingMatch by remember { mutableStateOf<MatchEntity?>(null) }
    var deletingMatch by remember { mutableStateOf<MatchEntity?>(null) }

    Scaffold(
        containerColor = TrackerColors.background,
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        bottomBar = {
            NavigationBar(
                containerColor = TrackerColors.background,
                contentColor = TrackerColors.text,
            ) {
                TrackerTab.entries.forEach { item ->
                    val selected = tab == item
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tab = item },
                        icon = {
                            val image = when (item) {
                                TrackerTab.RECORD -> Icons.Outlined.SportsEsports
                                TrackerTab.HISTORY -> Icons.Outlined.History
                                TrackerTab.ANALYTICS -> Icons.Outlined.Analytics
                            }
                            Icon(imageVector = image, contentDescription = item.label)
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TrackerColors.background)
                .padding(contentPadding)
                .imePadding(),
        ) {
            TrackerTopBar(
                onBackup = { showBackup = true },
                totalMatches = matches.size,
            )
            when (tab) {
                TrackerTab.RECORD -> HomeScreen(
                    matches = matches,
                    onSave = { match ->
                        try {
                            dao.insert(match)
                            snackbar.showSnackbar("Матч сохранён")
                            true
                        } catch (_: Exception) {
                            false
                        }
                    },
                )
                TrackerTab.HISTORY -> HistoryScreen(
                    matches = matches,
                    onOpenMatch = { selectedMatch = it },
                )
                TrackerTab.ANALYTICS -> AnalyticsScreen(matches = matches)
            }
        }
    }

    if (showBackup) {
        BackupDialog(
            matches = matches,
            dao = dao,
            onDismiss = { showBackup = false },
            onMessage = { message ->
                scope.launch { snackbar.showSnackbar(message) }
            },
        )
    }

    selectedMatch?.let { match ->
        MatchDetailsDialog(
            match = match,
            onDismiss = { selectedMatch = null },
            onEdit = {
                selectedMatch = null
                editingMatch = match
            },
            onDelete = {
                selectedMatch = null
                deletingMatch = match
            },
        )
    }

    editingMatch?.let { match ->
        MatchEditDialog(
            match = match,
            onDismiss = { editingMatch = null },
            onSave = { updated ->
                scope.launch {
                    try {
                        dao.update(updated)
                        editingMatch = null
                        snackbar.showSnackbar("Изменения сохранены")
                    } catch (_: Exception) {
                        snackbar.showSnackbar("Не удалось сохранить изменения")
                    }
                }
            },
        )
    }

    deletingMatch?.let { match ->
        DeleteMatchDialog(
            onDismiss = { deletingMatch = null },
            onConfirm = {
                scope.launch {
                    try {
                        dao.delete(match)
                        deletingMatch = null
                        snackbar.showSnackbar("Запись удалена")
                    } catch (_: Exception) {
                        snackbar.showSnackbar("Не удалось удалить запись")
                    }
                }
            },
        )
    }
}

@Composable
private fun TrackerTopBar(onBackup: () -> Unit, totalMatches: Int) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .background(TrackerColors.background)
            .padding(start = 20.dp, end = 12.dp, top = 9.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SO2 MATCH TRACKER",
                style = MaterialTheme.typography.labelSmall,
                color = TrackerColors.lime,
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
            )
            Text(
                text = "Личный разбор матчей",
                style = MaterialTheme.typography.titleMedium,
                color = TrackerColors.text,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = totalMatches.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = TrackerColors.muted,
        )
        IconButton(onClick = onBackup) {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = "Резервная копия",
                tint = TrackerColors.lime,
            )
        }
    }
}

fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("ru-RU")).format(Date(timestamp))

fun formatSigned(value: Int): String = when {
    value > 0 -> "+$value"
    else -> value.toString()
}

fun resultLabel(result: String): String =
    MatchResult.entries.firstOrNull { it.name == result }?.label ?: "Неизвестно"

fun categoryLabel(category: String?): String =
    ProblemCategory.entries.firstOrNull { it.name == category }?.label ?: "Не отмечена"
