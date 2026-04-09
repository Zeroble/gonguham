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
        val session = request.getSession(false)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.")
        val userId = (session.getAttribute(SESSION_KEY) as? Number)?.toLong()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.")

        return userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.")
        }
    }

    companion object {
        const val SESSION_KEY = "AUTH_USER_ID"
    }
}
