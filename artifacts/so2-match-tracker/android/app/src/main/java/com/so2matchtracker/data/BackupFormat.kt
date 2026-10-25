package com.so2matchtracker.data

import com.so2matchtracker.domain.MatchResult
import com.so2matchtracker.domain.ProblemCategory
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object BackupFormat {
    private const val SCHEMA_VERSION = 1
    private const val MAX_RECORDS = 20_000

    fun encode(matches: List<MatchEntity>): String {
        val root = JSONObject()
            .put("format", "so2-match-tracker")
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", System.currentTimeMillis())
        val records = JSONArray()
        matches.sortedBy { it.playedAt }.forEach { match ->
            records.put(
                JSONObject()
                    .put("id", match.id)
                    .put("result", match.result)
                    .put("kills", match.kills)
                    .put("deaths", match.deaths)
                    .put("mapName", match.mapName ?: JSONObject.NULL)
                    .put("mmrChange", match.mmrChange ?: JSONObject.NULL)
                    .put("problemCategory", match.problemCategory ?: JSONObject.NULL)
                    .put("note", match.note ?: JSONObject.NULL)
                    .put("playedAt", match.playedAt),
            )
        }
        root.put("matches", records)
        return root.toString(2)
    }

    @Throws(IllegalArgumentException::class)
    fun decode(json: String): List<MatchEntity> {
        try {
            val root = JSONObject(json)
            require(root.opt("format") == "so2-match-tracker") {
                "Файл не похож на резервную копию SO2 Match Tracker."
            }
            val version = root.opt("schemaVersion")
            require(version is Number && version.toDouble() == SCHEMA_VERSION.toDouble()) {
                "Версия формата не поддерживается."
            }
            val records = root.opt("matches")
            require(records is JSONArray) { "В файле не найден список матчей." }
            require(records.length() <= MAX_RECORDS) {
                "В файле слишком много записей (максимум $MAX_RECORDS)."
            }

            val ids = mutableSetOf<String>()
            return List(records.length()) { index ->
                val record = records.opt(index) as? JSONObject
                    ?: throw IllegalArgumentException("Запись ${index + 1} повреждена.")
                val id = requiredString(record, "id", maxLength = 100)
                require(id.isNotBlank()) { "У записи ${index + 1} отсутствует идентификатор." }
                require(ids.add(id)) { "В файле повторяется идентификатор матча." }

                val result = requiredString(record, "result", 16)
                require(result in MatchResult.entries.map { it.name }) {
                    "У записи ${index + 1} некорректный результат."
                }
                val kills = requiredInt(record, "kills", 0..9_999)
                val deaths = requiredInt(record, "deaths", 0..9_999)
                val playedAt = requiredLong(record, "playedAt", 1L..Long.MAX_VALUE)
                val mapName = optionalString(record, "mapName", 60)
                val note = optionalString(record, "note", 2_000)
                val mmr = optionalInt(record, "mmrChange", -100_000..100_000)
                val category = optionalString(record, "problemCategory", 32)
                require(category == null || category in ProblemCategory.entries.map { it.name }) {
                    "У записи ${index + 1} неизвестная категория проблемы."
                }

                MatchEntity(
                    id = id,
                    result = result,
                    kills = kills,
                    deaths = deaths,
                    mapName = mapName,
                    mmrChange = mmr,
                    problemCategory = category,
                    note = note,
                    playedAt = playedAt,
                )
            }
        } catch (error: JSONException) {
            throw IllegalArgumentException("Не удалось прочитать JSON: файл повреждён или имеет неверный формат.")
        }
    }

    private fun requiredString(json: JSONObject, key: String, maxLength: Int): String {
        val value = json.opt(key)
        require(value is String && value.length <= maxLength) {
            "Поле «$key» отсутствует или имеет неверный формат."
        }
        return value
    }

    private fun optionalString(json: JSONObject, key: String, maxLength: Int): String? {
        if (!json.has(key) || json.isNull(key)) return null
        val value = json.opt(key)
        require(value is String && value.length <= maxLength) {
            "Поле «$key» имеет неверный формат."
        }
        return value.takeIf { it.isNotBlank() }
    }

    private fun requiredInt(json: JSONObject, key: String, range: IntRange): Int {
        val value = json.opt(key)
        require(value is Number) { "Поле «$key» отсутствует или не является числом." }
        val number = value.toDouble()
        require(number.isFinite() && number % 1.0 == 0.0 && number >= Int.MIN_VALUE && number <= Int.MAX_VALUE) {
            "Поле «$key» должно быть целым числом."
        }
        return number.toInt().also {
            require(it in range) { "Значение «$key» выходит за допустимые пределы." }
        }
    }

    private fun optionalInt(json: JSONObject, key: String, range: IntRange): Int? {
        if (!json.has(key) || json.isNull(key)) return null
        return requiredInt(json, key, range)
    }

    private fun requiredLong(json: JSONObject, key: String, range: LongRange): Long {
        val value = json.opt(key)
        require(value is Number) { "Поле «$key» отсутствует или не является числом." }
        val number = value.toDouble()
        require(number.isFinite() && number % 1.0 == 0.0 && number >= 0.0 && number <= Long.MAX_VALUE.toDouble()) {
            "Поле «$key» должно быть целым числом."
        }
        return value.toLong().also {
            require(it in range) { "Значение «$key» выходит за допустимые пределы." }
        }
    }
}
