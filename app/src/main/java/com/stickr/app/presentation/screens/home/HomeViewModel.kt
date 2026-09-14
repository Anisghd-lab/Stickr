package com.stickr.app.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.domain.usecase.CreateStickerPackUseCase
import com.stickr.app.domain.usecase.GetStickerPacksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getStickerPacksUseCase: GetStickerPacksUseCase,
    private val createStickerPackUseCase: CreateStickerPackUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPacks()
    }

    private fun loadPacks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getStickerPacksUseCase()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { packs ->
                    _uiState.update { it.copy(isLoading = false, stickerPacks = packs) }
                }
        }
    }

    fun createNewPack(name: String, author: String, trayImageUri: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val packId = createStickerPackUseCase(name, author, trayImageUri)
                onCreated(packId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }
}
