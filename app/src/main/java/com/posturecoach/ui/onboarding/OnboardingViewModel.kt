package com.posturecoach.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val workScheduler: WorkScheduler,
) : ViewModel() {

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingComplete(true)
            workScheduler.scheduleAll()
            onDone()
        }
    }
}
