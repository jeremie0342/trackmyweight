package com.kps.trackmyweight.data.db.converters

import androidx.room.TypeConverter
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class Converters {

    // Dates
    @TypeConverter
    fun instantToMillis(v: Instant?): Long? = v?.toEpochMilliseconds()

    @TypeConverter
    fun millisToInstant(v: Long?): Instant? = v?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun localDateToString(v: LocalDate?): String? = v?.toString()

    @TypeConverter
    fun stringToLocalDate(v: String?): LocalDate? = v?.let(LocalDate::parse)

    // JSON collections utilisés par plusieurs entités
    @TypeConverter
    fun muscleGroupListToJson(v: List<MuscleGroup>?): String? =
        v?.let { json.encodeToString(ListSerializer(MuscleGroup.serializer()), it) }

    @TypeConverter
    fun jsonToMuscleGroupList(v: String?): List<MuscleGroup>? =
        v?.let { json.decodeFromString(ListSerializer(MuscleGroup.serializer()), it) }

    @TypeConverter
    fun stringIntMapToJson(v: Map<String, Int>?): String? =
        v?.let { json.encodeToString(MapSerializer(String.serializer(), Int.serializer()), it) }

    @TypeConverter
    fun jsonToStringIntMap(v: String?): Map<String, Int>? =
        v?.let { json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), it) }

    @TypeConverter
    fun stringListToJson(v: List<String>?): String? =
        v?.let { json.encodeToString(ListSerializer(String.serializer()), it) }

    @TypeConverter
    fun jsonToStringList(v: String?): List<String>? =
        v?.let { json.decodeFromString(ListSerializer(String.serializer()), it) }
}
