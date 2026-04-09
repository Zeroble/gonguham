package com.gonguham.backend.auth

import com.gonguham.backend.avatar.AvatarAssetCatalog
import com.gonguham.backend.avatar.AvatarItemRepository
import com.gonguham.backend.avatar.UserAvatarItem
import com.gonguham.backend.avatar.UserAvatarItemRepository
import com.gonguham.backend.common.support.CurrentUserService
import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.domain.AvatarCategory
import com.gonguham.backend.user.AvatarProfile
import com.gonguham.backend.user.AvatarProfileRepository
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val avatarProfileRepository: AvatarProfileRepository,
    private val avatarItemRepository: AvatarItemRepository,
    private val userAvatarItemRepository: UserAvatarItemRepository,
    private val levelPolicy: LevelPolicy,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun signUp(request: SignUpRequest, httpRequest: HttpServletRequest): SessionUserResponse {
        val email = normalizeEmail(request.email)
        val nickname = normalizeNickname(request.nickname)

        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use.")
        }

        val user = userRepository.save(
            User(
                email = email,
                passwordHash = passwordEncoder.encode(request.password)!!,
                nickname = nickname,
                totalEarnedChecks = 0,
                currentChecks = 0,
                level = levelPolicy.levelFor(0),
                createdAt = LocalDateTime.now(),
            ),
        )

        createDefaultAvatarSetup(user)
        establishSession(httpRequest, user.id!!)
        return user.toResponse(levelPolicy)
    }

    @Transactional
    fun login(request: LoginRequest, httpRequest: HttpServletRequest): SessionUserResponse {
        val email = normalizeEmail(request.email)
        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.")
        }

        establishSession(httpRequest, user.id!!)
        return user.toResponse(levelPolicy)
    }

    fun logout(request: HttpServletRequest) {
        request.getSession(false)?.invalidate()
    }

    private fun establishSession(request: HttpServletRequest, userId: Long) {
        request.getSession(false)?.invalidate()
        request.getSession(true).setAttribute(CurrentUserService.SESSION_KEY, userId)
    }

    private fun createDefaultAvatarSetup(user: User) {
        val defaultItemsByCategory = avatarItemRepository.findAllByIsDefaultTrue()
            .associateBy { it.category }

        if (defaultItemsByCategory.isNotEmpty()) {
            userAvatarItemRepository.saveAll(
                defaultItemsByCategory.values.map { item ->
                    UserAvatarItem(
                        userId = user.id!!,
                        avatarItemId = item.id!!,
                        purchasedAt = LocalDateTime.now(),
                    )
                },
            )
        }

        val defaultAppearance = AvatarAssetCatalog.defaultAppearance()
        avatarProfileRepository.save(
            AvatarProfile(
                userId = user.id!!,
                bodyAssetKey = defaultAppearance.bodyAssetKey,
                pupilAssetKey = defaultAppearance.pupilAssetKey,
                eyebrowAssetKey = defaultAppearance.eyebrowAssetKey,
                eyelashAssetKey = defaultAppearance.eyelashAssetKey,
                mouthAssetKey = defaultAppearance.mouthAssetKey,
                equippedHairItemId = defaultItemsByCategory[AvatarCategory.HAIR]?.id,
                equippedTopItemId = defaultItemsByCategory[AvatarCategory.TOP]?.id,
                equippedBottomItemId = defaultItemsByCategory[AvatarCategory.BOTTOM]?.id,
                equippedShoesItemId = defaultItemsByCategory[AvatarCategory.SHOES]?.id,
                equippedPupilItemId = defaultItemsByCategory[AvatarCategory.PUPIL]?.id,
                equippedEyebrowItemId = defaultItemsByCategory[AvatarCategory.EYEBROW]?.id,
                equippedEyelashItemId = defaultItemsByCategory[AvatarCategory.EYELASH]?.id,
                equippedMouthItemId = defaultItemsByCategory[AvatarCategory.MOUTH]?.id,
            ),
        )
    }

    private fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    private fun normalizeNickname(raw: String): String {
        val nickname = raw.trim()
        if (nickname.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname is required.")
        }
        if (nickname.length > 20) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname must be 20 characters or fewer.")
        }
        return nickname
    }
}

fun User.toResponse(levelPolicy: LevelPolicy): SessionUserResponse = SessionUserResponse(
    id = id!!,
    nickname = nickname,
    currentChecks = currentChecks,
    totalEarnedChecks = totalEarnedChecks,
    level = levelPolicy.levelFor(totalEarnedChecks),
    profileImageUrl = profileImageUrl,
)
