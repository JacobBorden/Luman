package com.lumen.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ExploreViewModel : ViewModel() {

    private val _prompts = MutableStateFlow(defaultPrompts)
    val prompts: StateFlow<List<String>> = _prompts

    fun shufflePrompts() {
        _prompts.value = defaultPrompts.shuffled()
    }

    companion object {
        private val defaultPrompts = listOf(
            "A place that calmed you today",
            "A color that matched your energy",
            "A quiet luxury moment",
            "Something that made you breathe deeper",
            "A texture that felt like home"
        )
    }
}
