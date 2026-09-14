package com.stickr.app.presentation.screens.packdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.domain.repository.StickerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackDetailViewModel @Inject constructor(
    private val repository: StickerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packId: Long = savedStateHandle.get<Long>("packId") ?: 0L

    private val _uiState = MutableStateFlow(PackDetailUiState())
    val uiState: StateFlow<PackDetailUiState> = _uiState.asStateFlow()

    init {
        loadPackDetails()
    }

    private fun loadPackDetails() {
        if (packId <= 0L) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                repository.getPackById(packId),
                repository.getStickersForPack(packId)
            ) { pack, stickers ->
                PackDetailUiState(
                    isLoading = false,
                    pack = pack,
                    stickers = stickers
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteSticker(stickerId: Long) {
        viewModelScope.launch {
            repository.deleteSticker(stickerId)
        }
    }
}
