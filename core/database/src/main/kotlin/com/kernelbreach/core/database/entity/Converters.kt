package com.kernelbreach.core.database.entity

import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** A lesson section, stored as JSON inside [LessonEntity]. */
@Serializable
data class SectionRecord(val h: String, val p: List<String>)

/** A lab step, stored as JSON inside [LabEntity]. */
@Serializable
data class StepRecord(val instruction: String, val output: String, val expect: String? = null)

/** A lesson's glossary term, stored as JSON inside [LessonEntity]. */
@Serializable
data class TermRecord(val term: String, val def: String)

/** Room type converters for the small structured columns. */
class Converters {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @TypeConverter
    fun stringListToJson(value: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        json.decodeFromString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun intListToJson(value: List<Int>): String =
        json.encodeToString(ListSerializer(Int.serializer()), value)

    @TypeConverter
    fun jsonToIntList(value: String): List<Int> =
        json.decodeFromString(ListSerializer(Int.serializer()), value)

    @TypeConverter
    fun sectionsToJson(value: List<SectionRecord>): String =
        json.encodeToString(ListSerializer(SectionRecord.serializer()), value)

    @TypeConverter
    fun jsonToSections(value: String): List<SectionRecord> =
        json.decodeFromString(ListSerializer(SectionRecord.serializer()), value)

    @TypeConverter
    fun stepsToJson(value: List<StepRecord>): String =
        json.encodeToString(ListSerializer(StepRecord.serializer()), value)

    @TypeConverter
    fun jsonToSteps(value: String): List<StepRecord> =
        json.decodeFromString(ListSerializer(StepRecord.serializer()), value)

    @TypeConverter
    fun termsToJson(value: List<TermRecord>): String =
        json.encodeToString(ListSerializer(TermRecord.serializer()), value)

    @TypeConverter
    fun jsonToTerms(value: String): List<TermRecord> =
        json.decodeFromString(ListSerializer(TermRecord.serializer()), value)
}
