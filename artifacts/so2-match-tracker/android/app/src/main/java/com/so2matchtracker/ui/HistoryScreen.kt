package com.so2matchtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import com.so2matchtracker.TrackerColors
import com.so2matchtracker.categoryLabel
import com.so2matchtracker.data.MatchEntity
import com.so2matchtracker.domain.AnalysisPeriod
import com.so2matchtracker.domain.MatchResult
import com.so2matchtracker.domain.MatchSort
import com.so2matchtracker.domain.MatchStats
import com.so2matchtracker.domain.ProblemCategory
import com.so2matchtracker.formatDateTime
import com.so2matchtracker.formatSigned
import com.so2matchtracker.resultLabel
import java.util.Locale

@Composable
fun HistoryScreen(
    matches: List<MatchEntity>,
    onOpenMatch: (MatchEntity) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var resultFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var period by remember { mutableStateOf(AnalysisPeriod.ALL) }
    var sort by remember { mutableStateOf(MatchSort.DATE) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPeriodMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val filtered = matches.asSequence()
        .filter { resultFilter == null || it.result == resultFilter }
        .filter { categoryFilter == null || it.problemCategory == categoryFilter }
        .filter { period.days == null || it.playedAt >= now - period.days * 86_400_000L }
        .filter {
            val text = query.trim().lowercase(Locale.forLanguageTag("ru-RU"))
            text.isEmpty() || it.mapName.orEmpty().lowercase(Locale.forLanguageTag("ru-RU")).contains(text) ||
                it.note.orEmpty().lowercase(Locale.forLanguageTag("ru-RU")).contains(text)
        }
        .toList()
        .let { list ->
            when (sort) {
                MatchSort.DATE -> list.sortedByDescending { it.playedAt }
                MatchSort.KD -> list.sortedByDescending { MatchStats.matchKd(it) }
                MatchSort.MMR -> list.sortedByDescending { it.mmrChange ?: Int.MIN_VALUE }
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("История матчей", "Новые записи отображаются первыми")
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TrackerColors.muted) },
                placeholder = { Text("Поиск по карте и заметкам") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = trackerFieldColors(),
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChoicePill("Все", selected = resultFilter == null, onClick = { resultFilter = null })
                MatchResult.entries.forEach { result ->
                    ChoicePill(
                        result.label,
                        selected = resultFilter == result.name,
                        onClick = {
                            resultFilter = if (resultFilter == result.name) null else result.name
                        },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMenuButton(
                    label = period.label,
                    expanded = showPeriodMenu,
                    onExpand = { showPeriodMenu = true },
                    onDismiss = { showPeriodMenu = false },
                    modifier = Modifier.weight(1f),
                ) {
                    AnalysisPeriod.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                period = option
                                showPeriodMenu = false
                            },
                        )
                    }
                }
                FilterMenuButton(
                    label = if (categoryFilter == null) "Все проблемы" else categoryLabel(categoryFilter),
                    expanded = showCategoryMenu,
                    onExpand = { showCategoryMenu = true },
                    onDismiss = { showCategoryMenu = false },
                    modifier = Modifier.weight(1.5f),
                ) {
                    DropdownMenuItem(
                        text = { Text("Все проблемы") },
                        onClick = {
                            categoryFilter = null
                            showCategoryMenu = false
                        },
                    )
                    ProblemCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.label) },
                            onClick = {
                                categoryFilter = category.name
                                showCategoryMenu = false
                            },
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${filtered.size} матч${matchSuffix(filtered.size)}", color = TrackerColors.muted, style = MaterialTheme.typography.bodySmall)
                FilterMenuButton(
                    label = sort.label,
                    expanded = showSortMenu,
                    onExpand = { showSortMenu = true },
                    onDismiss = { showSortMenu = false },
                ) {
                    MatchSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                sort = option
                                showSortMenu = false
                            },
                        )
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyHistory(hasAny = matches.isNotEmpty())
            }
        } else {
            items(filtered, key = { it.id }) { match ->
                MatchHistoryCard(match = match, onClick = { onOpenMatch(match) })
            }
        }
    }
}

@Composable
private fun FilterMenuButton(
    label: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        OutlinedButton(
            onClick = onExpand,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = TrackerColors.text,
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
            )
            Text("⌄", color = TrackerColors.muted)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.background(TrackerColors.surfaceRaised),
            content = menuContent,
        )
    }
}

@Composable
private fun MatchHistoryCard(match: MatchEntity, onClick: () -> Unit) {
    val resultColor = when (match.result) {
        MatchResult.WIN.name -> TrackerColors.lime
        MatchResult.LOSS.name -> TrackerColors.red
        else -> TrackerColors.blue
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = TrackerColors.surface,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    resultLabel(match.result),
                    color = resultColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                match.mapName?.let {
                    Text("  ·  $it", color = TrackerColors.text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Text(formatDateTime(match.playedAt), color = TrackerColors.muted, style = MaterialTheme.typography.labelSmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${match.kills} / ${match.deaths}", color = TrackerColors.text, fontWeight = FontWeight.SemiBold)
                Text("K/D ${MatchStats.formatKd(MatchStats.matchKd(match))}", color = TrackerColors.text)
                Text(
                    text = match.mmrChange?.let { "MMR ${formatSigned(it)}" } ?: "MMR —",
                    color = match.mmrChange?.let { if (it > 0) TrackerColors.lime else if (it < 0) TrackerColors.red else TrackerColors.muted }
                        ?: TrackerColors.muted,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (match.problemCategory != null) {
                Text(
                    text = "Отметка: ${categoryLabel(match.problemCategory)}",
                    color = TrackerColors.muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(hasAny: Boolean) {
    TrackerCard {
        Text(
            text = if (hasAny) "По этим условиям матчей нет" else "Пока нет записей",
            color = TrackerColors.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (hasAny) "Измени поиск или фильтры." else "После игры сохрани результат на вкладке «Запись».",
            color = TrackerColors.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun MatchDetailsDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrackerColors.surface,
        title = {
            Text(
                "${resultLabel(match.result)} · ${formatDateTime(match.playedAt)}",
                color = TrackerColors.text,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                DetailLine("Карта", match.mapName ?: "Не указана")
                DetailLine("Убийства / смерти", "${match.kills} / ${match.deaths}")
                DetailLine("K/D", MatchStats.formatKd(MatchStats.matchKd(match)))
                DetailLine("Изменение MMR", match.mmrChange?.let(::formatSigned) ?: "Не указано")
                DetailLine("Главная проблема", categoryLabel(match.problemCategory))
                DetailLine("Заметка", match.note?.takeIf { it.isNotBlank() } ?: "Нет")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Удалить", color = TrackerColors.red)
                }
                TextButton(onClick = onEdit) {
                    Text("Изменить", color = TrackerColors.lime)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = TrackerColors.muted) }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TrackerColors.muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = TrackerColors.text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MatchEditDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onSave: (MatchEntity) -> Unit,
) {
    var draft by remember(match.id) {
        mutableStateOf(
            MatchDraft(
                result = match.result,
                kills = match.kills.toString(),
                deaths = match.deaths.toString(),
                mapName = match.mapName.orEmpty(),
                mmrChange = match.mmrChange?.toString().orEmpty(),
                problemCategory = match.problemCategory,
                note = match.note.orEmpty(),
            ),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrackerColors.surface,
        title = { Text("Изменить матч", color = TrackerColors.text) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MatchFormFields(
                    draft = draft,
                    onDraftChange = { draft = it; error = null },
                    showOptional = true,
                    onToggleOptional = {},
                    error = error,
                    showOptionalControl = false,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val kills = draft.kills.toIntOrNull()
                    val deaths = draft.deaths.toIntOrNull()
                    val mmr = draft.mmrChange.toIntOrNull()
                    when {
                        kills == null || kills !in 0..9_999 -> error = "Проверь число убийств."
                        deaths == null || deaths !in 0..9_999 -> error = "Проверь число смертей."
                        draft.mmrChange.isNotBlank() && (mmr == null || mmr !in -100_000..100_000) ->
                            error = "Проверь изменение MMR."
                        else -> onSave(
                            match.copy(
                                result = draft.result,
                                kills = kills,
                                deaths = deaths,
                                mapName = draft.mapName.trim().takeIf { it.isNotEmpty() },
                                mmrChange = mmr,
                                problemCategory = draft.problemCategory,
                                note = draft.note.trim().takeIf { it.isNotEmpty() },
                            ),
                        )
                    }
                },
            ) {
                Text("Сохранить", color = TrackerColors.lime)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TrackerColors.muted) }
        },
    )
}

@Composable
fun DeleteMatchDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrackerColors.surface,
        title = { Text("Удалить матч?", color = TrackerColors.text) },
        text = { Text("Запись будет удалена с устройства. Это действие нельзя отменить.", color = TrackerColors.muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Удалить", color = TrackerColors.red) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Оставить", color = TrackerColors.lime) }
        },
    )
}

private fun matchSuffix(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    return when {
        lastTwo in 11..14 -> "ей"
        last == 1 -> ""
        last in 2..4 -> "а"
        else -> "ей"
    }
}
