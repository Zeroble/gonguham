package com.gonguham.backend.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:Email
    @field:NotBlank
    val email: String = "",
    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String = "",
    @field:NotBlank
    @field:Size(max = 20)
    val nickname: String = "",
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String = "",
    @field:NotBlank
    val password: String = "",
)

data class SessionUserResponse(
    val id: Long,
    val nickname: String,
    val currentChecks: Int,
    val totalEarnedChecks: Int,
    val level: Int,
    val profileImageUrl: String?,
)
