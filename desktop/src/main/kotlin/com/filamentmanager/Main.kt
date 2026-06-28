package com.filamentmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.filamentmanager.domain.LocalProfileLoader
import com.filamentmanager.domain.LocalSlicerScanner
import com.filamentmanager.domain.Slicer
import com.filamentmanager.domain.model.FilamentProfile

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Filament Manager v3 (Windows - All-in-One) | Moosepond Designs"
    ) {
        MaterialTheme {
            val slicers by remember { mutableStateOf(LocalSlicerScanner.detectSlicers()) }
            var selectedSlicer by remember { mutableStateOf<Slicer?>(null) }
            var profiles by remember { mutableStateOf<List<FilamentProfile>>(emptyList()) }
            var isSelectionMode by remember { mutableStateOf(false) }
            var selectedFileNames by remember { mutableStateOf(setOf<String>()) }
            var showSystem by remember { mutableStateOf(true) }
            var statusMessage by remember { mutableStateOf("") }

            LaunchedEffect(selectedSlicer, showSystem) {
                selectedSlicer?.let { s ->
                    val user = LocalProfileLoader.listUserProfiles(s.folder)
                    val systemDir = s.folder.replace("user/default/filament", "system").replace("user/[^/]+/filament".toRegex(), "system")
                    val sys = if (showSystem) LocalProfileLoader.listSystemProfiles(systemDir) else emptyList()
                    profiles = (user + sys).sortedBy { it.name }
                    profiles = profiles.map { it.copy(presentIn = listOf(s.id)) }
                }
            }

            Column(Modifier.fillMaxSize()) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filament Manager v3", style = MaterialTheme.typography.headlineSmall)
                    Text("by Moosepond Designs • Direct local access", style = MaterialTheme.typography.labelMedium)
                    Row {
                        Button(onClick = { 
                            statusMessage = "Bulk copy using LocalProfileLoader.copyProfile for each selected"
                        }) { Text("Bulk Copy") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { isSelectionMode = !isSelectionMode; selectedFileNames = emptySet() }) {
                            Text(if (isSelectionMode) "Done" else "Select")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { showSystem = !showSystem }) {
                            Text(if (showSystem) "Hide System" else "Show System")
                        }
                    }
                }

                Row(Modifier.fillMaxSize()) {
                    // Slicer sidebar
                    Column(Modifier.width(240.dp).fillMaxHeight().padding(8.dp)) {
                        Text("Detected Slicers", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        if (slicers.isEmpty()) {
                            Text("No slicers found. Install OrcaSlicer etc.", color = MaterialTheme.colorScheme.error)
                        } else {
                            LazyColumn {
                                items(slicers) { s ->
                                    Card(
                                        onClick = { selectedSlicer = s },
                                        modifier = Modifier.fillMaxWidth().padding(2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedSlicer?.id == s.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text(s.name, style = MaterialTheme.typography.titleSmall)
                                            Text(s.folder.take(40) + "...", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerticalDivider()

                    // Profile list area
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        if (selectedSlicer == null) {
                            Text("Select a slicer on the left to browse its profiles (user + system grouped by brand by default, collapsed).")
                        } else {
                            val displayProfiles = if (showSystem) profiles else profiles.filter { !it.isSystem }
                            val grouped = displayProfiles.groupBy { it.vendor ?: "Unknown" }.toSortedMap()

                            Text("${selectedSlicer!!.name} — ${displayProfiles.size} profiles", style = MaterialTheme.typography.titleMedium)
                            if (statusMessage.isNotEmpty()) Text(statusMessage, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))

                            LazyColumn {
                                grouped.forEach { (brand, list) ->
                                    item {
                                        Text(brand, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                    items(list) { p ->
                                        val isSelected = p.fileName in selectedFileNames
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            onClick = {
                                                if (isSelectionMode) {
                                                    selectedFileNames = if (isSelected) selectedFileNames - p.fileName else selectedFileNames + p.fileName
                                                }
                                            }
                                        ) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelectionMode) {
                                                    Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                                        selectedFileNames = if (checked) selectedFileNames + p.fileName else selectedFileNames - p.fileName
                                                    })
                                                }
                                                Column(Modifier.weight(1f)) {
                                                    Text(p.name)
                                                    Text("${if (p.isSystem) "System" else "User"} • ${p.materialType ?: ""}", style = MaterialTheme.typography.bodySmall)
                                                }
                                                if (!isSelectionMode) {
                                                    TextButton(onClick = {
                                                        val target = slicers.firstOrNull { it.id != selectedSlicer!!.id }
                                                        if (target != null) {
                                                            val isAc = target.name.lowercase().contains("anycubic")
                                                            val success = LocalProfileLoader.copyProfile(selectedSlicer!!.folder, target.folder, p.fileName, isAc)
                                                            statusMessage = if (success) "Copied ${p.name} → ${target.name}" else "Copy failed"
                                                            val user = LocalProfileLoader.listUserProfiles(selectedSlicer!!.folder)
                                                            val sysDir = selectedSlicer!!.folder.replace("user/default/filament", "system")
                                                            val sys = LocalProfileLoader.listSystemProfiles(sysDir)
                                                            profiles = (user + sys).sortedBy { it.name }
                                                        }
                                                    }) { Text("Copy") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}