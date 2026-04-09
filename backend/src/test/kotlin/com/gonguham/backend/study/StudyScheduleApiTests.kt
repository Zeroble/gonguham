package com.gonguham.backend.study

import com.gonguham.backend.dashboard.DashboardService
import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.domain.StudyType
import com.gonguham.backend.user.UserRepository
import java.time.DayOfWeek
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
    private val studySessionRepository: StudySessionRepository,
    private val userRepository: UserRepository,
) {
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
}
