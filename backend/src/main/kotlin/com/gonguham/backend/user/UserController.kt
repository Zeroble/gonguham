package com.gonguham.backend.user

import com.gonguham.backend.auth.SessionUserResponse
import com.gonguham.backend.auth.toResponse
import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/me")
class UserController(
    private val currentUserService: CurrentUserService,
    private val userRepository: UserRepository,
    private val levelPolicy: LevelPolicy,
) {
    @PatchMapping
    fun updateNickname(
        request: HttpServletRequest,
        @RequestBody body: UpdateNicknameRequest,
    ): SessionUserResponse {
        val user = currentUserService.currentUser(request)
        val nickname = body.nickname.trim()

        if (nickname.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요.")
        }

        if (nickname.length > 20) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 20자 이하여야 합니다.")
        }

        user.nickname = nickname
        userRepository.save(user)
        return user.toResponse(levelPolicy)
    }
}
