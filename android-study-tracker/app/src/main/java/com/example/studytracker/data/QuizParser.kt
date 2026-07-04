package com.example.studytracker.data

import org.json.JSONArray
import org.json.JSONObject

data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val correct: Int
)

data class ParsedQuiz(
    val title: String?,
    val subjectName: String?,
    val questions: List<QuizQuestion>
)

/**
 * Разбор JSON-файла с тестом. Ожидаемый формат:
 *
 * {
 *   "title": "Модуль 1. Основы",
 *   "subject": "Физика",          // необязательно
 *   "questions": [
 *     {
 *       "text": "Текст вопроса?",
 *       "options": ["Вариант 1", "Вариант 2", "Вариант 3"],
 *       "correct": 0              // индекс правильного варианта, счёт с нуля
 *     }
 *   ]
 * }
 */
object QuizParser {

    /** Бросает IllegalArgumentException с понятным сообщением при неверном формате. */
    fun parse(json: String): ParsedQuiz {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Файл не является корректным JSON-объектом")
        }
        val questionsArray = root.optJSONArray("questions")
            ?: throw IllegalArgumentException("В файле нет массива \"questions\"")
        if (questionsArray.length() == 0) {
            throw IllegalArgumentException("Список вопросов пуст")
        }
        val questions = (0 until questionsArray.length()).map { i ->
            val q = questionsArray.optJSONObject(i)
                ?: throw IllegalArgumentException("Вопрос №${i + 1} должен быть объектом")
            val text = q.optString("text").trim()
            if (text.isEmpty()) {
                throw IllegalArgumentException("У вопроса №${i + 1} нет текста (\"text\")")
            }
            val optionsArray = q.optJSONArray("options")
                ?: throw IllegalArgumentException("У вопроса №${i + 1} нет вариантов (\"options\")")
            val options = (0 until optionsArray.length())
                .map { j -> optionsArray.optString(j).trim() }
                .filter { it.isNotEmpty() }
            if (options.size < 2) {
                throw IllegalArgumentException("У вопроса №${i + 1} должно быть минимум два варианта")
            }
            val correct = q.optInt("correct", -1)
            if (correct !in options.indices) {
                throw IllegalArgumentException(
                    "У вопроса №${i + 1} неверный номер правильного ответа " +
                        "(\"correct\" — индекс варианта, счёт с нуля)"
                )
            }
            QuizQuestion(text, options, correct)
        }
        return ParsedQuiz(
            title = root.optString("title").trim().takeIf { it.isNotEmpty() },
            subjectName = root.optString("subject").trim().takeIf { it.isNotEmpty() },
            questions = questions
        )
    }

    /** Нормализованное хранение вопросов в базе (JSON-массив). */
    fun questionsToJson(questions: List<QuizQuestion>): String {
        val array = JSONArray()
        questions.forEach { q ->
            val obj = JSONObject()
            obj.put("text", q.text)
            obj.put("options", JSONArray(q.options))
            obj.put("correct", q.correct)
            array.put(obj)
        }
        return array.toString()
    }

    fun questionsFromJson(json: String): List<QuizQuestion> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val opts = obj.getJSONArray("options")
            QuizQuestion(
                text = obj.getString("text"),
                options = (0 until opts.length()).map { opts.getString(it) },
                correct = obj.getInt("correct")
            )
        }
    }
}
