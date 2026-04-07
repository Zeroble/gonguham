package com.gonguham.backend.auth

data class DemoLoginRequest(
    val nickname: String? = null,
)

data class SessionUserResponse(
    val id: Long,
    val nickname: String,
    val currentChecks: Int,
    val totalEarnedChecks: Int,
    val level: Int,
    val profileImageUrl: String?,
)
