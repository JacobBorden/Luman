package com.lumen.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.lumen.data.MomentRepository

class AddMomentViewModel(
    private val repository: MomentRepository
) : ViewModel() {

    val selectedTone = mutableStateOf("Golden Hour")
    val caption = mutableStateOf("")
    val imageUrl = mutableStateOf("https://images.unsplash.com/photo-1487412947147-5cebf100ffc2")

    fun saveMoment() {
        repository.addMoment(
            imageUrl = imageUrl.value,
            tone = selectedTone.value,
            caption = caption.value.takeIf { it.isNotBlank() },
            accentColor = 0xFFC3A35BL
        )
        caption.value = ""
        selectedTone.value = "Golden Hour"
    }
}
