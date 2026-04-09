package com.gonguham.backend.avatar

import com.gonguham.backend.support.PostgresIntegrationTest
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
) : PostgresIntegrationTest() {
    @Test
    fun `summary and shop include paid face slots and shoes`() {
        val leader = userRepository.findById(1L).orElseThrow()

        val summary = avatarService.summary(leader)
        val hairShop = avatarService.shop(leader, AvatarCategory.HAIR.name)
        val pupilShop = avatarService.shop(leader, AvatarCategory.PUPIL.name)

        assertEquals("body-05", summary.appearance.bodyAssetKey)
        assertEquals("pupil-03", summary.appearance.pupilAssetKey)
        assertNotNull(summary.equipped.hair)
        assertNotNull(summary.equipped.pupil)
        assertNotNull(summary.equipped.shoes)
        assertTrue(summary.equipped.hair.assetKey.startsWith("hair-"))
        assertEquals("shoes-02", summary.appearance.shoesAssetKey)
        assertTrue(hairShop.isNotEmpty())
        assertTrue(pupilShop.isNotEmpty())
        assertTrue(hairShop.all { it.assetKey.startsWith("hair-") })
        assertTrue(pupilShop.all { it.assetKey.startsWith("pupil-") })
    }

    @Test
    fun `save appearance purchases unowned items during save`() {
        val member = userRepository.findById(2L).orElseThrow()
        val current = avatarService.summary(member)
        val lockedTop = avatarItemRepository.findAllByCategoryOrderByPriceChecksAscNameAsc(AvatarCategory.TOP)
            .first { !userAvatarItemRepository.existsByUserIdAndAvatarItemId(member.id!!, it.id!!) }

        val nextSummary = avatarService.saveAppearance(
            member,
            SaveAvatarAppearanceRequest(
                hairItemId = current.equipped.hair?.itemId,
                topItemId = lockedTop.id!!,
                bottomItemId = current.equipped.bottom?.itemId,
                shoesItemId = current.equipped.shoes?.itemId,
                pupilItemId = current.equipped.pupil?.itemId,
                eyebrowItemId = current.equipped.eyebrow?.itemId,
                eyelashItemId = current.equipped.eyelash?.itemId,
                mouthItemId = current.equipped.mouth?.itemId,
                bodyAssetKey = current.appearance.bodyAssetKey,
            ),
        )

        val refreshedMember = userRepository.findById(member.id!!).orElseThrow()

        assertEquals(lockedTop.id, nextSummary.equipped.top?.itemId)
        assertTrue(userAvatarItemRepository.existsByUserIdAndAvatarItemId(member.id!!, lockedTop.id!!))
        assertEquals(4, refreshedMember.currentChecks)
    }

    @Test
    fun `save appearance rejects invalid skin tone key`() {
        val member = userRepository.findById(2L).orElseThrow()
        val current = avatarService.summary(member)

        val exception = assertFailsWith<ResponseStatusException> {
            avatarService.saveAppearance(
                member,
                SaveAvatarAppearanceRequest(
                    hairItemId = current.equipped.hair?.itemId,
                    topItemId = current.equipped.top?.itemId,
                    bottomItemId = current.equipped.bottom?.itemId,
                    shoesItemId = current.equipped.shoes?.itemId,
                    pupilItemId = current.equipped.pupil?.itemId,
                    eyebrowItemId = current.equipped.eyebrow?.itemId,
                    eyelashItemId = current.equipped.eyelash?.itemId,
                    mouthItemId = current.equipped.mouth?.itemId,
                    bodyAssetKey = "body-99",
                ),
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    @Test
    fun `save appearance rejects checkout when total price exceeds balance`() {
        val member = userRepository.findById(2L).orElseThrow()
        val current = avatarService.summary(member)
        val lockedTop = avatarItemRepository.findAllByCategoryOrderByPriceChecksAscNameAsc(AvatarCategory.TOP)
            .first { !userAvatarItemRepository.existsByUserIdAndAvatarItemId(member.id!!, it.id!!) }
        val lockedBottom = avatarItemRepository.findAllByCategoryOrderByPriceChecksAscNameAsc(AvatarCategory.BOTTOM)
            .first { !userAvatarItemRepository.existsByUserIdAndAvatarItemId(member.id!!, it.id!!) }

        val exception = assertFailsWith<ResponseStatusException> {
            avatarService.saveAppearance(
                member,
                SaveAvatarAppearanceRequest(
                    hairItemId = current.equipped.hair?.itemId,
                    topItemId = lockedTop.id!!,
                    bottomItemId = lockedBottom.id!!,
                    shoesItemId = current.equipped.shoes?.itemId,
                    pupilItemId = current.equipped.pupil?.itemId,
                    eyebrowItemId = current.equipped.eyebrow?.itemId,
                    eyelashItemId = current.equipped.eyelash?.itemId,
                    mouthItemId = current.equipped.mouth?.itemId,
                    bodyAssetKey = current.appearance.bodyAssetKey,
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

        assertEquals(initialChecks - 5, afterFirstPurchase.currentChecks)
        assertEquals(afterFirstPurchase.currentChecks, afterSecondPurchase.currentChecks)
        assertEquals(
            1,
            userAvatarItemRepository.findAllByUserId(member.id!!).count { it.avatarItemId == item.id!! },
        )
    }
}
