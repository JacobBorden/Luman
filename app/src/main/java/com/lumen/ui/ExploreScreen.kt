package com.lumen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumen.viewmodel.ExploreViewModel
import androidx.compose.ui.res.stringResource
import com.lumen.R

@Composable
fun ExploreScreen(viewModel: ExploreViewModel) {
    val prompts by viewModel.prompts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.explore_prompt_header),
            style = MaterialTheme.typography.titleLarge
        )
        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(prompts) { prompt ->
                Surface(shape = MaterialTheme.shapes.medium) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        Button(onClick = viewModel::shufflePrompts) {
            Text(text = stringResource(id = R.string.shuffle_action))
        }
    }
}
