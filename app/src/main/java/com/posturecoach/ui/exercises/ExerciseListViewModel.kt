package com.posturecoach.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posturecoach.data.repository.ExerciseRepository
import com.posturecoach.data.repository.ScanRepository
import com.posturecoach.domain.model.Exercise
import com.posturecoach.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scanRepository: ScanRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val scanId: String = checkNotNull(savedStateHandle[Routes.ARG_SCAN_ID])

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    init {
        viewModelScope.launch {
            val scan = scanRepository.getById(scanId)
            _exercises.value = exerciseRepository.forIssues(scan?.issues ?: emptyList())
        }
    }
}
