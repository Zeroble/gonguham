package com.gonguham.backend.avatar

data class AvatarSummaryResponse(
    val currentChecks: Int,
    val totalEarnedChecks: Int,
    val level: Int,
    val equipped: EquippedAvatarResponse,
)

data class EquippedAvatarResponse(
    val hair: AvatarSlotResponse?,
    val top: AvatarSlotResponse?,
    val bottom: AvatarSlotResponse?,
)

data class AvatarSlotResponse(
    val itemId: Long,
    val name: String,
)

data class AvatarShopItemResponse(
    val itemId: Long,
    val category: String,
    val rarity: String,
    val name: String,
    val description: String,
    val priceChecks: Int,
    val owned: Boolean,
    val equipped: Boolean,
)

data class EquipAvatarRequest(
    val itemId: Long,
)
