package com.stickr.app.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stickr.app.core.data.repository.StickerPackRepository
import com.stickr.app.core.database.entity.StickerPackWithStickers
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
class DashboardViewModel @Inject constructor(
    private val repository: StickerPackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observePacks()
    }

    private fun observePacks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllPacks()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
                }
                .collect { packsList ->
                    _uiState.update { it.copy(isLoading = false, packs = packsList) }
                }
        }
    }

    fun createNewPack(name: String, publisher: String, onCreated: (String) -> Unit) {
        if (name.isBlank() || publisher.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nom du pack et nom du créateur requis.") }
            return
        }

        viewModelScope.launch {
            try {
                val newPackId = repository.createPack(name.trim(), publisher.trim())
                onCreated(newPackId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Échec de création : ${e.localizedMessage}") }
            }
        }
    }

    fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur de suppression : ${e.localizedMessage}") }
            }
        }
    }

    fun exportToWhatsApp(context: Context, pack: StickerPackWithStickers, onFeedback: (String) -> Unit) {
        if (!pack.isWhatsAppReady) {
            onFeedback("WhatsApp requiert entre ${WhatsAppStickerValidator.MIN_STICKERS_PER_PACK} et ${WhatsAppStickerValidator.MAX_STICKERS_PER_PACK} stickers.")
            return
        }

        viewModelScope.launch {
            // S'assurer que l'icône de plateau 96x96 est bien générée
            repository.ensureTrayIcon(pack.pack.id)

            val result = WhatsAppIntentHelper.launchAddToWhatsAppIntent(
                context = context,
                packId = pack.pack.id,
                packName = pack.pack.name
            )

            result.fold(
                onSuccess = {
                    onFeedback("Ajout à WhatsApp lancé pour \"${pack.pack.name}\" !")
                },
                onFailure = { error ->
                    onFeedback("Erreur WhatsApp : ${error.localizedMessage}")
                }
            )
        }
    }
}
