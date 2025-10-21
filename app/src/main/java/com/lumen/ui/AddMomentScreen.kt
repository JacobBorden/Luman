package com.lumen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.lumen.viewmodel.AddMomentViewModel
import com.lumen.R

@Composable
fun AddMomentScreen(viewModel: AddMomentViewModel) {
    var tone by viewModel.selectedTone
    var caption by viewModel.caption

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.add_moment_cta),
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = tone,
            onValueChange = { tone = it },
            label = { Text(stringResource(id = R.string.tone_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text(stringResource(id = R.string.caption_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.saveMoment() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.save_moment))
        }
    }
}
