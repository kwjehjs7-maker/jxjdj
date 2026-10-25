package com.so2matchtracker.domain

enum class MatchResult(val label: String) {
    WIN("Победа"),
    LOSS("Поражение"),
    DRAW("Ничья"),
}

enum class ProblemCategory(val label: String) {
    AIM("Прицел и наведение"),
    MOVEMENT("Движение и остановка"),
    POSITIONING("Позиционирование и укрытия"),
    DECISIONS("Решения и тайминги"),
    RECOIL("Контроль отдачи"),
    TECHNICAL("FPS, лаги и помехи"),
    NONE("Неясно / явной проблемы нет"),
}

enum class AnalysisPeriod(val label: String, val days: Int?) {
    WEEK("7 дней", 7),
    MONTH("30 дней", 30),
    QUARTER("90 дней", 90),
    ALL("Всё время", null),
}

enum class MatchSort(val label: String) {
    DATE("Сначала новые"),
    KD("По K/D"),
    MMR("По изменению MMR"),
}
