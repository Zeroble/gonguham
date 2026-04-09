package com.gonguham.backend.user

import com.gonguham.backend.avatar.AvatarService
import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.domain.AttendanceStatus
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.study.AttendanceRepository
import com.gonguham.backend.study.PostCommentRepository
import com.gonguham.backend.study.PostRepository
import com.gonguham.backend.study.StudyMembershipRepository
import com.gonguham.backend.study.StudySession
import com.gonguham.backend.study.StudySessionRepository
import kotlin.math.roundToInt
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studySessionRepository: StudySessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
    private val avatarService: AvatarService,
    private val levelPolicy: LevelPolicy,
) {
    fun profileFor(userId: Long): UserProfileResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")
        }
        val levelProgress = levelPolicy.progressFor(user.totalEarnedChecks)
        val memberships = studyMembershipRepository.findAllByUserId(user.id!!)
        val studyIds = memberships.map { it.studyId }.distinct()
        val activeStudyCount = memberships.count { it.status == MembershipStatus.ACTIVE }
        val closedRegularSessions = loadClosedRegularSessions(studyIds)
        val attendancesBySessionId = attendanceRepository.findAllByUserId(user.id!!)
            .associateBy { it.sessionId }
        val totalAttendanceCount = closedRegularSessions.count { session ->
            attendancesBySessionId[session.id]?.status == AttendanceStatus.PRESENT
        }
        val consecutiveAttendanceCount = closedRegularSessions
            .takeWhile { session -> attendancesBySessionId[session.id]?.status == AttendanceStatus.PRESENT }
            .size
        val recentTwoWeekSessions = filterRecentTwoWeekSessions(closedRegularSessions)
        val recentTwoWeekAttendedCount = recentTwoWeekSessions.count { session ->
            attendancesBySessionId[session.id]?.status == AttendanceStatus.PRESENT
        }
        val recentTwoWeekSessionCount = recentTwoWeekSessions.size
        val recentTwoWeekAttendanceRatePercent =
            if (recentTwoWeekSessionCount == 0) {
                0
            } else {
                ((recentTwoWeekAttendedCount.toDouble() / recentTwoWeekSessionCount.toDouble()) * 100)
                    .roundToInt()
            }
        val totalPostCount = postRepository.countByAuthorUserId(user.id!!).toInt()
        val totalCommentCount = postCommentRepository.countByAuthorUserId(user.id!!).toInt()
        val avatarSummary = avatarService.summary(user)

        return UserProfileResponse(
            userId = user.id!!,
            nickname = user.nickname,
            level = levelProgress.level,
            levelProgress = UserLevelProgressResponse(
                currentLevelStartTotalChecks = levelProgress.currentLevelStartTotalChecks,
                nextLevelTargetTotalChecks = levelProgress.nextLevelTargetTotalChecks,
                remainingChecksToNextLevel = levelProgress.remainingChecksToNextLevel,
                progressPercent = levelProgress.progressPercent,
            ),
            stats = UserProfileStatsResponse(
                activeStudyCount = activeStudyCount,
                currentChecks = user.currentChecks,
                consecutiveAttendanceCount = consecutiveAttendanceCount,
                totalAttendanceCount = totalAttendanceCount,
                recentTwoWeekAttendanceRatePercent = recentTwoWeekAttendanceRatePercent,
                recentTwoWeekAttendedCount = recentTwoWeekAttendedCount,
                recentTwoWeekSessionCount = recentTwoWeekSessionCount,
                totalEarnedChecks = user.totalEarnedChecks,
                totalPostCount = totalPostCount,
                totalCommentCount = totalCommentCount,
            ),
            avatar = UserProfileAvatarResponse(
                appearance = avatarSummary.appearance,
                equipped = avatarSummary.equipped,
            ),
        )
    }

    private fun loadClosedRegularSessions(studyIds: List<Long>): List<StudySession> {
        if (studyIds.isEmpty()) {
            return emptyList()
        }

        return studySessionRepository
            .findAllByStudyIdInAndSessionTypeAndAttendanceClosedAtIsNotNullOrderByScheduledAtDesc(
                studyIds,
                SessionType.REGULAR,
            )
            .sortedWith(compareByDescending<StudySession> { it.scheduledAt }.thenByDescending { it.id ?: 0L })
    }

    private fun filterRecentTwoWeekSessions(closedRegularSessions: List<StudySession>): List<StudySession> {
        val today = LocalDate.now()
        val windowStart = today.minusDays(14)

        return closedRegularSessions.filter { session ->
            val scheduledDate = session.scheduledAt.toLocalDate()
            !scheduledDate.isBefore(windowStart) && scheduledDate.isBefore(today)
        }
    }
}
