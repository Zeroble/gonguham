package com.gonguham.backend.auth

import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class AuthController(
    private val authService: AuthService,
    private val currentUserService: CurrentUserService,
    private val levelPolicy: LevelPolicy,
) {
    @PostMapping("/auth/demo-login")
    fun demoLogin(@RequestBody request: DemoLoginRequest): SessionUserResponse =
        authService.demoLogin(request)

    @GetMapping("/me")
    fun me(request: HttpServletRequest): SessionUserResponse =
        currentUserService.currentUser(request).toResponse(levelPolicy)
}
