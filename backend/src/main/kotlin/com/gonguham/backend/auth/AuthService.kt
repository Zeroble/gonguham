package com.gonguham.backend.auth

import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val levelPolicy: LevelPolicy,
) {
    @Transactional
    fun demoLogin(request: DemoLoginRequest): SessionUserResponse {
        val user = userRepository.findByKakaoId(DEMO_KAKAO_ID)
            ?: userRepository.save(
                User(
                    kakaoId = DEMO_KAKAO_ID,
                    nickname = request.nickname?.ifBlank { null } ?: "정다솔",
                    totalEarnedChecks = 0,
                    currentChecks = 0,
                    level = levelPolicy.levelFor(0),
                    createdAt = LocalDateTime.now(),
                ),
            )

        if (!request.nickname.isNullOrBlank()) {
            user.nickname = request.nickname.trim()
            userRepository.save(user)
        }

        return user.toResponse()
    }

    companion object {
        private const val DEMO_KAKAO_ID = "kakao-101"
    }
}

fun User.toResponse(): SessionUserResponse = SessionUserResponse(
    id = id!!,
    nickname = nickname,
    currentChecks = currentChecks,
    totalEarnedChecks = totalEarnedChecks,
    level = level,
    profileImageUrl = profileImageUrl,
)
