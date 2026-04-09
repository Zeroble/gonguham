package com.gonguham.backend.avatar

import com.gonguham.backend.domain.AvatarCategory
import com.gonguham.backend.user.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@Transactional
class AvatarServiceTests @Autowired constructor(
    private val avatarService: AvatarService,
    private val avatarItemRepository: AvatarItemRepository,
    private val userAvatarItemRepository: UserAvatarItemRepository,
    private val userRepository: UserRepository,
) {
    @Test
    fun `summary and shop include asset keys and free appearance`() {
        val leader = userRepository.findById(1L).orElseThrow()

        val summary = avatarService.summary(leader)
        val hairShop = avatarService.shop(leader, AvatarCategory.HAIR.name)

        assertEquals("body-05", summary.appearance.bodyAssetKey)
        assertEquals("pupil-03", summary.appearance.pupilAssetKey)
        assertNotNull(summary.equipped.hair)
        assertTrue(summary.equipped.hair.assetKey.startsWith("hair-"))
        assertTrue(hairShop.isNotEmpty())
        assertTrue(hairShop.all { it.assetKey.startsWith("hair-") })
    }

    @Test
    fun `save appearance rejects unowned item`() {
        val member = userRepository.findById(2L).orElseThrow()
        val current = avatarService.summary(member)
        val lockedTop = avatarItemRepository.findAllByCategoryOrderByPriceChecksAscNameAsc(AvatarCategory.TOP)
            .first { !userAvatarItemRepository.existsByUserIdAndAvatarItemId(member.id!!, it.id!!) }

        val exception = assertFailsWith<ResponseStatusException> {
            avatarService.saveAppearance(
                member,
                SaveAvatarAppearanceRequest(
                    hairItemId = current.equipped.hair?.itemId,
                    topItemId = lockedTop.id!!,
                    bottomItemId = current.equipped.bottom?.itemId,
                    bodyAssetKey = current.appearance.bodyAssetKey,
                    pupilAssetKey = current.appearance.pupilAssetKey,
                    eyebrowAssetKey = current.appearance.eyebrowAssetKey,
                    eyelashAssetKey = current.appearance.eyelashAssetKey,
                    mouthAssetKey = current.appearance.mouthAssetKey,
                ),
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    @Test
    fun `save appearance rejects invalid free asset key`() {
        val member = userRepository.findById(2L).orElseThrow()
        val current = avatarService.summary(member)

        val exception = assertFailsWith<ResponseStatusException> {
            avatarService.saveAppearance(
                member,
                SaveAvatarAppearanceRequest(
                    hairItemId = current.equipped.hair?.itemId,
                    topItemId = current.equipped.top?.itemId,
                    bottomItemId = current.equipped.bottom?.itemId,
                    bodyAssetKey = current.appearance.bodyAssetKey,
                    pupilAssetKey = "pupil-99",
                    eyebrowAssetKey = current.appearance.eyebrowAssetKey,
                    eyelashAssetKey = current.appearance.eyelashAssetKey,
                    mouthAssetKey = current.appearance.mouthAssetKey,
                ),
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    @Test
    fun `purchase only deducts checks once for duplicate purchase`() {
        val member = userRepository.findById(2L).orElseThrow()
        val item = avatarItemRepository.findAllByCategoryOrderByPriceChecksAscNameAsc(AvatarCategory.TOP)
            .first { it.assetKey == "top-02" }
        val initialChecks = member.currentChecks

        avatarService.purchase(member, item.id!!)
        val afterFirstPurchase = userRepository.findById(member.id!!).orElseThrow()

        avatarService.purchase(afterFirstPurchase, item.id!!)
        val afterSecondPurchase = userRepository.findById(member.id!!).orElseThrow()

        assertEquals(initialChecks - item.priceChecks, afterFirstPurchase.currentChecks)
        assertEquals(afterFirstPurchase.currentChecks, afterSecondPurchase.currentChecks)
        assertEquals(
            1,
            userAvatarItemRepository.findAllByUserId(member.id!!).count { it.avatarItemId == item.id!! },
        )
    }
}
