package com.lumen.viewmodel

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreViewModelTest {

    @Test
    fun `default prompts are immediately available`() = runTest {
        val viewModel = ExploreViewModel()

        val prompts = viewModel.prompts.value

        assertEquals(5, prompts.size)
        assertTrue(prompts.contains("A quiet luxury moment"))
    }

    @Test
    fun `shufflePrompts emits a new permutation of the defaults`() = runTest {
        val viewModel = ExploreViewModel()
        val original = viewModel.prompts.value

        viewModel.shufflePrompts()

        val shuffled = viewModel.prompts.value

        assertNotSame(original, shuffled)
        assertEquals(original.toSet(), shuffled.toSet())
    }
}
