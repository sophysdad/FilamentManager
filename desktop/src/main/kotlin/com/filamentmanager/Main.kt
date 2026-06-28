package com.filamentmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.filamentmanager.domain.LocalSlicerScanner
import com.filamentmanager.domain.Slicer

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Filament Manager v3 (Windows - All-in-One)"
    ) {
        MaterialTheme {
            val slicers by remember { mutableStateOf(LocalSlicerScanner.detectSlicers()) }
            var selectedSlicer by remember { mutableStateOf<Slicer?>(null) }

            Row(Modifier.fillMaxSize()) {
                // Left: Slicer list (similar to SlicerSelector)
                Column(Modifier.width(220.dp).fillMaxHeight().padding(12.dp)) {
                    Text("Slicers (local)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (slicers.isEmpty()) {
                        Text("No slicers detected.\n\nMake sure OrcaSlicer/Elegoo/etc. are installed.", color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyColumn {
                            items(slicers) { slicer ->
                                Card(
                                    onClick = { selectedSlicer = slicer },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(slicer.name, style = MaterialTheme.typography.titleSmall)
                                        Text(slicer.folder, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }

                VerticalDivider()

                // Main area (same layout spirit as ProfileList)
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "Filament Manager v3",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("by Moosepond Designs • All-in-one (no companion)", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(16.dp))

                    if (selectedSlicer == null) {
                        Text("Select a slicer on the left to browse its profiles.")
                    } else {
                        Text("Selected: ${selectedSlicer!!.name}", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Profiles would load here using direct file access + OrcaSlicerAdapter (same as v2).")
                        Spacer(Modifier.height(12.dp))

                        // Placeholder for the grouped/brand list experience from v2
                        Card {
                            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                                Text("• User + System profiles (grouped by brand by default)")
                                Text("• Multi-select + bulk copy/sync")
                                Text("• Overwrite confirmation")
                                Text("• Direct write with correct .info for Anycubic")
                                Text("• Same flattening & inheritance handling")
                            }
                        }
                    }
                }
            }
        }
    }
}