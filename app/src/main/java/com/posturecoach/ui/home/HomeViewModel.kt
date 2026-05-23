package com.posturecoach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posturecoach.data.repository.ActivityRepository
import com.posturecoach.data.repository.ScanRepository
import com.posturecoach.domain.model.PostureScan
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val latestScan: PostureScan? = null,
    val sittingTodayMs: Long = 0,
    val currentStillMs: Long = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {

    val latestScan: StateFlow<PostureScan?> = scanRepository.observeLatest().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    private val _currentStillMs = MutableStateFlow(0L)
    val currentStillMs: StateFlow<Long> = _currentStillMs.asStateFlow()

    private val _sittingTodayMs = MutableStateFlow(0L)
    val sittingTodayMs: StateFlow<Long> = _sittingTodayMs.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _currentStillMs.value = activityRepository.currentStillDurationMs()
                _sittingTodayMs.value = activityRepository.stillDurationTodayMs()
                delay(REFRESH_MS)
            }
        }
    }

    companion object {
        private const val REFRESH_MS = 30_000L
    }
}
