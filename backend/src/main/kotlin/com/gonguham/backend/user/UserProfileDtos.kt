package com.gonguham.backend.user

import com.gonguham.backend.avatar.AvatarAppearanceResponse
import com.gonguham.backend.avatar.EquippedAvatarResponse

data class UserProfileResponse(
    val userId: Long,
    val nickname: String,
    val level: Int,
    val levelProgress: UserLevelProgressResponse,
    val stats: UserProfileStatsResponse,
    val avatar: UserProfileAvatarResponse,
)

data class UserLevelProgressResponse(
    val currentLevelStartTotalChecks: Int,
    val nextLevelTargetTotalChecks: Int,
    val remainingChecksToNextLevel: Int,
    val progressPercent: Int,
)

data class UserProfileStatsResponse(
    val activeStudyCount: Int,
    val currentChecks: Int,
    val consecutiveAttendanceCount: Int,
    val totalAttendanceCount: Int,
    val recentTwoWeekAttendanceRatePercent: Int,
    val recentTwoWeekAttendedCount: Int,
    val recentTwoWeekSessionCount: Int,
    val totalEarnedChecks: Int,
    val totalPostCount: Int,
    val totalCommentCount: Int,
)

data class UserProfileAvatarResponse(
    val appearance: AvatarAppearanceResponse,
    val equipped: EquippedAvatarResponse,
)
