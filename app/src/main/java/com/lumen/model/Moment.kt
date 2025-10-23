package com.lumen.model

/**
 * Represents a captured memory moment in the Lumen journal.
 */
data class Moment(
    val id: String,
    val imageUrl: String,
    val tone: String,
    val caption: String?,
    val createdAtMillis: Long,
    val accentColor: Long
)
