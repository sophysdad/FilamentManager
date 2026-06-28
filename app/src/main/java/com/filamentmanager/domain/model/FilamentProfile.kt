package com.filamentmanager.domain.model

data class FilamentProfile(
    val fileName: String,
    val settingId: String?,
    val filamentId: String?,
    val name: String,
    val inherits: String?,
    val source: ProfileSource,
    val version: String?,

    val vendor: String?,
    val materialType: String?,
    val color: String?,

    // Temps — nullable when inherited from a parent profile
    val nozzleTempC: Int?,
    val nozzleTempRangeHigh: Int?,
    val nozzleTempRangeLow: Int?,
    val nozzleTempInitialC: Int?,
    val bedTempC: Int?,
    val bedTempInitialC: Int?,

    val fanMaxSpeed: Int?,
    val fanMinSpeed: Int?,
    val fanCoolingLayerTime: Int?,
    val flowRatio: Float?,
    val maxVolumetricSpeed: Float?,

    val compatiblePrinters: List<String>,

    // Which slicer folders contain this profile (slicer IDs from the companion config).
    val presentIn: List<String> = emptyList(),

    // For system profiles only: the slicer whose system folder this was read from.
    val systemSlicerID: String = "",

    // Full JSON preserved for round-trip write-back
    val rawProfile: String,
    val lastModifiedMs: Long,
) {
    val isSystem: Boolean get() = source == ProfileSource.SYSTEM
    val displayNozzleTemp: Int? get() = nozzleTempC ?: nozzleTempRangeHigh
    val displayBedTemp: Int? get() = bedTempC
}

enum class ProfileSource { USER, SYSTEM }