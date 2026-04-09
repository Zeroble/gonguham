package com.gonguham.backend.study

import com.gonguham.backend.support.PostgresIntegrationTest
import com.gonguham.backend.dashboard.DashboardService
import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.RepeatType
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.domain.StudyStatus
import com.gonguham.backend.domain.StudyType
import com.gonguham.backend.user.UserRepository
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@Transactional
class StudyScheduleApiTests @Autowired constructor(
    private val studyService: StudyService,
    private val dashboardService: DashboardService,
    private val studyRepository: StudyRepository,
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studySessionRepository: StudySessionRepository,
    private val userRepository: UserRepository,
) : PostgresIntegrationTest() {
    @Test
    fun `leader can create topic study with multiple days and break sessions`() {
        val leader = userRepository.findById(1L).orElseThrow()

        val created = studyService.createStudy(
            leader,
            CreateStudyRequest(
                type = StudyType.TOPIC,
                title = "자동 회차 생성 테스트",
                description = "다중 요일과 휴차 저장을 확인합니다.",
                daysOfWeek = listOf("TUESDAY", "THURSDAY"),
                startTime = "19:00",
                endTime = "21:00",
                startDate = "2026-04-14",
                endDate = "2026-04-23",
                maxMembers = 6,
                locationType = LocationType.OFFLINE,
                locationText = "스터디룸 A",
                rulesText = "결석 전날 공유",
                suppliesText = "노트북",
                cautionText = "지각 금지",
                tags = listOf("CS", "테스트"),
                sessions = listOf(
                    CreateStudySessionRequest(
                        title = "OT",
                        scheduledAt = "2026-04-14T19:00",
                        sessionType = SessionType.REGULAR,
                        placeText = "스터디룸 A",
                    ),
                    CreateStudySessionRequest(
                        title = "이 제목은 저장되지 않음",
                        scheduledAt = "2026-04-16T19:00",
                        sessionType = SessionType.BREAK,
                        placeText = "스터디룸 A",
                    ),
                    CreateStudySessionRequest(
                        title = "그래프 풀이",
                        scheduledAt = "2026-04-21T19:00",
                        sessionType = SessionType.REGULAR,
                        placeText = "스터디룸 A",
                    ),
                ),
            ),
        )

        val study = studyRepository.findById(created.studyId).orElseThrow()
        val sessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(created.studyId)

        assertEquals(listOf("TUESDAY", "THURSDAY"), created.daysOfWeek)
        assertEquals("화·목", created.dayLabel)
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), study.daysOfWeek)
        assertEquals(listOf(SessionType.REGULAR, SessionType.BREAK, SessionType.REGULAR), sessions.map { it.sessionType })
        assertEquals("쉬어가는 회차", sessions[1].title)
    }

    @Test
    fun `break session is shown in timeline and cannot toggle participation`() {
        val leader = userRepository.findById(1L).orElseThrow()
        val panel = dashboardService.studyHomePanel(leader, 1L)
        val breakTimeline = panel.sessions.first { it.sessionType == SessionType.BREAK.name }
        val breakSession = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(1L)
            .first { it.sessionType == SessionType.BREAK }

        assertEquals("BREAK", breakTimeline.nodeState)
        assertEquals("쉬어가는 회차", breakTimeline.title)
        assertEquals(false, breakTimeline.planned)
        assertNotEquals(breakSession.id, panel.currentSessionId)

        val exception = assertFailsWith<ResponseStatusException> {
            studyService.updateParticipation(leader, breakSession.id!!, true)
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    @Test
    fun `leader can edit upcoming study sessions from timeline editor`() {
        val leader = userRepository.findById(1L).orElseThrow()
        val sessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(1L)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val breakSession = sessions.first { it.sessionType == SessionType.BREAK }
        val twelfthSession = sessions.first { it.sessionNo == 12 }
        val thirteenthSession = sessions.first { it.sessionNo == 13 }

        val result = studyService.updateStudySessions(
            leader,
            1L,
            UpdateStudySessionsRequest(
                sessions = sessions.map { session ->
                    when (session.id) {
                        breakSession.id -> UpdateStudySessionRequest(
                            sessionId = session.id!!,
                            title = "unused",
                            scheduledAt = breakSession.scheduledAt.plusDays(1).format(formatter),
                            sessionType = SessionType.BREAK,
                        )
                        twelfthSession.id -> UpdateStudySessionRequest(
                            sessionId = session.id!!,
                            title = "동적 계획법 실전",
                            scheduledAt = thirteenthSession.scheduledAt.format(formatter),
                            sessionType = SessionType.REGULAR,
                        )
                        thirteenthSession.id -> UpdateStudySessionRequest(
                            sessionId = session.id!!,
                            title = "트리와 힙 마무리",
                            scheduledAt = twelfthSession.scheduledAt.format(formatter),
                            sessionType = SessionType.REGULAR,
                        )
                        else -> UpdateStudySessionRequest(
                            sessionId = session.id!!,
                            title = session.title,
                            scheduledAt = session.scheduledAt.format(formatter),
                            sessionType = session.sessionType,
                        )
                    }
                },
            ),
        )

        val updatedStudy = studyRepository.findById(result.studyId).orElseThrow()
        val updatedSessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(result.studyId)
        val movedTwelfth = updatedSessions.first { it.id == thirteenthSession.id }
        val movedThirteenth = updatedSessions.first { it.id == twelfthSession.id }
        val movedBreak = updatedSessions.first { it.id == breakSession.id }

        assertEquals(1L, result.studyId)
        assertEquals("트리와 힙 마무리", movedTwelfth.title)
        assertEquals("동적 계획법 실전", movedThirteenth.title)
        assertEquals(12, movedTwelfth.sessionNo)
        assertEquals(13, movedThirteenth.sessionNo)
        assertEquals("쉬어가는 회차", movedBreak.title)
        assertEquals(RepeatType.WEEKLY, updatedStudy.repeatType)
    }

    @Test
    fun `completed session cannot be edited from timeline editor`() {
        val leader = userRepository.findById(1L).orElseThrow()
        val sessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(1L)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val completedSession = sessions.first { it.attendanceClosedAt != null }

        val exception = assertFailsWith<ResponseStatusException> {
            studyService.updateStudySessions(
                leader,
                1L,
                UpdateStudySessionsRequest(
                    sessions = sessions.map { session ->
                        if (session.id == completedSession.id) {
                            UpdateStudySessionRequest(
                                sessionId = session.id!!,
                                title = "수정된 완료 회차",
                                scheduledAt = session.scheduledAt.format(formatter),
                                sessionType = session.sessionType,
                            )
                        } else {
                            UpdateStudySessionRequest(
                                sessionId = session.id!!,
                                title = session.title,
                                scheduledAt = session.scheduledAt.format(formatter),
                                sessionType = session.sessionType,
                            )
                        }
                    },
                ),
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
    }

    @Test
    fun `member can leave joined study and detail reflects left membership`() {
        val member = userRepository.findById(2L).orElseThrow()

        val result = studyService.leaveStudy(member, 1L)
        val membership = studyMembershipRepository.findByStudyIdAndUserId(1L, member.id!!)!!
        val dashboard = dashboardService.dashboardFor(member)
        val detail = studyService.studyDetail(member, 1L)

        assertEquals(1L, result.studyId)
        assertEquals(MembershipStatus.LEFT, membership.status)
        assertEquals(false, detail.joined)
        assertEquals(listOf(3L), dashboard.joinedStudies.map { it.studyId })
    }

    @Test
    fun `leader can close study and it disappears from dashboard`() {
        val guestLeader = userRepository.findById(3L).orElseThrow()

        val result = studyService.closeStudy(guestLeader, 2L)
        val closedStudy = studyRepository.findById(2L).orElseThrow()
        val dashboard = dashboardService.dashboardFor(guestLeader)

        assertEquals(2L, result.studyId)
        assertEquals(StudyStatus.CLOSED, closedStudy.status)
        assertEquals(true, dashboard.joinedStudies.none { it.studyId == 2L })
        assertEquals(
            true,
            studyMembershipRepository.findAllByStudyIdAndStatus(2L, MembershipStatus.ACTIVE).isEmpty(),
        )
    }
}
