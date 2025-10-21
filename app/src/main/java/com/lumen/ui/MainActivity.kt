package com.lumen.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lumen.data.MomentRepository
import com.lumen.theme.LumenTheme
import com.lumen.viewmodel.AddMomentViewModel
import com.lumen.viewmodel.ExploreViewModel
import com.lumen.viewmodel.FeedViewModel

class MainActivity : ComponentActivity() {

    private val repository by lazy { MomentRepository() }

    private val feedViewModel: FeedViewModel by viewModels { repositoryFactory { FeedViewModel(repository) } }
    private val addMomentViewModel: AddMomentViewModel by viewModels { repositoryFactory { AddMomentViewModel(repository) } }
    private val exploreViewModel: ExploreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LumenTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LumenApp(
                        feedViewModel = feedViewModel,
                        addMomentViewModel = addMomentViewModel,
                        exploreViewModel = exploreViewModel
                    )
                }
            }
        }
    }
}

private fun <T : ViewModel> ComponentActivity.repositoryFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return create() as T
        }
    }
