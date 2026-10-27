package com.example.gymtracker.domain

import com.example.gymtracker.data.local.entity.ExerciseEntity
import com.example.gymtracker.data.model.MuscleGroup
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ExerciseMatchSource(val badgeRu: String) {
    LOCAL_DB("В вашей базе"),
    ONLINE_CATALOG("В каталоге (970+)"),
    NEW_CUSTOM("Новое упражнение")
}

data class ParsedExerciseItem(
    val id: String = UUID.randomUUID().toString(),
    val rawName: String,
    val matchedExercise: ExerciseEntity? = null,
    val matchSource: ExerciseMatchSource = ExerciseMatchSource.NEW_CUSTOM,
    val customName: String = rawName,
    val customMuscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val targetWeightKg: Float? = null,
    val supersetLabel: String? = null
) {
    val displayName: String
        get() = when (matchSource) {
            ExerciseMatchSource.LOCAL_DB, ExerciseMatchSource.ONLINE_CATALOG ->
                matchedExercise?.name ?: customName
            ExerciseMatchSource.NEW_CUSTOM -> customName
        }

    val displayMuscleGroup: MuscleGroup
        get() = when (matchSource) {
            ExerciseMatchSource.LOCAL_DB, ExerciseMatchSource.ONLINE_CATALOG ->
                matchedExercise?.muscleGroup ?: customMuscleGroup
            ExerciseMatchSource.NEW_CUSTOM -> customMuscleGroup
        }
}

data class ParsedDayPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exercises: List<ParsedExerciseItem>
)

data class ParsedProgramPlan(
    val title: String,
    val description: String,
    val days: List<ParsedDayPlan>
)

object LlmPlanParser {

    val LLM_PROMPT_TEMPLATE = """
Составь для меня программу тренировок в тренажёрном зале.
Пожалуйста, выведи итоговый план строго в следующем текстовом формате (без лишнего форматирования внутри строк), чтобы я мог скопировать и импортировать его в приложение GymTracker:

ПРОГРАММА: [Название программы]
ОПИСАНИЕ: [Краткое описание цели и сплита]

ДЕНЬ: [Название дня, например: День 1 — Грудь и Трицепс]
- [Название упражнения на русском] | [Подходы]x[Повторения] | [Вес в кг или 0] | [Суперсет: A, B или пусто]

Пример:
ПРОГРАММА: Сплит на рельеф и силу (3 дня)
ОПИСАНИЕ: Классический 3-дневный сплит для среднего уровня

ДЕНЬ: День 1 — Грудь и Бицепс
- Жим штанги лёжа | 4x8-10 | 70 |
- Жим гантелей на наклонной скамье | 3x10-12 | 24 |
- Сведение рук в тренажёре (Бабочка) | 3x12-15 | 45 | A
- Сгибание рук со штангой стоя | 3x10-12 | 30 | A
- Молотковые сгибания с гантелями | 3x12 | 14 |
""".trimIndent()

    private val STOP_WORDS = setOf(
        "в", "на", "с", "со", "к", "ко", "из", "за", "по", "под", "над", "от", "до", "для", "и", "или",
        "тренажере", "тренажер", "положении", "вариант", "упражнение", "подхода", "подходов", "повторений", "раз"
    )

    fun parse(
        rawText: String,
        localExercises: List<ExerciseEntity>,
        catalogExercises: List<ExerciseEntity>
    ): ParsedProgramPlan {
        val cleaned = rawText
            .replace("```json", "")
            .replace("```text", "")
            .replace("```markdown", "")
            .replace("```", "")
            .trim()

        if (cleaned.isEmpty()) {
            return ParsedProgramPlan(
                title = "Импортированная программа",
                description = "",
                days = emptyList()
            )
        }

        // 1. Попытка распарсить JSON, если LLM вернула JSON-объект
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            parseJsonPlan(cleaned, localExercises, catalogExercises)?.let { return it }
        }

        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var programTitle = ""
        var programDescription = ""
        val days = mutableListOf<ParsedDayPlan>()
        var currentDayName = ""
        var currentDayExercises = mutableListOf<ParsedExerciseItem>()

        fun flushDay() {
            if (currentDayExercises.isNotEmpty()) {
                val finalDayName = currentDayName.ifBlank { "День ${days.size + 1}" }
                days.add(ParsedDayPlan(name = finalDayName, exercises = currentDayExercises.toList()))
                currentDayExercises = mutableListOf()
            }
        }

        for (line in lines) {
            val cleanLine = line.replace(Regex("^[*#_~>]+\\s*"), "").replace(Regex("[*_~]+$"), "").trim()
            if (cleanLine.isEmpty()) continue

            // Проверка заголовка ПРОГРАММА:
            val progMatch = Regex("^(?:программа|название программы|план тренировок|program|title)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
                .find(cleanLine)
            if (progMatch != null) {
                programTitle = progMatch.groupValues[1].trim().trim('"', '«', '»', '*')
                continue
            }

            // Проверка заголовка ОПИСАНИЕ:
            val descMatch = Regex("^(?:описание|цель|сплит|description|goal)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)
                .find(cleanLine)
            if (descMatch != null) {
                programDescription = descMatch.groupValues[1].trim().trim('"', '«', '»', '*')
                continue
            }

            // Проверка заголовка ДНЯ (шаблонного или свободного формата)
            val dayHeader = extractDayHeader(cleanLine)
            if (dayHeader != null) {
                flushDay()
                currentDayName = dayHeader
                continue
            }

            // Если это первая строка с # Заголовком и название программы ещё не задано
            if (line.startsWith("#") && programTitle.isEmpty() && days.isEmpty() && currentDayExercises.isEmpty() && !looksLikeExerciseLine(cleanLine)) {
                programTitle = cleanLine
                continue
            }

            // Попытка распарсить строку как упражнение
            val parsedExercise = parseExerciseLine(line, localExercises, catalogExercises)
            if (parsedExercise != null) {
                currentDayExercises.add(parsedExercise)
            }
        }

        flushDay()

        return ParsedProgramPlan(
            title = programTitle.ifBlank { "Программа из LLM" },
            description = programDescription,
            days = days
        )
    }

    private fun parseJsonPlan(
        jsonStr: String,
        localExercises: List<ExerciseEntity>,
        catalogExercises: List<ExerciseEntity>
    ): ParsedProgramPlan? {
        return try {
            val obj = JSONObject(jsonStr)
            val title = obj.optString("title", obj.optString("program", "Программа из LLM"))
            val desc = obj.optString("description", "")
            val daysArr = obj.optJSONArray("days") ?: JSONArray()
            val parsedDays = mutableListOf<ParsedDayPlan>()

            for (i in 0 until daysArr.length()) {
                val dObj = daysArr.getJSONObject(i)
                val dayName = dObj.optString("name", dObj.optString("day", "День ${i + 1}"))
                val exArr = dObj.optJSONArray("exercises") ?: JSONArray()
                val parsedExList = mutableListOf<ParsedExerciseItem>()

                for (j in 0 until exArr.length()) {
                    val exObj = exArr.getJSONObject(j)
                    val rawName = exObj.optString("name", exObj.optString("exercise", "")).trim()
                    if (rawName.isNotEmpty()) {
                        val sets = exObj.optInt("sets", exObj.optInt("targetSets", 3)).coerceIn(1, 20)
                        val reps = exObj.optString("reps", exObj.optString("targetReps", "8-12")).ifBlank { "8-12" }
                        val weightVal = exObj.optDouble("weight", exObj.optDouble("targetWeightKg", 0.0))
                        val weightKg = if (!weightVal.isNaN() && weightVal > 0.0) weightVal.toFloat() else null
                        val superset = exObj.optString("superset", exObj.optString("supersetLabel", "")).trim().uppercase().takeIf { it.isNotEmpty() }

                        parsedExList.add(
                            buildMatchedExerciseItem(
                                rawName = rawName,
                                sets = sets,
                                reps = reps,
                                weightKg = weightKg,
                                supersetLabel = superset,
                                localExercises = localExercises,
                                catalogExercises = catalogExercises
                            )
                        )
                    }
                }
                if (parsedExList.isNotEmpty()) {
                    parsedDays.add(ParsedDayPlan(name = dayName, exercises = parsedExList))
                }
            }
            if (parsedDays.isEmpty()) null else ParsedProgramPlan(title = title, description = desc, days = parsedDays)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDayHeader(cleanLine: String): String? {
        // 1. Явный префикс "ДЕНЬ:"
        val explicitMatch = Regex("^(?:день|тренировка|day|workout)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE).find(cleanLine)
        if (explicitMatch != null) {
            return explicitMatch.groupValues[1].trim().trim('*', '#', '-', '—', ':')
        }

        // Исключаем строки с разделителем шаблона упражнений "|"
        if (cleanLine.contains("|") && Regex("\\d+\\s*[xх×*]\\s*\\d+", RegexOption.IGNORE_CASE).containsMatchIn(cleanLine)) {
            return null
        }

        // 2. Свободный формат: "День 1: Грудь и трицепс", "Тренировка А — Верх тела", "Понедельник (Грудь)"
        val freeDayRegex = Regex(
            "^(?:(?:день|тренировка|day|workout)\\s*(?:№?\\s*\\d+|[A-ZА-ЯЁ])|(?:понедельник|вторник|среда|четверг|пятница|суббота|воскресенье))(?:\\s*[:\\-—–(].*)?$",
            RegexOption.IGNORE_CASE
        )
        if (freeDayRegex.matches(cleanLine) && cleanLine.length <= 75) {
            return cleanLine.trim('*', '#', ':').trim()
        }

        return null
    }

    private fun looksLikeExerciseLine(line: String): Boolean {
        if (line.contains("|")) return true
        if (Regex("\\d+\\s*[xх×*]\\s*\\d+", RegexOption.IGNORE_CASE).containsMatchIn(line)) return true
        if (Regex("\\d+\\s*(?:подх|сет|set)", RegexOption.IGNORE_CASE).containsMatchIn(line)) return true
        return false
    }

    private fun parseExerciseLine(
        rawLine: String,
        localExercises: List<ExerciseEntity>,
        catalogExercises: List<ExerciseEntity>
    ): ParsedExerciseItem? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return null

        // Игнорируем разделители таблиц Markdown типа |---|---|
        if (Regex("^[|\\s\\-:]+$").matches(trimmed)) return null
        // Игнорируем заголовки таблиц
        if (trimmed.contains("Упражнение", ignoreCase = true) && trimmed.contains("Подход", ignoreCase = true)) return null
        // Игнорируем вводные фразы LLM
        if (trimmed.startsWith("Вот ", ignoreCase = true) ||
            trimmed.startsWith("Конечно", ignoreCase = true) ||
            trimmed.startsWith("Пример:", ignoreCase = true) ||
            trimmed.startsWith("Примечание:", ignoreCase = true) ||
            trimmed.startsWith("Отдых между", ignoreCase = true) ||
            trimmed.startsWith("Разминка:", ignoreCase = true) ||
            trimmed.startsWith("Заминка:", ignoreCase = true)
        ) return null

        // Вариант А: Строка с разделителями "|" (по нашему шаблону или таблица Markdown)
        if (trimmed.contains("|")) {
            val parts = trimmed.trim('|').split("|").map { it.trim() }
            if (parts.size >= 2) {
                val rawName = cleanExerciseName(parts[0])
                if (rawName.length >= 3) {
                    var sets = 3
                    var reps = "8-12"
                    var weightKg: Float? = null
                    var supersetLabel: String? = null

                    // Проверяем второй столбец: может быть "4x8-12" или просто "4" (если таблица | Упр | Подходы | Повторения |)
                    val col1 = parts[1]
                    val sxr = parseSetsAndReps(col1)
                    if (sxr != null) {
                        sets = sxr.first
                        reps = sxr.second
                        if (parts.size >= 3) {
                            weightKg = parseWeight(parts[2])
                        }
                        if (parts.size >= 4) {
                            supersetLabel = parseSuperset(parts[3])
                        }
                    } else {
                        val sInt = Regex("(\\d+)").find(col1)?.groupValues?.get(1)?.toIntOrNull()
                        if (sInt != null && parts.size >= 3) {
                            sets = sInt.coerceIn(1, 20)
                            reps = cleanRepsString(parts[2])
                            if (parts.size >= 4) weightKg = parseWeight(parts[3])
                            if (parts.size >= 5) supersetLabel = parseSuperset(parts[4])
                        }
                    }

                    return buildMatchedExerciseItem(
                        rawName = rawName,
                        sets = sets,
                        reps = reps,
                        weightKg = weightKg,
                        supersetLabel = supersetLabel,
                        localExercises = localExercises,
                        catalogExercises = catalogExercises
                    )
                }
            }
        }

        // Вариант Б: Свободный текстовый список (например: "1. Жим штанги лёжа — 4x8-10 (70 кг)" или "- Подтягивания: 4 подхода по 10-12 повт.")
        val isBulletOrNumbered = Regex("^(?:[-•*—–]|\\d+[.)]|\\[?[A-ZА-ЯЁ]\\d\\]?)\\s+.+").matches(trimmed)
        val hasSetsReps = looksLikeExerciseLine(trimmed)

        if (!isBulletOrNumbered && !hasSetsReps) return null

        var working = trimmed
        var supersetLabel: String? = null

        // Проверяем префикс суперсета типа "A1. ", "[A1]", "(Суперсет A)"
        val prefixSuperset = Regex("^\\[?([A-ZА-ЯЁ])\\d\\]?[.):\\s-]+(.+)$").find(working)
        if (prefixSuperset != null) {
            supersetLabel = normalizeSupersetLetter(prefixSuperset.groupValues[1])
            working = prefixSuperset.groupValues[2].trim()
        }

        val inlineSuperset = Regex("(?:суперсет|superset|связка)\\s*([A-ZА-ЯЁ1-9])?", RegexOption.IGNORE_CASE).find(working)
        if (inlineSuperset != null) {
            val rawLetter = inlineSuperset.groupValues.getOrNull(1).orEmpty()
            supersetLabel = if (rawLetter.isNotEmpty()) normalizeSupersetLetter(rawLetter) else "A"
            working = working.replace(inlineSuperset.value, "").trim()
        }

        // Извлекаем вес, если указан ("70 кг", "22.5 kg")
        val weightRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:кг|kg|lbs|фунт)\\b", RegexOption.IGNORE_CASE)
        val weightMatch = weightRegex.find(working)
        val weightKg = weightMatch?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull()?.takeIf { it > 0f }
        if (weightMatch != null) {
            working = working.replace(weightMatch.value, "").trim()
        }

        // Извлекаем подходы и повторения
        var sets = 3
        var reps = "8-12"
        var namePart = working

        // Шаблон 1: "4 x 8-12", "4х10", "3*12-15", "4×10-12"
        val compactSxr = Regex("(\\d+)\\s*[xх×*]\\s*(\\d+(?:\\s*[-–—]\\s*\\d+)?|до отказа|макс|max)", RegexOption.IGNORE_CASE).find(working)
        if (compactSxr != null) {
            sets = compactSxr.groupValues[1].toIntOrNull()?.coerceIn(1, 20) ?: 3
            reps = compactSxr.groupValues[2].replace(" ", "")
            namePart = working.substring(0, compactSxr.range.first)
        } else {
            // Шаблон 2: "4 подхода по 8-12 повторений", "3 сета на 15 раз"
            val verboseSxr = Regex(
                "(\\d+)(?:\\s*[-–—]\\s*\\d+)?\\s*(?:подхода|подходов|подход|подх\\.?|сета|сетов|сет|sets?)\\s*(?:по|на|x|х|×)?\\s*(\\d+(?:\\s*[-–—]\\s*\\d+)?|до отказа|макс)?",
                RegexOption.IGNORE_CASE
            ).find(working)
            if (verboseSxr != null) {
                sets = verboseSxr.groupValues[1].toIntOrNull()?.coerceIn(1, 20) ?: 3
                val r = verboseSxr.groupValues.getOrNull(2)?.trim().orEmpty()
                if (r.isNotEmpty()) {
                    reps = r.replace(" ", "")
                }
                namePart = working.substring(0, verboseSxr.range.first)
            }
        }

        val cleanedName = cleanExerciseName(namePart)
        if (cleanedName.length < 3) return null

        return buildMatchedExerciseItem(
            rawName = cleanedName,
            sets = sets,
            reps = reps,
            weightKg = weightKg,
            supersetLabel = supersetLabel,
            localExercises = localExercises,
            catalogExercises = catalogExercises
        )
    }

    private fun parseSetsAndReps(input: String): Pair<Int, String>? {
        val m = Regex("(\\d+)\\s*[xх×*по]+\\s*(\\d+(?:\\s*[-–—]\\s*\\d+)?|до отказа|макс|max)", RegexOption.IGNORE_CASE).find(input)
        if (m != null) {
            val s = m.groupValues[1].toIntOrNull()?.coerceIn(1, 20) ?: 3
            val r = m.groupValues[2].replace(" ", "")
            return s to r
        }
        val verbose = Regex("(\\d+)\\s*(?:подх|сет).*?(\\d+(?:\\s*[-–—]\\s*\\d+)?)", RegexOption.IGNORE_CASE).find(input)
        if (verbose != null) {
            val s = verbose.groupValues[1].toIntOrNull()?.coerceIn(1, 20) ?: 3
            val r = verbose.groupValues[2].replace(" ", "")
            return s to r
        }
        return null
    }

    private fun cleanRepsString(raw: String): String {
        val m = Regex("(\\d+(?:\\s*[-–—]\\s*\\d+)?|до отказа|макс)", RegexOption.IGNORE_CASE).find(raw)
        return m?.groupValues?.get(1)?.replace(" ", "") ?: "8-12"
    }

    private fun parseWeight(raw: String): Float? {
        val m = Regex("(\\d+(?:[.,]\\d+)?)").find(raw) ?: return null
        val v = m.groupValues[1].replace(',', '.').toFloatOrNull() ?: return null
        return if (v > 0f) v else null
    }

    private fun parseSuperset(raw: String): String? {
        val cleaned = raw.replace(Regex("(?i)суперсет|superset|связка"), "").trim()
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "—" || cleaned == "0" || cleaned.equals("нет", ignoreCase = true)) {
            return null
        }
        return normalizeSupersetLetter(cleaned.first().toString())
    }

    private fun normalizeSupersetLetter(ch: String): String {
        return when (ch.uppercase()) {
            "А", "A", "1" -> "A"
            "Б", "В", "B", "2" -> "B"
            "С", "C", "3" -> "C"
            "Д", "D", "4" -> "D"
            else -> ch.uppercase().take(1)
        }
    }

    private fun cleanExerciseName(raw: String): String {
        return raw
            .replace(Regex("^\\s*(?:[-•*—–]+|\\d+[.)])\\s*"), "")
            .replace(Regex("[*_~`]+"), "")
            .replace(Regex("\\(\\s*\\)"), "")
            .replace(Regex("[:\\-—–•]+\\s*$"), "")
            .trim()
    }

    fun buildMatchedExerciseItem(
        rawName: String,
        sets: Int,
        reps: String,
        weightKg: Float?,
        supersetLabel: String?,
        localExercises: List<ExerciseEntity>,
        catalogExercises: List<ExerciseEntity>
    ): ParsedExerciseItem {
        val (matched, source) = findBestExerciseMatch(rawName, localExercises, catalogExercises)
        val guessedGroup = matched?.muscleGroup ?: guessMuscleGroup(rawName)
        return ParsedExerciseItem(
            rawName = rawName,
            matchedExercise = matched,
            matchSource = source,
            customName = rawName,
            customMuscleGroup = guessedGroup,
            targetSets = sets,
            targetReps = reps,
            targetWeightKg = weightKg,
            supersetLabel = supersetLabel
        )
    }

    fun findBestExerciseMatch(
        queryName: String,
        localExercises: List<ExerciseEntity>,
        catalogExercises: List<ExerciseEntity>
    ): Pair<ExerciseEntity?, ExerciseMatchSource> {
        val normQuery = normalizeText(queryName)
        if (normQuery.isEmpty()) return null to ExerciseMatchSource.NEW_CUSTOM

        // 1. Точное совпадение в локальной БД
        localExercises.firstOrNull { normalizeText(it.name) == normQuery || (it.nameEn.isNotBlank() && normalizeText(it.nameEn) == normQuery) }
            ?.let { return it to ExerciseMatchSource.LOCAL_DB }

        // 2. Точное совпадение в онлайн-каталоге (970+ упражнений)
        catalogExercises.firstOrNull { normalizeText(it.name) == normQuery || (it.nameEn.isNotBlank() && normalizeText(it.nameEn) == normQuery) }
            ?.let { return it to ExerciseMatchSource.ONLINE_CATALOG }

        val queryTokens = tokenize(normQuery)
        val queryMuscleHint = guessMuscleGroupOrNull(normQuery)

        var bestLocal: ExerciseEntity? = null
        var bestLocalScore = 0.0

        for (ex in localExercises) {
            val score = computeSimilarity(normQuery, queryTokens, queryMuscleHint, ex)
            if (score > bestLocalScore) {
                bestLocalScore = score
                bestLocal = ex
            }
        }

        var bestCatalog: ExerciseEntity? = null
        var bestCatalogScore = 0.0

        for (ex in catalogExercises) {
            val score = computeSimilarity(normQuery, queryTokens, queryMuscleHint, ex)
            if (score > bestCatalogScore) {
                bestCatalogScore = score
                bestCatalog = ex
            }
        }

        // Отдаём небольшой приоритет уже существующим в локальной базе упражнениям (чтобы сохранялась история весов)
        if (bestLocal != null && bestLocalScore >= 0.50 && bestLocalScore + 0.06 >= bestCatalogScore) {
            return bestLocal to ExerciseMatchSource.LOCAL_DB
        }
        if (bestCatalog != null && bestCatalogScore >= 0.50) {
            return bestCatalog to ExerciseMatchSource.ONLINE_CATALOG
        }

        return null to ExerciseMatchSource.NEW_CUSTOM
    }

    private fun computeSimilarity(
        normQuery: String,
        queryTokens: List<String>,
        queryMuscleHint: MuscleGroup?,
        candidate: ExerciseEntity
    ): Double {
        val normCand = normalizeText(candidate.name)
        if (normCand.isEmpty()) return 0.0

        if (normCand == normQuery) return 1.0

        val candTokens = tokenize(normCand)
        if (queryTokens.isEmpty() || candTokens.isEmpty()) return 0.0

        var matchedQueryTokens = 0.0
        for (qt in queryTokens) {
            val bestTokenSim = candTokens.maxOfOrNull { ct -> tokenSimilarity(qt, ct) } ?: 0.0
            matchedQueryTokens += bestTokenSim
        }

        var matchedCandTokens = 0.0
        for (ct in candTokens) {
            val bestTokenSim = queryTokens.maxOfOrNull { qt -> tokenSimilarity(qt, ct) } ?: 0.0
            matchedCandTokens += bestTokenSim
        }

        val recall = matchedQueryTokens / queryTokens.size.toDouble()
        val precision = matchedCandTokens / candTokens.size.toDouble()
        var score = (recall * 0.65) + (precision * 0.35)

        // Бонус за вхождение подстроки целиком
        if (normCand.contains(normQuery) || normQuery.contains(normCand)) {
            score += 0.15
        }

        // Бонус/штраф за совпадение главного движения (жим, тяга, присед, сгибание, разгибание, махи, выпады)
        val movementWords = listOf("жим", "тяг", "присед", "сгиб", "разгиб", "мах", "развод", "свед", "выпад", "подтяг", "отжим", "скруч", "планк", "подъем", "шраг")
        for (mw in movementWords) {
            val qHas = normQuery.contains(mw)
            val cHas = normCand.contains(mw)
            if (qHas != cHas && (qHas || cHas)) {
                score -= 0.20
            }
        }

        if (queryMuscleHint != null && candidate.muscleGroup != queryMuscleHint) {
            score -= 0.12
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun tokenSimilarity(t1: String, t2: String): Double {
        if (t1 == t2) return 1.0
        val minLen = minOf(t1.length, t2.length)
        val maxLen = maxOf(t1.length, t2.length)
        if (minLen < 3) return 0.0
        // Общий префикс (учёт русских окончаний: "гантелями" vs "гантелей", "наклонной" vs "наклонная")
        var prefix = 0
        while (prefix < minLen && t1[prefix] == t2[prefix]) {
            prefix++
        }
        if (prefix >= 4 && prefix.toDouble() / maxLen >= 0.55) {
            return 0.90
        }
        if (prefix >= 3 && minLen <= 5 && prefix == minLen) {
            return 0.80
        }
        return 0.0
    }

    private fun normalizeText(s: String): String {
        return s.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenize(norm: String): List<String> {
        return norm.split(" ")
            .map { it.trim() }
            .filter { it.length >= 2 && it !in STOP_WORDS }
    }

    fun guessMuscleGroup(name: String): MuscleGroup {
        return guessMuscleGroupOrNull(normalizeText(name)) ?: MuscleGroup.CHEST
    }

    private fun guessMuscleGroupOrNull(norm: String): MuscleGroup? {
        return when {
            norm.contains("присед") || norm.contains("выпад") || norm.contains("ногами") ||
                norm.contains("ног ") || norm.endsWith(" ног") || norm.contains("квадрицепс") ||
                norm.contains("бедр") || norm.contains("ягодиц") || norm.contains("икр") ||
                norm.contains("голен") || norm.contains("румынск") || norm.contains("гакк") ||
                norm.contains("болгарск") -> MuscleGroup.LEGS

            norm.contains("подтяг") || norm.contains("тяга верхнего") || norm.contains("тяга горизонтального") ||
                norm.contains("тяга штанги в наклоне") || norm.contains("тяга гантели") ||
                norm.contains("тяга т грифа") || norm.contains("становая") || norm.contains("гиперэкстенз") ||
                norm.contains("широчайш") || norm.contains("спин") || norm.contains("пуловер в блоке") -> MuscleGroup.BACK

            norm.contains("бицепс") || norm.contains("трицепс") || norm.contains("молот") ||
                norm.contains("французск") || norm.contains("пюпитр") || norm.contains("скотт") ||
                norm.contains("сгибание рук") || norm.contains("сгибания рук") ||
                norm.contains("разгибание рук") || norm.contains("разгибания на") ||
                norm.contains("узким хватом") || norm.contains("предплеч") -> MuscleGroup.ARMS

            norm.contains("плеч") || norm.contains("дельт") || norm.contains("махи") ||
                norm.contains("армейск") || norm.contains("жим сидя") || norm.contains("жим стоя") ||
                norm.contains("протяжк") || norm.contains("шраг") || norm.contains("тяга к лицу") ||
                norm.contains("разведение гантелей в стороны") -> MuscleGroup.SHOULDERS

            norm.contains("пресс") || norm.contains("скручиван") || norm.contains("планк") ||
                norm.contains("подъем ног в висе") || norm.contains("подъемы ног") ||
                norm.contains("молитва") || norm.contains("ролик") || norm.contains("вакуум") -> MuscleGroup.ABS

            norm.contains("жим") || norm.contains("груд") || norm.contains("разводк") ||
                norm.contains("сведени") || norm.contains("бабочк") || norm.contains("пек дек") ||
                norm.contains("кроссовер") || norm.contains("отжиман") -> MuscleGroup.CHEST

            else -> null
        }
    }
}
