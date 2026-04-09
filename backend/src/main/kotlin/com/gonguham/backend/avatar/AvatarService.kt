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
                pupilAssetKey = profile.pupilAssetKey,
                eyebrowAssetKey = profile.eyebrowAssetKey,
                eyelashAssetKey = profile.eyelashAssetKey,
                mouthAssetKey = profile.mouthAssetKey,
            ),
            equipped = EquippedAvatarResponse(
                hair = profile.equippedHairItemId?.let(::toSlot),
                top = profile.equippedTopItemId?.let(::toSlot),
                bottom = profile.equippedBottomItemId?.let(::toSlot),
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
                rarity = item.rarity.name,
                name = item.name,
                description = item.description,
                priceChecks = item.priceChecks,
                assetKey = item.assetKey,
                owned = ownedIds.contains(item.id!!),
                equipped = when (item.category) {
                    AvatarCategory.HAIR -> profile.equippedHairItemId == item.id
                    AvatarCategory.TOP -> profile.equippedTopItemId == item.id
                    AvatarCategory.BOTTOM -> profile.equippedBottomItemId == item.id
                },
            )
        }
    }

    @Transactional
    fun purchase(user: User, itemId: Long): SessionUserResponse {
        val item = avatarItemRepository.findById(itemId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다.")
        }
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
        val item = avatarItemRepository.findById(request.itemId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다.")
        }
        if (!userAvatarItemRepository.existsByUserIdAndAvatarItemId(user.id!!, request.itemId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "보유하지 않은 아이템입니다.")
        }
        val profile = ensureProfile(user.id!!)
        when (item.category) {
            AvatarCategory.HAIR -> profile.equippedHairItemId = item.id
            AvatarCategory.TOP -> profile.equippedTopItemId = item.id
            AvatarCategory.BOTTOM -> profile.equippedBottomItemId = item.id
        }
        avatarProfileRepository.save(profile)
        return summary(user)
    }

    @Transactional
    fun saveAppearance(user: User, request: SaveAvatarAppearanceRequest): AvatarSummaryResponse {
        validateFreeAppearance(request)

        val profile = ensureProfile(user.id!!)
        profile.bodyAssetKey = request.bodyAssetKey
        profile.pupilAssetKey = request.pupilAssetKey
        profile.eyebrowAssetKey = request.eyebrowAssetKey
        profile.eyelashAssetKey = request.eyelashAssetKey
        profile.mouthAssetKey = request.mouthAssetKey
        profile.equippedHairItemId = resolveOwnedItemId(user.id!!, request.hairItemId, AvatarCategory.HAIR)
        profile.equippedTopItemId = resolveOwnedItemId(user.id!!, request.topItemId, AvatarCategory.TOP)
        profile.equippedBottomItemId = resolveOwnedItemId(user.id!!, request.bottomItemId, AvatarCategory.BOTTOM)
        avatarProfileRepository.save(profile)
        return summary(user)
    }

    private fun toSlot(itemId: Long): AvatarSlotResponse {
        val item = avatarItemRepository.findById(itemId).orElseThrow()
        return AvatarSlotResponse(itemId = item.id!!, name = item.name, assetKey = item.assetKey)
    }

    private fun ensureProfile(userId: Long): AvatarProfile =
        avatarProfileRepository.findByUserId(userId) ?: avatarProfileRepository.save(AvatarProfile(userId = userId))

    private fun validateFreeAppearance(request: SaveAvatarAppearanceRequest) {
        if (!AvatarAssetCatalog.isValidBodyAssetKey(request.bodyAssetKey)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 스킨톤입니다.")
        }
        if (!AvatarAssetCatalog.isValidPupilAssetKey(request.pupilAssetKey)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 눈 파츠입니다.")
        }
        if (!AvatarAssetCatalog.isValidEyebrowAssetKey(request.eyebrowAssetKey)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 눈썹 파츠입니다.")
        }
        if (!AvatarAssetCatalog.isValidEyelashAssetKey(request.eyelashAssetKey)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 속눈썹 파츠입니다.")
        }
        if (!AvatarAssetCatalog.isValidMouthAssetKey(request.mouthAssetKey)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 입 파츠입니다.")
        }
    }

    private fun resolveOwnedItemId(userId: Long, itemId: Long?, category: AvatarCategory): Long? {
        if (itemId == null) {
            return null
        }

        val item = avatarItemRepository.findById(itemId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다.")
        }

        if (item.category != category) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 카테고리의 아이템입니다.")
        }

        if (!userAvatarItemRepository.existsByUserIdAndAvatarItemId(userId, itemId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "보유하지 않은 아이템입니다.")
        }

        return item.id
    }
}
