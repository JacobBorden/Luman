package com.lumen.viewmodel

import com.lumen.data.MomentRepository
import com.lumen.util.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState reflects repository contents`() = runTest(dispatcherRule.testDispatcher) {
        val repository = MomentRepository()
        val viewModel = FeedViewModel(repository)

        val state = viewModel.uiState.first { it.moments.isNotEmpty() }

        assertEquals(3, state.moments.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `refresh toggles loading indicator`() = runTest(dispatcherRule.testDispatcher) {
        val repository = MomentRepository()
        val viewModel = FeedViewModel(repository)
        val emissions = mutableListOf<FeedUiState>()

        val job = launch {
            viewModel.uiState
                .drop(1)
                .take(3)
                .collect(emissions::add)
        }

        advanceUntilIdle()

        viewModel.refresh()

        advanceUntilIdle()
        advanceTimeBy(450)
        advanceUntilIdle()

        job.join()

        assertEquals(3, emissions.size)
        assertTrue(emissions.first().moments.isNotEmpty())
        assertFalse(emissions.first().isRefreshing)
        assertTrue(emissions[1].isRefreshing)
        assertFalse(emissions[2].isRefreshing)
        assertEquals(emissions.first().moments, emissions[1].moments)
        assertEquals(emissions.first().moments, emissions[2].moments)
    }
}
