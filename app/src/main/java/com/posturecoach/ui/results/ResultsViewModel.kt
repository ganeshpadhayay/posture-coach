package com.posturecoach.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posturecoach.data.repository.ScanRepository
import com.posturecoach.domain.model.PostureScan
import com.posturecoach.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ScanRepository,
) : ViewModel() {

    val scanId: String = checkNotNull(savedStateHandle[Routes.ARG_SCAN_ID])

    private val _scan = MutableStateFlow<PostureScan?>(null)
    val scan: StateFlow<PostureScan?> = _scan.asStateFlow()

    init {
        viewModelScope.launch {
            _scan.value = repository.getById(scanId)
        }
    }
}
