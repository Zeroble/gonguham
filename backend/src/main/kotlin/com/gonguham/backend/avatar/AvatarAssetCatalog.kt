package com.gonguham.backend.avatar

import com.gonguham.backend.domain.AvatarCategory
import com.gonguham.backend.domain.AvatarRarity

data class AvatarFreeAppearance(
    val bodyAssetKey: String,
    val pupilAssetKey: String,
    val eyebrowAssetKey: String,
    val eyelashAssetKey: String,
    val mouthAssetKey: String,
)

object AvatarAssetCatalog {
    private val hairColorSpecs = listOf(
        HairColorSpec("a", "진갈색"),
        HairColorSpec("b", "생강색"),
        HairColorSpec("c", "레드"),
        HairColorSpec("d", "연갈색"),
        HairColorSpec("e", "블론드"),
        HairColorSpec("f", "블랙"),
        HairColorSpec("g", "화이트"),
        HairColorSpec("h", "그레이"),
    )

    val bodyAssetKeys = (1..29).map { "body-${it.toAssetNo()}" }.toSet()
    val pupilAssetKeys = (1..16).map { "pupil-${it.toAssetNo()}" }.toSet()
    val eyebrowAssetKeys = (1..5).map { "eyebrow-${it.toAssetNo()}" }.toSet()
    val eyelashAssetKeys = (1..5).map { "eyelash-${it.toAssetNo()}" }.toSet()
    val mouthAssetKeys = (1..20).map { "mouth-${it.toAssetNo()}" }.toSet()

    const val DEFAULT_BODY_ASSET_KEY = "body-01"
    const val DEFAULT_PUPIL_ASSET_KEY = "pupil-01"
    const val DEFAULT_EYEBROW_ASSET_KEY = "eyebrow-01"
    const val DEFAULT_EYELASH_ASSET_KEY = "eyelash-01"
    const val DEFAULT_MOUTH_ASSET_KEY = "mouth-01"
    const val DEFAULT_HAIR_ITEM_KEY = "hair-01-f"
    const val DEFAULT_TOP_ITEM_KEY = "top-01"
    const val DEFAULT_BOTTOM_ITEM_KEY = "bottom-01"

    fun defaultAppearance() = AvatarFreeAppearance(
        bodyAssetKey = DEFAULT_BODY_ASSET_KEY,
        pupilAssetKey = DEFAULT_PUPIL_ASSET_KEY,
        eyebrowAssetKey = DEFAULT_EYEBROW_ASSET_KEY,
        eyelashAssetKey = DEFAULT_EYELASH_ASSET_KEY,
        mouthAssetKey = DEFAULT_MOUTH_ASSET_KEY,
    )

    fun isValidBodyAssetKey(assetKey: String) = bodyAssetKeys.contains(assetKey)

    fun isValidPupilAssetKey(assetKey: String) = pupilAssetKeys.contains(assetKey)

    fun isValidEyebrowAssetKey(assetKey: String) = eyebrowAssetKeys.contains(assetKey)

    fun isValidEyelashAssetKey(assetKey: String) = eyelashAssetKeys.contains(assetKey)

    fun isValidMouthAssetKey(assetKey: String) = mouthAssetKeys.contains(assetKey)

    fun buildSeedItems(): List<AvatarItem> = buildList {
        for (styleNo in 1..12) {
            val assetNo = styleNo.toAssetNo()
            val priceChecks = when (styleNo) {
                in 1..4 -> 1
                in 5..8 -> 2
                else -> 3
            }
            val rarity = when (styleNo) {
                in 1..4 -> AvatarRarity.BASIC
                in 5..8 -> AvatarRarity.POINT
                else -> AvatarRarity.SIGNATURE
            }

            for (colorSpec in hairColorSpecs) {
                add(
                    AvatarItem(
                        category = AvatarCategory.HAIR,
                        name = "헤어 프리셋 $assetNo · ${colorSpec.label}",
                        description = "앞머리, 옆머리, 뒷머리가 함께 구성된 헤어 프리셋",
                        priceChecks = priceChecks,
                        rarity = rarity,
                        assetKey = "hair-$assetNo-${colorSpec.code}",
                        isDefault = assetNo == "01" && colorSpec.code == "f",
                    ),
                )
            }
        }

        for (index in 1..12) {
            val assetNo = index.toAssetNo()
            add(
                AvatarItem(
                    category = AvatarCategory.TOP,
                    name = "상의 $assetNo",
                    description = "치비 캐릭터용 상의 $assetNo",
                    priceChecks = when (index) {
                        in 1..4 -> 1
                        in 5..8 -> 2
                        else -> 3
                    },
                    rarity = when (index) {
                        in 1..4 -> AvatarRarity.BASIC
                        in 5..8 -> AvatarRarity.POINT
                        else -> AvatarRarity.SIGNATURE
                    },
                    assetKey = "top-$assetNo",
                    isDefault = assetNo == "01",
                ),
            )
        }

        for (index in 1..8) {
            val assetNo = index.toAssetNo()
            add(
                AvatarItem(
                    category = AvatarCategory.BOTTOM,
                    name = "하의 $assetNo",
                    description = "치비 캐릭터용 하의 $assetNo",
                    priceChecks = when (index) {
                        in 1..3 -> 1
                        in 4..6 -> 2
                        else -> 3
                    },
                    rarity = when (index) {
                        in 1..3 -> AvatarRarity.BASIC
                        in 4..6 -> AvatarRarity.POINT
                        else -> AvatarRarity.SIGNATURE
                    },
                    assetKey = "bottom-$assetNo",
                    isDefault = assetNo == "01",
                ),
            )
        }
    }

    private fun Int.toAssetNo(): String = toString().padStart(2, '0')

    private data class HairColorSpec(
        val code: String,
        val label: String,
    )
}
