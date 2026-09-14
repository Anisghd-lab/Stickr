package com.stickr.app.domain.model

data class Sticker(
    val id: Long = 0,
    val packId: Long,
    val imageUri: String,
    val emojis: List<String> = emptyList(),
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
