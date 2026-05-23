package com.posturecoach.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.domain.model.NudgeFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, true,
    )
    val frequency: StateFlow<NudgeFrequency> = repository.frequency.stateIn(
        viewModelScope, SharingStarted.Eagerly, NudgeFrequency.MEDIUM,
    )
    val thresholdMin: StateFlow<Int> = repository.sittingThresholdMin.stateIn(
        viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_THRESHOLD_MIN,
    )

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(value) }
    }

    fun setFrequency(value: NudgeFrequency) {
        viewModelScope.launch { repository.setFrequency(value) }
    }

    fun setThresholdMin(value: Int) {
        viewModelScope.launch { repository.setSittingThresholdMin(value) }
    }
}
