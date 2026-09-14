package com.stickr.app.domain.model

data class StickerPack(
    val id: Long = 0,
    val name: String,
    val author: String,
    val trayImageUri: String,
    val stickerCount: Int = 0,
    val stickers: List<Sticker> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
