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
        val profile = avatarProfileRepository.findByUserId(user.id!!) ?: avatarProfileRepository.save(AvatarProfile(userId = user.id!!))
        return AvatarSummaryResponse(
            currentChecks = user.currentChecks,
            totalEarnedChecks = user.totalEarnedChecks,
            level = user.level,
            equipped = EquippedAvatarResponse(
                hair = profile.equippedHairItemId?.let(::toSlot),
                top = profile.equippedTopItemId?.let(::toSlot),
                bottom = profile.equippedBottomItemId?.let(::toSlot),
            ),
        )
    }

    fun shop(user: User, category: String?): List<AvatarShopItemResponse> {
        val ownedIds = userAvatarItemRepository.findAllByUserId(user.id!!).map { it.avatarItemId }.toSet()
        val profile = avatarProfileRepository.findByUserId(user.id!!)
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
                owned = ownedIds.contains(item.id!!),
                equipped = when (item.category) {
                    AvatarCategory.HAIR -> profile?.equippedHairItemId == item.id
                    AvatarCategory.TOP -> profile?.equippedTopItemId == item.id
                    AvatarCategory.BOTTOM -> profile?.equippedBottomItemId == item.id
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
        val profile = avatarProfileRepository.findByUserId(user.id!!) ?: avatarProfileRepository.save(AvatarProfile(userId = user.id!!))
        when (item.category) {
            AvatarCategory.HAIR -> profile.equippedHairItemId = item.id
            AvatarCategory.TOP -> profile.equippedTopItemId = item.id
            AvatarCategory.BOTTOM -> profile.equippedBottomItemId = item.id
        }
        avatarProfileRepository.save(profile)
        return summary(user)
    }

    private fun toSlot(itemId: Long): AvatarSlotResponse {
        val item = avatarItemRepository.findById(itemId).orElseThrow()
        return AvatarSlotResponse(itemId = item.id!!, name = item.name)
    }
}
