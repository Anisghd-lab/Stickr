package com.stickr.app.feature.packdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.core.database.entity.StickerPackEntity
import com.stickr.app.core.model.WhatsAppStickerValidator
import com.stickr.app.core.util.WhatsAppIntentHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackDetailViewModel @Inject constructor(
    private val repository: StickerPackRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packId: String = savedStateHandle.get<String>("packId") ?: ""

    private val _uiState = MutableStateFlow(PackDetailUiState(packId = packId))
    val uiState: StateFlow<PackDetailUiState> = _uiState.asStateFlow()

    init {
        loadPack()
    }

    private fun loadPack() {
        if (packId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getPackById(packId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { packWithStickers ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            packWithStickers = packWithStickers
                        )
                    }
                }
        }
    }

    fun deleteSticker(stickerId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSticker(stickerId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur de suppression : ${e.localizedMessage}") }
            }
        }
    }

    fun updateStickerEmojis(stickerId: String, newEmojis: String) {
        viewModelScope.launch {
            try {
                repository.updateStickerEmojis(stickerId, newEmojis.trim())
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur de mise à jour des émojis : ${e.localizedMessage}") }
            }
        }
    }

    fun updatePackMetadata(newName: String, newPublisher: String) {
        val currentPack = _uiState.value.packWithStickers?.pack ?: return
        if (newName.isBlank() || newPublisher.isBlank()) return

        viewModelScope.launch {
            try {
                val updated = currentPack.copy(
                    name = newName.trim(),
                    publisher = newPublisher.trim()
                )
                repository.updatePack(updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur de mise à jour : ${e.localizedMessage}") }
            }
        }
    }

    fun exportToWhatsApp(context: Context, onFeedback: (String) -> Unit) {
        val packWithStickers = _uiState.value.packWithStickers ?: return

        if (!packWithStickers.isWhatsAppReady) {
            onFeedback("Un pack doit contenir au moins ${WhatsAppStickerValidator.MIN_STICKERS_PER_PACK} stickers pour WhatsApp.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                // Vérifier ou générer automatiquement l'icône 96x96
                repository.ensureTrayIcon(packId)

                val result = WhatsAppIntentHelper.launchAddToWhatsAppIntent(
                    context = context,
                    packId = packId,
                    packName = packWithStickers.pack.name
                )

                _uiState.update { it.copy(isExporting = false) }
                result.fold(
                    onSuccess = {
                        onFeedback("Ajout à WhatsApp en cours...")
                    },
                    onFailure = { error ->
                        onFeedback("Erreur WhatsApp : ${error.localizedMessage}")
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false) }
                onFeedback("Erreur : ${e.localizedMessage}")
            }
        }
    }
}
