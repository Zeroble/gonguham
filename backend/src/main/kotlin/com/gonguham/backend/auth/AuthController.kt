package com.gonguham.backend.auth

import com.gonguham.backend.common.support.CurrentUserService
import com.gonguham.backend.common.support.LevelPolicy
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus

@RestController
@RequestMapping("/api/v1")
class AuthController(
    private val authService: AuthService,
    private val currentUserService: CurrentUserService,
    private val levelPolicy: LevelPolicy,
) {
    @PostMapping("/auth/signup")
    fun signUp(
        request: HttpServletRequest,
        @Valid @RequestBody body: SignUpRequest,
    ): SessionUserResponse = authService.signUp(body, request)

    @PostMapping("/auth/login")
    fun login(
        request: HttpServletRequest,
        @Valid @RequestBody body: LoginRequest,
    ): SessionUserResponse = authService.login(body, request)

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(request: HttpServletRequest) {
        authService.logout(request)
    }

    @GetMapping("/me")
    fun me(request: HttpServletRequest): SessionUserResponse =
        currentUserService.currentUser(request).toResponse(levelPolicy)
}
