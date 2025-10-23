package com.lumen.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentRepositoryTest {

    @Test
    fun `seed data is available when observing moments`() = runTest {
        val repository = MomentRepository()

        val moments = repository.observeMoments().first()

        assertEquals(3, moments.size)
        assertTrue(moments.all { it.caption != null })
    }

    @Test
    fun `adding a moment prepends a freshly timestamped entry`() = runTest {
        val repository = MomentRepository()
        val before = System.currentTimeMillis()
        val initial = repository.observeMoments().first()

        repository.addMoment(
            imageUrl = "https://example.com/image.jpg",
            tone = "New Tone",
            caption = "A calm memory",
            accentColor = 0xFF000000
        )

        val updated = repository.observeMoments().first()
        val newMoment = updated.first()

        assertEquals(initial.size + 1, updated.size)
        assertEquals("https://example.com/image.jpg", newMoment.imageUrl)
        assertEquals("New Tone", newMoment.tone)
        assertEquals("A calm memory", newMoment.caption)
        assertEquals(0xFF000000, newMoment.accentColor)
        assertTrue(newMoment.createdAtMillis >= before)
        assertNotEquals(initial.first().id, newMoment.id)
    }
}
