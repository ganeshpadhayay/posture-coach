package com.posturecoach.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.posturecoach.R
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.domain.model.NudgeFrequency

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val notif by viewModel.notificationsEnabled.collectAsState()
    val freq by viewModel.frequency.collectAsState()
    val threshold by viewModel.thresholdMin.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.notifications_enabled),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(checked = notif, onCheckedChange = viewModel::setNotificationsEnabled)
                }
            }
            Spacer(Modifier.height(16.dp))

            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.frequency), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FrequencyChip(NudgeFrequency.LOW, freq, viewModel::setFrequency, R.string.frequency_low)
                        FrequencyChip(NudgeFrequency.MEDIUM, freq, viewModel::setFrequency, R.string.frequency_medium)
                        FrequencyChip(NudgeFrequency.HIGH, freq, viewModel::setFrequency, R.string.frequency_high)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.sitting_threshold),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(R.string.minutes_value, threshold),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = threshold.toFloat(),
                        onValueChange = { viewModel.setThresholdMin(it.toInt()) },
                        valueRange = SettingsRepository.MIN_THRESHOLD.toFloat()..SettingsRepository.MAX_THRESHOLD.toFloat(),
                        steps = (SettingsRepository.MAX_THRESHOLD - SettingsRepository.MIN_THRESHOLD) / 5 - 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyChip(
    target: NudgeFrequency,
    current: NudgeFrequency,
    onPick: (NudgeFrequency) -> Unit,
    labelRes: Int,
) {
    FilterChip(
        selected = current == target,
        onClick = { onPick(target) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) { Box { content() } }
}
