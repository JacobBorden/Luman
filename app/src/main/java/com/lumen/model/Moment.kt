package com.lumen.model

import java.time.Instant

/**
 * Represents a captured memory moment in the Lumen journal.
 */
data class Moment(
    val id: String,
    val imageUrl: String,
    val tone: String,
    val caption: String?,
    val createdAt: Instant,
    val accentColor: Long
)
