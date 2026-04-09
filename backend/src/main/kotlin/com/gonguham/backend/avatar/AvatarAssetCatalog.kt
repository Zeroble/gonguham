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
    val shoesAssetKeys = (1..4).map { "shoes-${it.toAssetNo()}" }.toSet()

    const val DEFAULT_BODY_ASSET_KEY = "body-01"
    const val DEFAULT_PUPIL_ASSET_KEY = "pupil-01"
    const val DEFAULT_EYEBROW_ASSET_KEY = "eyebrow-01"
    const val DEFAULT_EYELASH_ASSET_KEY = "eyelash-01"
    const val DEFAULT_MOUTH_ASSET_KEY = "mouth-01"
    const val DEFAULT_HAIR_ITEM_KEY = "hair-01-f"
    const val DEFAULT_TOP_ITEM_KEY = "top-06"
    const val DEFAULT_BOTTOM_ITEM_KEY = "bottom-04"
    val DEFAULT_SHOES_ITEM_KEY: String? = null
    const val DEFAULT_PUPIL_ITEM_KEY = DEFAULT_PUPIL_ASSET_KEY
    const val DEFAULT_EYEBROW_ITEM_KEY = DEFAULT_EYEBROW_ASSET_KEY
    const val DEFAULT_EYELASH_ITEM_KEY = DEFAULT_EYELASH_ASSET_KEY
    const val DEFAULT_MOUTH_ITEM_KEY = DEFAULT_MOUTH_ASSET_KEY

    fun defaultAppearance() = AvatarFreeAppearance(
        bodyAssetKey = DEFAULT_BODY_ASSET_KEY,
        pupilAssetKey = DEFAULT_PUPIL_ASSET_KEY,
        eyebrowAssetKey = DEFAULT_EYEBROW_ASSET_KEY,
        eyelashAssetKey = DEFAULT_EYELASH_ASSET_KEY,
        mouthAssetKey = DEFAULT_MOUTH_ASSET_KEY,
    )

    fun isValidBodyAssetKey(assetKey: String) = bodyAssetKeys.contains(assetKey)

    fun buildSeedItems(): List<AvatarItem> = buildList {
        for (styleNo in 1..12) {
            val assetNo = styleNo.toAssetNo()

            for (colorSpec in hairColorSpecs) {
                add(
                    AvatarItem(
                        category = AvatarCategory.HAIR,
                        name = "헤어 프리셋 $assetNo · ${colorSpec.label}",
                        priceChecks = 5,
                        rarity = AvatarRarity.BASIC,
                        assetKey = "hair-$assetNo-${colorSpec.code}",
                        isDefault = assetNo == "01" && colorSpec.code == "f",
                    ),
                )
            }
        }

        addAll(buildSimpleItems(AvatarCategory.TOP, "top", 12, "상의", defaultAssetNo = "06"))
        addAll(buildSimpleItems(AvatarCategory.BOTTOM, "bottom", 8, "하의", defaultAssetNo = "04"))
        addAll(buildSimpleItems(AvatarCategory.SHOES, "shoes", 4, "신발"))
        addAll(buildSimpleItems(AvatarCategory.PUPIL, "pupil", 16, "눈", defaultAssetNo = "01"))
        addAll(buildSimpleItems(AvatarCategory.EYEBROW, "eyebrow", 5, "눈썹", defaultAssetNo = "01"))
        addAll(buildSimpleItems(AvatarCategory.EYELASH, "eyelash", 5, "속눈썹", defaultAssetNo = "01"))
        addAll(buildSimpleItems(AvatarCategory.MOUTH, "mouth", 20, "입", defaultAssetNo = "01"))
    }

    private fun buildSimpleItems(
        category: AvatarCategory,
        assetPrefix: String,
        count: Int,
        label: String,
        defaultAssetNo: String? = null,
    ): List<AvatarItem> = (1..count).map { index ->
        val assetNo = index.toAssetNo()
        AvatarItem(
            category = category,
            name = "$label $assetNo",
            priceChecks = 5,
            rarity = AvatarRarity.BASIC,
            assetKey = "$assetPrefix-$assetNo",
            isDefault = defaultAssetNo != null && assetNo == defaultAssetNo,
        )
    }

    private fun Int.toAssetNo(): String = toString().padStart(2, '0')

    private data class HairColorSpec(
        val code: String,
        val label: String,
    )
}
