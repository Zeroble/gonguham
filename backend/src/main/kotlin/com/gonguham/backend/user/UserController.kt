package com.gonguham.backend.user

import com.gonguham.backend.auth.SessionUserResponse
import com.gonguham.backend.auth.toResponse
import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me")
class UserController(
    private val currentUserService: CurrentUserService,
    private val userRepository: UserRepository,
) {
    @PatchMapping
    fun updateNickname(
        request: HttpServletRequest,
        @RequestBody body: UpdateNicknameRequest,
    ): SessionUserResponse {
        val user = currentUserService.currentUser(request)
        user.nickname = body.nickname.trim()
        userRepository.save(user)
        return user.toResponse()
    }
}
