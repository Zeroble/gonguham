package com.gonguham.backend.common.support

import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class CurrentUserService(
    private val userRepository: UserRepository,
) {
    fun currentUser(request: HttpServletRequest): User {
        val userId = request.getHeader(HEADER_NAME)?.toLongOrNull() ?: DEFAULT_USER_ID
        return userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보가 없습니다.")
        }
    }

    companion object {
        const val HEADER_NAME = "X-Demo-User-Id"
        const val DEFAULT_USER_ID = 1L
    }
}
