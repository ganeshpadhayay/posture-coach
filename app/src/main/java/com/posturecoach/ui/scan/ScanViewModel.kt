package com.posturecoach.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posturecoach.domain.usecase.AnalyzePostureUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val analyze: AnalyzePostureUseCase,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Analyzing : State
        data class Success(val scanId: String) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun analyze(imagePath: String) {
        if (_state.value is State.Analyzing) return
        _state.value = State.Analyzing
        viewModelScope.launch {
            when (val result = analyze.invoke(imagePath)) {
                is AnalyzePostureUseCase.Result.Success ->
                    _state.value = State.Success(result.scan.id)
                AnalyzePostureUseCase.Result.NoPoseDetected ->
                    _state.value = State.Error(NO_POSE)
                is AnalyzePostureUseCase.Result.Error ->
                    _state.value = State.Error(result.cause.message ?: GENERIC_ERROR)
            }
        }
    }

    fun reset() {
        _state.value = State.Idle
    }

    companion object {
        const val NO_POSE = "no_pose"
        const val GENERIC_ERROR = "error"
    }
}
