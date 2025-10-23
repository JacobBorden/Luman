package com.lumen.viewmodel

import com.lumen.data.MomentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddMomentViewModelTest {

    @Test
    fun `saveMoment commits user input and resets form state`() = runTest {
        val repository = MomentRepository()
        val viewModel = AddMomentViewModel(repository)

        val initialSize = repository.observeMoments().first().size

        viewModel.selectedTone.value = "Forest Quiet"
        viewModel.caption.value = "Unwinding after dusk"

        viewModel.saveMoment()

        val moments = repository.observeMoments().first()
        val saved = moments.first()

        assertEquals(initialSize + 1, moments.size)
        assertEquals("https://images.unsplash.com/photo-1487412947147-5cebf100ffc2", saved.imageUrl)
        assertEquals("Forest Quiet", saved.tone)
        assertEquals("Unwinding after dusk", saved.caption)
        assertEquals("", viewModel.caption.value)
        assertEquals("Golden Hour", viewModel.selectedTone.value)
    }

    @Test
    fun `blank captions are persisted as null`() = runTest {
        val repository = MomentRepository()
        val viewModel = AddMomentViewModel(repository)

        viewModel.caption.value = "   "

        viewModel.saveMoment()

        val saved = repository.observeMoments().first().first()

        assertNull(saved.caption)
        assertEquals("", viewModel.caption.value)
    }
}
