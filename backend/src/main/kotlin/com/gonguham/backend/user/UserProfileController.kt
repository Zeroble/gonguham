package com.gonguham.backend.user

import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserProfileController(
    private val currentUserService: CurrentUserService,
    private val userProfileService: UserProfileService,
) {
    @GetMapping("/{userId}/profile")
    fun profile(
        request: HttpServletRequest,
        @PathVariable userId: Long,
    ): UserProfileResponse {
        currentUserService.currentUser(request)
        return userProfileService.profileFor(userId)
    }
}
