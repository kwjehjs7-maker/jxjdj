package com.so2matchtracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.so2matchtracker.TrackerColors
import com.so2matchtracker.categoryLabel
import com.so2matchtracker.data.MatchEntity
import com.so2matchtracker.domain.MatchResult
import com.so2matchtracker.domain.MatchStats
import com.so2matchtracker.domain.ProblemCategory
import java.util.UUID

data class MatchDraft(
    val result: String = MatchResult.WIN.name,
    val kills: String = "",
    val deaths: String = "",
    val mapName: String = "",
    val mmrChange: String = "",
    val problemCategory: String? = null,
    val note: String = "",
)

@Composable
fun HomeScreen(
    matches: List<MatchEntity>,
    onSave: suspend (MatchEntity) -> Boolean,
) {
    var draft by remember { mutableStateOf(MatchDraft()) }
    var showOptional by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Твоя статистика", "Общий итог по записанным матчам")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val winRate = if (matches.isEmpty()) "—" else
                        "${String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.0f", MatchStats.winRate(matches))}%"
                    StatTile(
                        label = "Матчи",
                        value = matches.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(label = "Победы", value = winRate, modifier = Modifier.weight(1f))
                    StatTile(
                        label = "Общий K/D",
                        value = if (matches.isEmpty()) "—" else MatchStats.formatKd(MatchStats.overallKd(matches)),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Убийства",
                        value = MatchStats.totalKills(matches).toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Смерти",
                        value = MatchStats.totalDeaths(matches).toString(),
                        modifier = Modifier.weight(1f),
                    )
                    val mmr = MatchStats.totalMmr(matches)?.let(::signedText) ?: "—"
                    StatTile(label = "Изменение MMR", value = mmr, modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Записать матч", "Только результат и два числа — остальное по желанию")
                TrackerCard {
                    MatchFormFields(
                        draft = draft,
                        onDraftChange = {
                            draft = it
                            error = null
                        },
                        showOptional = showOptional,
                        onToggleOptional = { showOptional = !showOptional },
                        error = error,
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val kills = draft.kills.toIntOrNull()
                            val deaths = draft.deaths.toIntOrNull()
                            when {
                                draft.result !in MatchResult.entries.map { it.name } ->
                                    error = "Выбери результат матча."
                                kills == null || kills !in 0..9_999 ->
                                    error = "Укажи число убийств от 0 до 9 999."
                                deaths == null || deaths !in 0..9_999 ->
                                    error = "Укажи число смертей от 0 до 9 999."
                                draft.mmrChange.isNotBlank() &&
                                    (draft.mmrChange.toIntOrNull() == null || draft.mmrChange.toInt() !in -100_000..100_000) ->
                                    error = "Изменение MMR должно быть целым числом от −100 000 до 100 000."
                                else -> {
                                    val match = MatchEntity(
                                        id = UUID.randomUUID().toString(),
                                        result = draft.result,
                                        kills = kills,
                                        deaths = deaths,
                                        mapName = draft.mapName.trim().takeIf { it.isNotEmpty() },
                                        mmrChange = draft.mmrChange.toIntOrNull(),
                                        problemCategory = draft.problemCategory,
                                        note = draft.note.trim().takeIf { it.isNotEmpty() },
                                        playedAt = System.currentTimeMillis(),
                                    )
                                    scope.launch {
                                        if (onSave(match)) {
                                            draft = MatchDraft()
                                            showOptional = false
                                            error = null
                                        } else {
                                            error = "Не удалось сохранить матч. Данные формы оставлены."
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrackerColors.lime,
                            contentColor = TrackerColors.background,
                        ),
                    ) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить матч", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Text(
                text = "Время матча запишется автоматически. Данные остаются только на этом устройстве.",
                style = MaterialTheme.typography.bodySmall,
                color = TrackerColors.muted,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun MatchFormFields(
    draft: MatchDraft,
    onDraftChange: (MatchDraft) -> Unit,
    showOptional: Boolean,
    onToggleOptional: () -> Unit,
    error: String?,
    showOptionalControl: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Результат", style = MaterialTheme.typography.labelLarge, color = TrackerColors.muted)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MatchResult.entries.forEach { result ->
                ChoicePill(
                    label = result.label,
                    selected = draft.result == result.name,
                    onClick = { onDraftChange(draft.copy(result = result.name)) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(
                label = "Убийства",
                value = draft.kills,
                onValueChange = { onDraftChange(draft.copy(kills = it.filter { char -> char.isDigit() }.take(4))) },
                modifier = Modifier.weight(1f),
                allowSign = false,
            )
            NumberField(
                label = "Смерти",
                value = draft.deaths,
                onValueChange = { onDraftChange(draft.copy(deaths = it.filter { char -> char.isDigit() }.take(4))) },
                modifier = Modifier.weight(1f),
                allowSign = false,
            )
        }

        if (showOptionalControl) {
            TextButton(
                onClick = onToggleOptional,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(
                    if (showOptional) "Скрыть дополнительные поля" else "Добавить карту, MMR или заметку",
                    color = TrackerColors.lime,
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = TrackerColors.lime,
                )
            }
        }

        if (showOptional) {
            OutlinedTextField(
                value = draft.mapName,
                onValueChange = { onDraftChange(draft.copy(mapName = it.take(60))) },
                label = { Text("Карта") },
                placeholder = { Text("Например, Sandstone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = trackerFieldColors(),
            )
            NumberField(
                label = "Изменение MMR",
                value = draft.mmrChange,
                onValueChange = { raw ->
                    val digits = raw.filter { char -> char.isDigit() }.take(6)
                    onDraftChange(draft.copy(mmrChange = if (raw.startsWith("-")) "-$digits" else digits))
                },
                modifier = Modifier.fillMaxWidth(),
                allowSign = true,
            )
            CategoryPicker(
                selected = draft.problemCategory,
                onSelected = { onDraftChange(draft.copy(problemCategory = it)) },
            )
            OutlinedTextField(
                value = draft.note,
                onValueChange = { onDraftChange(draft.copy(note = it.take(2_000))) },
                label = { Text("Заметка") },
                placeholder = { Text("Что заметил в этом матче?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                colors = trackerFieldColors(),
            )
        }

        if (error != null) {
            Text(
                text = error,
                color = TrackerColors.red,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowSign: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowSign) KeyboardType.NumberSigned else KeyboardType.Number,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = trackerFieldColors(),
    )
}

@Composable
private fun CategoryPicker(
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Главная проблема", style = MaterialTheme.typography.labelMedium, color = TrackerColors.muted)
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TrackerColors.border),
            ) {
                Text(
                    text = categoryLabel(selected),
                    modifier = Modifier.weight(1f),
                    color = TrackerColors.text,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = TrackerColors.muted)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(TrackerColors.surfaceRaised),
            ) {
                DropdownMenuItem(
                    text = { Text("Не отмечена") },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    },
                )
                ProblemCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.label) },
                        onClick = {
                            onSelected(category.name)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val fill = if (selected) TrackerColors.limeDim else TrackerColors.surfaceRaised
    val labelColor = if (selected) TrackerColors.lime else TrackerColors.text
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, color = labelColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) TrackerColors.lime.copy(alpha = 0.45f) else TrackerColors.border),
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = fill,
            selectedContainerColor = fill,
        ),
    )
}

@Composable
fun TrackerCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TrackerColors.surface, RoundedCornerShape(20.dp))
            .padding(15.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TrackerColors.text, fontWeight = FontWeight.Bold)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = TrackerColors.muted)
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TrackerColors.surface, RoundedCornerShape(15.dp))
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TrackerColors.muted,
            maxLines = 1,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = TrackerColors.lime,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun trackerFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = TrackerColors.text,
    unfocusedTextColor = TrackerColors.text,
    focusedContainerColor = TrackerColors.surfaceRaised,
    unfocusedContainerColor = TrackerColors.surfaceRaised,
    focusedIndicatorColor = TrackerColors.lime,
    unfocusedIndicatorColor = TrackerColors.border,
    focusedLabelColor = TrackerColors.lime,
    unfocusedLabelColor = TrackerColors.muted,
    cursorColor = TrackerColors.lime,
)

private fun signedText(value: Long): String = if (value > 0) "+$value" else value.toString()
