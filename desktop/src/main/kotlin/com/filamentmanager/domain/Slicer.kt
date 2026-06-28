package com.filamentmanager.domain

data class Slicer(
    val id: String,
    val name: String,
    val folder: String,
    val enabled: Boolean = true
)