package com.example.gymtracker.data.model

enum class SetType(val titleRu: String, val badge: String) {
    NORMAL("Обычный подход", ""),
    WARMUP("Разминочный (W)", "W"),
    DROP("Дропсет (D)", "D"),
    FAILURE("До отказа (F)", "F")
}
