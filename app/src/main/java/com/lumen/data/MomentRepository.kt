package com.lumen.data

import com.lumen.model.Moment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID

/**
 * Simple in-memory repository to power the MVP prototype.
 */
class MomentRepository {
    private val moments = MutableStateFlow(sampleMoments())

    fun observeMoments(): Flow<List<Moment>> = moments.asStateFlow()

    fun addMoment(
        imageUrl: String,
        tone: String,
        caption: String?,
        accentColor: Long
    ) {
        val newMoment = Moment(
            id = UUID.randomUUID().toString(),
            imageUrl = imageUrl,
            tone = tone,
            caption = caption,
            createdAt = Instant.now(),
            accentColor = accentColor
        )
        moments.update { listOf(newMoment) + it }
    }

    private fun sampleMoments(): List<Moment> = listOf(
        Moment(
            id = UUID.randomUUID().toString(),
            imageUrl = "https://images.unsplash.com/photo-1526481280695-3c46917166ab",
            tone = "Soft Dawn",
            caption = "Slow coffee before sunrise.",
            createdAt = Instant.now().minusSeconds(60 * 60 * 8),
            accentColor = 0xFFC3A35BL
        ),
        Moment(
            id = UUID.randomUUID().toString(),
            imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee",
            tone = "Forest Quiet",
            caption = "Walked until the noise softened.",
            createdAt = Instant.now().minusSeconds(60 * 60 * 24),
            accentColor = 0xFF2F4F4FL
        ),
        Moment(
            id = UUID.randomUUID().toString(),
            imageUrl = "https://images.unsplash.com/photo-1500534623283-312aade485b7",
            tone = "Golden Hour",
            caption = "Laughed until the sky blushed.",
            createdAt = Instant.now().minusSeconds(60 * 60 * 48),
            accentColor = 0xFFC3A35BL
        )
    )
}
