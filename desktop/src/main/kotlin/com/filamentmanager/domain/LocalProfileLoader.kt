package com.filamentmanager.domain

import com.filamentmanager.domain.model.FilamentProfile
import com.filamentmanager.domain.model.ProfileSource
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.Files

object LocalProfileLoader {
    private val gson = Gson()

    fun listUserProfiles(folder: String): List<FilamentProfile> {
        val dir = File(folder)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }?.mapNotNull { file ->
            try {
                val json = file.readText()
                val obj = gson.fromJson(json, JsonObject::class.java)
                parseToProfile(file.name, json, obj, ProfileSource.USER)
            } catch (e: Exception) { null }
        } ?: emptyList()
    }

    fun listSystemProfiles(systemFolder: String): List<FilamentProfile> {
        val dir = File(systemFolder)
        if (!dir.exists()) return emptyList()
        val profiles = mutableListOf<FilamentProfile>()
        dir.walk().filter { it.isFile && it.extension == "json" && (it.name.contains("@base", ignoreCase = true) || it.name.contains("@acbase", ignoreCase = true)) }
            .forEach { file ->
                try {
                    val json = file.readText()
                    val obj = gson.fromJson(json, JsonObject::class.java)
                    profiles.add(parseToProfile(file.name, json, obj, ProfileSource.SYSTEM))
                } catch (e: Exception) {}
            }
        return profiles.sortedBy { it.fileName }
    }

    private fun parseToProfile(fileName: String, raw: String, obj: JsonObject, source: ProfileSource): FilamentProfile {
        fun getStr(key: String) = if (obj.has(key) && !obj.get(key).isJsonNull) obj.get(key).asString else null
        fun getArrStr(key: String): List<String> {
            if (!obj.has(key)) return emptyList()
            val el = obj.get(key)
            return if (el.isJsonArray) el.asJsonArray.map { it.asString } else listOf(el.asString)
        }
        val name = getStr("name") ?: fileName.removeSuffix(".json")
        return FilamentProfile(
            fileName = fileName,
            settingId = getStr("setting_id") ?: getArrStr("filament_settings_id").firstOrNull(),
            filamentId = getStr("filament_id"),
            name = name,
            inherits = getStr("inherits"),
            source = source,
            version = getStr("version"),
            vendor = getStr("filament_vendor"),
            materialType = getStr("filament_type"),
            color = getStr("default_filament_colour"),
            nozzleTempC = getStr("nozzle_temperature")?.toIntOrNull(),
            nozzleTempRangeHigh = getStr("nozzle_temperature_range_high")?.toIntOrNull(),
            nozzleTempRangeLow = getStr("nozzle_temperature_range_low")?.toIntOrNull(),
            nozzleTempInitialC = getStr("nozzle_temperature_initial_layer")?.toIntOrNull(),
            bedTempC = getStr("hot_plate_temp")?.toIntOrNull() ?: getStr("textured_plate_temp")?.toIntOrNull(),
            bedTempInitialC = getStr("hot_plate_temp_initial_layer")?.toIntOrNull(),
            fanMaxSpeed = getStr("fan_max_speed")?.toIntOrNull(),
            fanMinSpeed = getStr("fan_min_speed")?.toIntOrNull(),
            fanCoolingLayerTime = getStr("fan_cooling_layer_time")?.toIntOrNull(),
            flowRatio = getStr("filament_flow_ratio")?.toFloatOrNull(),
            maxVolumetricSpeed = getStr("filament_max_volumetric_speed")?.toFloatOrNull(),
            compatiblePrinters = getArrStr("compatible_printers"),
            presentIn = listOf(),
            systemSlicerID = "",
            rawProfile = raw,
            lastModifiedMs = Files.getLastModifiedTime(file.toPath()).toMillis()
        )
    }
}