package com.filamentmanager.domain

import java.io.File

/**
 * V3 Windows all-in-one scanner.
 * Replicates (in Kotlin) the logic previously in the Go companion-service.
 */
object LocalSlicerScanner {

    private val knownForks = listOf(
        "OrcaSlicer" to "OrcaSlicer/user/default/filament",
        "ElegooSlicer" to "ElegooSlicer/user/default/filament",
        "AnycubicSlicerNext" to "AnycubicSlicerNext/user/default/filament",
        "BambuStudio" to "BambuStudio/user/default/filament",
        // add more as needed
    )

    fun detectSlicers(): List<Slicer> {
        val appData = System.getenv("APPDATA") ?: return emptyList()
        val result = mutableListOf<Slicer>()

        for ((name, relPath) in knownForks) {
            val base = File(appData, name)
            val userDir = File(base, "user")
            if (!userDir.exists()) continue

            // Prefer most recently modified non-default folder (same logic as Go v2)
            val candidates = mutableListOf<File>()
            File(userDir, "default/filament").takeIf { it.exists() }?.let { candidates.add(it) }

            userDir.listFiles { f -> f.isDirectory && f.name != "default" }?.forEach { sub ->
                val cand = File(sub, "filament")
                if (cand.exists()) candidates.add(cand)
            }

            val best = candidates.maxByOrNull { it.lastModified() } ?: continue

            val id = name.lowercase().replace(" ", "").trimEnd { it.isDigit() }
            result += Slicer(id = id, name = name, folder = best.absolutePath)
        }

        return result.sortedByDescending { File(it.folder).lastModified() }
    }
}