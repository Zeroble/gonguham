package com.gonguham.backend.user

import com.gonguham.backend.support.PostgresIntegrationTest
import com.gonguham.backend.domain.AttendanceStatus
import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.MembershipRole
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.RepeatType
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.domain.StudyStatus
import com.gonguham.backend.domain.StudyType
import com.gonguham.backend.study.Attendance
import com.gonguham.backend.study.AttendanceRepository
import com.gonguham.backend.study.Post
import com.gonguham.backend.study.PostComment
import com.gonguham.backend.study.PostCommentRepository
import com.gonguham.backend.study.PostRepository
import com.gonguham.backend.study.Study
import com.gonguham.backend.study.StudyMembership
import com.gonguham.backend.study.StudyMembershipRepository
import com.gonguham.backend.study.StudyRepository
import com.gonguham.backend.study.StudySession
import com.gonguham.backend.study.StudySessionRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class UserProfileServiceTests @Autowired constructor(
    private val studyRepository: StudyRepository,
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studySessionRepository: StudySessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
    private val userProfileService: UserProfileService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : PostgresIntegrationTest() {
    @Test
    fun `profile stats summarize active studies attendance streak and recent rate`() {
        val now = LocalDateTime.now()
        val user = userRepository.save(
            User(
                email = "profile-spec-user@gonguham.app",
                passwordHash = passwordEncoder.encode("profile-spec-password")!!,
                nickname = "Spec User",
                totalEarnedChecks = 36,
                currentChecks = 11,
                level = 1,
                createdAt = now.minusDays(20),
            ),
        )
        val leader = userRepository.findById(1L).orElseThrow()
        val studyOne = studyRepository.save(baseStudy(leader.id!!, "Profile Study A", now.minusDays(30)))
        val studyTwo = studyRepository.save(baseStudy(leader.id!!, "Profile Study B", now.minusDays(20)))

        studyMembershipRepository.saveAll(
            listOf(
                StudyMembership(
                    studyId = studyOne.id!!,
                    userId = user.id!!,
                    role = MembershipRole.MEMBER,
                    status = MembershipStatus.ACTIVE,
                    joinedAt = now.minusDays(18),
                ),
                StudyMembership(
                    studyId = studyTwo.id!!,
                    userId = user.id!!,
                    role = MembershipRole.MEMBER,
                    status = MembershipStatus.ACTIVE,
                    joinedAt = now.minusDays(17),
                ),
            ),
        )

        val sessions = studySessionRepository.saveAll(
            listOf(
                closedSession(studyOne.id!!, 1, "Latest present", now.minusDays(1)),
                closedSession(studyTwo.id!!, 1, "Second present", now.minusDays(3)),
                closedSession(studyOne.id!!, 2, "Breaks streak", now.minusDays(5)),
                closedSession(studyTwo.id!!, 2, "Recent present", now.minusDays(9)),
                closedSession(studyOne.id!!, 3, "Old present", now.minusDays(16)),
                closedBreakSession(studyOne.id!!, 4, now.minusDays(2)),
            ),
        )

        attendanceRepository.saveAll(
            listOf(
                Attendance(
                    sessionId = sessions[0].id!!,
                    userId = user.id!!,
                    status = AttendanceStatus.PRESENT,
                    checkedByUserId = leader.id!!,
                    checkedAt = now.minusDays(1),
                ),
                Attendance(
                    sessionId = sessions[1].id!!,
                    userId = user.id!!,
                    status = AttendanceStatus.PRESENT,
                    checkedByUserId = leader.id!!,
                    checkedAt = now.minusDays(3),
                ),
                Attendance(
                    sessionId = sessions[2].id!!,
                    userId = user.id!!,
                    status = AttendanceStatus.ABSENT,
                    checkedByUserId = leader.id!!,
                    checkedAt = now.minusDays(5),
                ),
                Attendance(
                    sessionId = sessions[3].id!!,
                    userId = user.id!!,
                    status = AttendanceStatus.PRESENT,
                    checkedByUserId = leader.id!!,
                    checkedAt = now.minusDays(9),
                ),
                Attendance(
                    sessionId = sessions[4].id!!,
                    userId = user.id!!,
                    status = AttendanceStatus.PRESENT,
                    checkedByUserId = leader.id!!,
                    checkedAt = now.minusDays(16),
                ),
            ),
        )

        val posts = postRepository.saveAll(
            listOf(
                Post(
                    studyId = studyOne.id!!,
                    authorUserId = user.id!!,
                    type = PostType.POST,
                    title = "First profile post",
                    content = "Content",
                    createdAt = now.minusDays(2),
                    updatedAt = now.minusDays(2),
                ),
                Post(
                    studyId = studyTwo.id!!,
                    authorUserId = user.id!!,
                    type = PostType.NOTICE,
                    title = "Second profile post",
                    content = "Content",
                    createdAt = now.minusDays(4),
                    updatedAt = now.minusDays(4),
                ),
                Post(
                    studyId = studyOne.id!!,
                    authorUserId = leader.id!!,
                    type = PostType.POST,
                    title = "Leader post",
                    content = "Content",
                    createdAt = now.minusDays(6),
                    updatedAt = now.minusDays(6),
                ),
            ),
        )

        postCommentRepository.saveAll(
            listOf(
                PostComment(
                    postId = posts[0].id!!,
                    authorUserId = user.id!!,
                    content = "First comment",
                    createdAt = now.minusDays(1),
                    updatedAt = now.minusDays(1),
                ),
                PostComment(
                    postId = posts[1].id!!,
                    authorUserId = user.id!!,
                    content = "Second comment",
                    createdAt = now.minusDays(2),
                    updatedAt = now.minusDays(2),
                ),
                PostComment(
                    postId = posts[2].id!!,
                    authorUserId = user.id!!,
                    content = "Third comment",
                    createdAt = now.minusDays(3),
                    updatedAt = now.minusDays(3),
                ),
            ),
        )

        val profile = userProfileService.profileFor(user.id!!)

        assertEquals(4, profile.level)
        assertEquals(36, profile.levelProgress.currentLevelStartTotalChecks)
        assertEquals(52, profile.levelProgress.nextLevelTargetTotalChecks)
        assertEquals(16, profile.levelProgress.remainingChecksToNextLevel)
        assertEquals(0, profile.levelProgress.progressPercent)
        assertEquals(2, profile.stats.activeStudyCount)
        assertEquals(11, profile.stats.currentChecks)
        assertEquals(2, profile.stats.consecutiveAttendanceCount)
        assertEquals(4, profile.stats.totalAttendanceCount)
        assertEquals(75, profile.stats.recentTwoWeekAttendanceRatePercent)
        assertEquals(3, profile.stats.recentTwoWeekAttendedCount)
        assertEquals(4, profile.stats.recentTwoWeekSessionCount)
        assertEquals(2, profile.stats.totalPostCount)
        assertEquals(3, profile.stats.totalCommentCount)
        assertEquals("body-01", profile.avatar.appearance.bodyAssetKey)
    }

    private fun baseStudy(leaderUserId: Long, title: String, createdAt: LocalDateTime) =
        Study(
            leaderUserId = leaderUserId,
            type = StudyType.TOPIC,
            title = title,
            description = "$title description",
            daysOfWeek = mutableSetOf(DayOfWeek.MONDAY),
            startTime = LocalTime.of(19, 0),
            endTime = LocalTime.of(21, 0),
            startDate = LocalDate.now().minusDays(30),
            endDate = LocalDate.now().plusDays(30),
            repeatType = RepeatType.WEEKLY,
            maxMembers = 8,
            locationType = LocationType.ONLINE,
            locationText = "Discord",
            rulesText = "Rules",
            suppliesText = "Supplies",
            cautionText = "Caution",
            status = StudyStatus.OPEN,
            createdAt = createdAt,
        )

    private fun closedSession(studyId: Long, sessionNo: Int, title: String, scheduledAt: LocalDateTime) =
        StudySession(
            studyId = studyId,
            sessionNo = sessionNo,
            title = title,
            scheduledAt = scheduledAt,
            sessionType = SessionType.REGULAR,
            placeText = "Discord",
            attendanceClosedAt = scheduledAt.plusHours(2),
        )

    private fun closedBreakSession(studyId: Long, sessionNo: Int, scheduledAt: LocalDateTime) =
        StudySession(
            studyId = studyId,
            sessionNo = sessionNo,
            title = "Break",
            scheduledAt = scheduledAt,
            sessionType = SessionType.BREAK,
            placeText = "Discord",
            attendanceClosedAt = scheduledAt.plusHours(1),
        )
}
