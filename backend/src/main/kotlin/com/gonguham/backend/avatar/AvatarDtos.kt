package com.gonguham.backend.avatar

data class AvatarSummaryResponse(
    val currentChecks: Int,
    val totalEarnedChecks: Int,
    val level: Int,
    val appearance: AvatarAppearanceResponse,
    val equipped: EquippedAvatarResponse,
)

data class AvatarAppearanceResponse(
    val bodyAssetKey: String,
    val pupilAssetKey: String,
    val eyebrowAssetKey: String,
    val eyelashAssetKey: String,
    val mouthAssetKey: String,
    val shoesAssetKey: String?,
)

data class EquippedAvatarResponse(
    val hair: AvatarSlotResponse?,
    val top: AvatarSlotResponse?,
    val bottom: AvatarSlotResponse?,
    val shoes: AvatarSlotResponse?,
    val pupil: AvatarSlotResponse?,
    val eyebrow: AvatarSlotResponse?,
    val eyelash: AvatarSlotResponse?,
    val mouth: AvatarSlotResponse?,
)

data class AvatarSlotResponse(
    val itemId: Long,
    val name: String,
    val assetKey: String,
)

data class AvatarShopItemResponse(
    val itemId: Long,
    val category: String,
    val name: String,
    val priceChecks: Int,
    val assetKey: String,
    val owned: Boolean,
    val equipped: Boolean,
)

data class EquipAvatarRequest(
    val itemId: Long,
)

data class SaveAvatarAppearanceRequest(
    val hairItemId: Long?,
    val topItemId: Long?,
    val bottomItemId: Long?,
    val shoesItemId: Long?,
    val pupilItemId: Long?,
    val eyebrowItemId: Long?,
    val eyelashItemId: Long?,
    val mouthItemId: Long?,
    val bodyAssetKey: String,
)
