package com.gonguham.backend.avatar

import com.gonguham.backend.auth.SessionUserResponse
import com.gonguham.backend.auth.toResponse
import com.gonguham.backend.check.CheckLedger
import com.gonguham.backend.check.CheckLedgerRepository
import com.gonguham.backend.domain.AvatarCategory
import com.gonguham.backend.domain.CheckChangeType
import com.gonguham.backend.domain.CheckReason
import com.gonguham.backend.user.AvatarProfile
import com.gonguham.backend.user.AvatarProfileRepository
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class AvatarService(
    private val avatarItemRepository: AvatarItemRepository,
    private val userAvatarItemRepository: UserAvatarItemRepository,
    private val avatarProfileRepository: AvatarProfileRepository,
    private val checkLedgerRepository: CheckLedgerRepository,
    private val userRepository: UserRepository,
) {
    fun summary(user: User): AvatarSummaryResponse {
        val profile = ensureProfile(user.id!!)
        return AvatarSummaryResponse(
            currentChecks = user.currentChecks,
            totalEarnedChecks = user.totalEarnedChecks,
            level = user.level,
            appearance = AvatarAppearanceResponse(
                bodyAssetKey = profile.bodyAssetKey,
                pupilAssetKey = resolveAppearanceAssetKey(
                    profile.equippedPupilItemId,
                    profile.pupilAssetKey,
                    AvatarAssetCatalog.DEFAULT_PUPIL_ASSET_KEY,
                ),
                eyebrowAssetKey = resolveAppearanceAssetKey(
                    profile.equippedEyebrowItemId,
                    profile.eyebrowAssetKey,
                    AvatarAssetCatalog.DEFAULT_EYEBROW_ASSET_KEY,
                ),
                eyelashAssetKey = resolveAppearanceAssetKey(
                    profile.equippedEyelashItemId,
                    profile.eyelashAssetKey,
                    AvatarAssetCatalog.DEFAULT_EYELASH_ASSET_KEY,
                ),
                mouthAssetKey = resolveAppearanceAssetKey(
                    profile.equippedMouthItemId,
                    profile.mouthAssetKey,
                    AvatarAssetCatalog.DEFAULT_MOUTH_ASSET_KEY,
                ),
                shoesAssetKey = profile.equippedShoesItemId?.let(::findItem)?.assetKey,
            ),
            equipped = EquippedAvatarResponse(
                hair = profile.equippedHairItemId?.let(::toSlot),
                top = profile.equippedTopItemId?.let(::toSlot),
                bottom = profile.equippedBottomItemId?.let(::toSlot),
                shoes = profile.equippedShoesItemId?.let(::toSlot),
                pupil = profile.equippedPupilItemId?.let(::toSlot),
                eyebrow = profile.equippedEyebrowItemId?.let(::toSlot),
                eyelash = profile.equippedEyelashItemId?.let(::toSlot),
                mouth = profile.equippedMouthItemId?.let(::toSlot),
            ),
        )
    }

    fun shop(user: User, category: String?): List<AvatarShopItemResponse> {
        val ownedIds = userAvatarItemRepository.findAllByUserId(user.id!!).map { it.avatarItemId }.toSet()
        val profile = ensureProfile(user.id!!)
        val items = if (category.isNullOrBlank()) {
            avatarItemRepository.findAll()
        } else {
            avatarItemRepository.findAllByCategoryOrderByPriceChecksAscNameAsc(AvatarCategory.valueOf(category))
        }
        return items.map { item ->
            AvatarShopItemResponse(
                itemId = item.id!!,
                category = item.category.name,
                name = item.name,
                priceChecks = item.priceChecks,
                assetKey = item.assetKey,
                owned = ownedIds.contains(item.id!!),
                equipped = isEquipped(profile, item),
            )
        }
    }

    @Transactional
    fun purchase(user: User, itemId: Long): SessionUserResponse {
        val item = findItem(itemId)
        if (userAvatarItemRepository.existsByUserIdAndAvatarItemId(user.id!!, itemId)) {
            return user.toResponse()
        }
        if (user.currentChecks < item.priceChecks) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "체크가 부족합니다.")
        }

        user.currentChecks -= item.priceChecks
        userRepository.save(user)
        userAvatarItemRepository.save(UserAvatarItem(userId = user.id!!, avatarItemId = itemId, purchasedAt = LocalDateTime.now()))
        checkLedgerRepository.save(
            CheckLedger(
                userId = user.id!!,
                changeType = CheckChangeType.SPEND,
                amount = -item.priceChecks,
                reason = CheckReason.ITEM_PURCHASE,
                refType = "ITEM",
                refId = itemId,
                createdAt = LocalDateTime.now(),
            ),
        )
        return user.toResponse()
    }

    @Transactional
    fun equip(user: User, request: EquipAvatarRequest): AvatarSummaryResponse {
        val item = findItem(request.itemId)
        if (!userAvatarItemRepository.existsByUserIdAndAvatarItemId(user.id!!, request.itemId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "보유하지 않은 아이템입니다.")
        }
        val profile = ensureProfile(user.id!!)
        applyEquippedItem(profile, item)
        avatarProfileRepository.save(profile)
        return summary(user)
    }

    @Transactional
    fun saveAppearance(user: User, request: SaveAvatarAppearanceRequest): AvatarSummaryResponse {
        validateFreeAppearance(request)

        val pendingPurchaseItems = linkedMapOf<Long, AvatarItem>()
        val profile = ensureProfile(user.id!!)
        profile.bodyAssetKey = request.bodyAssetKey
        profile.equippedHairItemId = resolveCheckoutItemId(user.id!!, request.hairItemId, AvatarCategory.HAIR, pendingPurchaseItems)
        profile.equippedTopItemId = resolveCheckoutItemId(user.id!!, request.topItemId, AvatarCategory.TOP, pendingPurchaseItems)
        profile.equippedBottomItemId = resolveCheckoutItemId(user.id!!, request.bottomItemId, AvatarCategory.BOTTOM, pendingPurchaseItems)
        profile.equippedShoesItemId = resolveCheckoutItemId(user.id!!, request.shoesItemId, AvatarCategory.SHOES, pendingPurchaseItems)
        profile.equippedPupilItemId = resolveCheckoutItemId(user.id!!, request.pupilItemId, AvatarCategory.PUPIL, pendingPurchaseItems)
        profile.equippedEyebrowItemId = resolveCheckoutItemId(user.id!!, request.eyebrowItemId, AvatarCategory.EYEBROW, pendingPurchaseItems)
        profile.equippedEyelashItemId = resolveCheckoutItemId(user.id!!, request.eyelashItemId, AvatarCategory.EYELASH, pendingPurchaseItems)
        profile.equippedMouthItemId = resolveCheckoutItemId(user.id!!, request.mouthItemId, AvatarCategory.MOUTH, pendingPurchaseItems)
        checkoutPendingItems(user, pendingPurchaseItems.values.toList())
        profile.pupilAssetKey = resolveAppearanceAssetKey(
            profile.equippedPupilItemId,
            profile.pupilAssetKey,
            AvatarAssetCatalog.DEFAULT_PUPIL_ASSET_KEY,
        )
        profile.eyebrowAssetKey = resolveAppearanceAssetKey(
            profile.equippedEyebrowItemId,
            profile.eyebrowAssetKey,
            AvatarAssetCatalog.DEFAULT_EYEBROW_ASSET_KEY,
        )
        profile.eyelashAssetKey = resolveAppearanceAssetKey(
            profile.equippedEyelashItemId,
            profile.eyelashAssetKey,
            AvatarAssetCatalog.DEFAULT_EYELASH_ASSET_KEY,
        )
        profile.mouthAssetKey = resolveAppearanceAssetKey(
            profile.equippedMouthItemId,
            profile.mouthAssetKey,
            AvatarAssetCatalog.DEFAULT_MOUTH_ASSET_KEY,
        )
        avatarProfileRepository.save(profile)
        return summary(user)
    }

    private fun findItem(itemId: Long): AvatarItem =
        avatarItemRepository.findById(itemId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다.")
        }

    private fun toSlot(itemId: Long): AvatarSlotResponse {
        val item = findItem(itemId)
        return AvatarSlotResponse(itemId = item.id!!, name = item.name, assetKey = item.assetKey)
    }

    private fun ensureProfile(userId: Long): AvatarProfile =
        avatarProfileRepository.findByUserId(userId) ?: avatarProfileRepository.save(AvatarProfile(userId = userId))

    private fun validateFreeAppearance(request: SaveAvatarAppearanceRequest) {
        if (!AvatarAssetCatalog.isValidBodyAssetKey(request.bodyAssetKey)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 스킨톤입니다.")
        }
    }

    private fun resolveCheckoutItemId(
        userId: Long,
        itemId: Long?,
        category: AvatarCategory,
        pendingPurchaseItems: MutableMap<Long, AvatarItem>,
    ): Long? {
        if (itemId == null) {
            return null
        }

        val item = findItem(itemId)

        if (item.category != category) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 카테고리의 아이템입니다.")
        }

        if (!userAvatarItemRepository.existsByUserIdAndAvatarItemId(userId, itemId)) {
            pendingPurchaseItems[item.id!!] = item
        }

        return item.id
    }

    private fun resolveOwnedItemId(userId: Long, itemId: Long?, category: AvatarCategory): Long? {
        if (itemId == null) {
            return null
        }

        val item = findItem(itemId)

        if (item.category != category) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 카테고리의 아이템입니다.")
        }

        if (!userAvatarItemRepository.existsByUserIdAndAvatarItemId(userId, itemId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "보유하지 않은 아이템입니다.")
        }

        return item.id
    }

    private fun checkoutPendingItems(user: User, pendingItems: List<AvatarItem>) {
        if (pendingItems.isEmpty()) {
            return
        }

        val totalPrice = pendingItems.sumOf { it.priceChecks }
        if (user.currentChecks < totalPrice) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "체크가 부족합니다.")
        }

        val now = LocalDateTime.now()
        user.currentChecks -= totalPrice
        userRepository.save(user)
        userAvatarItemRepository.saveAll(
            pendingItems.map { item ->
                UserAvatarItem(userId = user.id!!, avatarItemId = item.id!!, purchasedAt = now)
            },
        )
        checkLedgerRepository.saveAll(
            pendingItems.map { item ->
                CheckLedger(
                    userId = user.id!!,
                    changeType = CheckChangeType.SPEND,
                    amount = -item.priceChecks,
                    reason = CheckReason.ITEM_PURCHASE,
                    refType = "ITEM",
                    refId = item.id,
                    createdAt = now,
                )
            },
        )
    }

    private fun resolveAppearanceAssetKey(itemId: Long?, fallbackAssetKey: String?, defaultAssetKey: String): String {
        if (itemId != null) {
            return findItem(itemId).assetKey
        }

        return fallbackAssetKey ?: defaultAssetKey
    }

    private fun isEquipped(profile: AvatarProfile, item: AvatarItem): Boolean =
        when (item.category) {
            AvatarCategory.HAIR -> profile.equippedHairItemId == item.id
            AvatarCategory.TOP -> profile.equippedTopItemId == item.id
            AvatarCategory.BOTTOM -> profile.equippedBottomItemId == item.id
            AvatarCategory.SHOES -> profile.equippedShoesItemId == item.id
            AvatarCategory.PUPIL -> profile.equippedPupilItemId == item.id
            AvatarCategory.EYEBROW -> profile.equippedEyebrowItemId == item.id
            AvatarCategory.EYELASH -> profile.equippedEyelashItemId == item.id
            AvatarCategory.MOUTH -> profile.equippedMouthItemId == item.id
        }

    private fun applyEquippedItem(profile: AvatarProfile, item: AvatarItem) {
        when (item.category) {
            AvatarCategory.HAIR -> profile.equippedHairItemId = item.id
            AvatarCategory.TOP -> profile.equippedTopItemId = item.id
            AvatarCategory.BOTTOM -> profile.equippedBottomItemId = item.id
            AvatarCategory.SHOES -> profile.equippedShoesItemId = item.id
            AvatarCategory.PUPIL -> {
                profile.equippedPupilItemId = item.id
                profile.pupilAssetKey = item.assetKey
            }
            AvatarCategory.EYEBROW -> {
                profile.equippedEyebrowItemId = item.id
                profile.eyebrowAssetKey = item.assetKey
            }
            AvatarCategory.EYELASH -> {
                profile.equippedEyelashItemId = item.id
                profile.eyelashAssetKey = item.assetKey
            }
            AvatarCategory.MOUTH -> {
                profile.equippedMouthItemId = item.id
                profile.mouthAssetKey = item.assetKey
            }
        }
    }
}
