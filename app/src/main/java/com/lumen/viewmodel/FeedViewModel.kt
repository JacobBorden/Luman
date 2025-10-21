package com.lumen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumen.data.MomentRepository
import com.lumen.model.Moment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: MomentRepository
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<FeedUiState> = combine(
        repository.observeMoments(),
        refreshing
    ) { moments, isRefreshing ->
        FeedUiState(moments = moments, isRefreshing = isRefreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState()
    )

    fun refresh() {
        viewModelScope.launch {
            refreshing.emit(true)
            delay(450)
            refreshing.emit(false)
        }
    }
}

data class FeedUiState(
    val moments: List<Moment> = emptyList(),
    val isRefreshing: Boolean = false
)
