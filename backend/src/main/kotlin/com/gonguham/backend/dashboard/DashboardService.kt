package com.gonguham.backend.dashboard

import com.gonguham.backend.domain.MembershipRole
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.study.AttendanceRepository
import com.gonguham.backend.study.PostRepository
import com.gonguham.backend.study.SessionParticipationRepository
import com.gonguham.backend.study.StudyMembershipRepository
import com.gonguham.backend.study.StudyRepository
import com.gonguham.backend.study.StudySessionRepository
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studyRepository: StudyRepository,
    private val studySessionRepository: StudySessionRepository,
    private val sessionParticipationRepository: SessionParticipationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    fun dashboardFor(user: User): DashboardResponse {
        val memberships = studyMembershipRepository.findAllByUserIdAndStatus(user.id!!, MembershipStatus.ACTIVE)
        val studies = studyRepository.findAllById(memberships.map { it.studyId }).sortedBy { it.createdAt }
        val today = LocalDate.now()
        val sessions = studies.flatMap { study -> studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(study.id!!) }

        val joinedStudies = studies.map { study ->
            JoinedStudyCard(
                studyId = study.id!!,
                typeLabel = typeLabel(study.type.name),
                title = study.title,
                timeLabel = "${dayLabel(study.dayOfWeek.name)} ${study.startTime}",
                locationLabel = study.locationText,
            )
        }

        val activeStudy = studies.firstOrNull()?.let { study ->
            val membership = memberships.first { it.studyId == study.id }
            val studySessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(study.id!!)
            val upcomingSession = studySessions.firstOrNull { it.scheduledAt.isAfter(LocalDateTime.now()) } ?: studySessions.lastOrNull()
            val members = studyMembershipRepository.findAllByStudyIdAndStatus(study.id!!, MembershipStatus.ACTIVE)
            val usersById = userRepository.findAllById(members.map { it.userId }).associateBy { it.id!! }
            val notice = postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(study.id!!, PostType.NOTICE).firstOrNull()
            val posts = postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(study.id!!, PostType.POST).take(5)

            ActiveStudyPanel(
                studyId = study.id!!,
                title = study.title,
                description = study.description,
                locationText = study.locationText,
                isLeader = membership.role == MembershipRole.LEADER,
                attendanceSessionId = upcomingSession?.id,
                attendanceSessionLabel = upcomingSession?.let { "${it.sessionNo}회차 ${it.title}" },
                sessions = studySessions.reversed().map { session ->
                    val attendance = attendanceRepository.findBySessionIdAndUserId(session.id!!, user.id!!)
                    val participation = sessionParticipationRepository.findBySessionIdAndUserId(session.id!!, user.id!!)
                    TimelineSession(
                        sessionId = session.id!!,
                        roundLabel = "${session.sessionNo}회차",
                        title = session.title,
                        statusLabel = when {
                            attendance != null -> if (attendance.status.name == "PRESENT") "출석 완료" else "결석"
                            participation?.planned == true -> "참여 예정"
                            session.scheduledAt.isAfter(LocalDateTime.now()) -> "미응답"
                            else -> "기록 없음"
                        },
                        scheduledAt = session.scheduledAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                    )
                },
                notice = notice?.let {
                    FeedItem(
                        postId = it.id!!,
                        title = it.title,
                        content = it.content,
                        authorNickname = usersById[it.authorUserId]?.nickname ?: "?????놁쓬",
                        createdAt = it.createdAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                    )
                },
                posts = posts.map {
                    FeedItem(
                        postId = it.id!!,
                        title = it.title,
                        content = it.content,
                        authorNickname = usersById[it.authorUserId]?.nickname ?: "?????놁쓬",
                        createdAt = it.createdAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                    )
                },
                attendanceRoster = upcomingSession?.let { session ->
                    val participationMap = sessionParticipationRepository.findAllBySessionId(session.id!!).associateBy { it.userId }
                    val attendanceMap = attendanceRepository.findAllBySessionId(session.id!!).associateBy { it.userId }
                    members.map { memberItem ->
                        AttendanceRosterItem(
                            userId = memberItem.userId,
                            nickname = usersById[memberItem.userId]?.nickname ?: "?????놁쓬",
                            planned = participationMap[memberItem.userId]?.planned ?: false,
                            attendanceStatus = attendanceMap[memberItem.userId]?.status?.name,
                        )
                    }
                }.orEmpty(),
            )
        }

        return DashboardResponse(
            todayScheduledCount = sessions.count { it.scheduledAt.toLocalDate() == today },
            joinedStudies = joinedStudies,
            activeStudy = activeStudy,
        )
    }

    private fun typeLabel(raw: String): String = when (raw) {
        "TOPIC" -> "주제"
        "MOGAKGONG" -> "모각공"
        else -> "반짝"
    }

    private fun dayLabel(raw: String): String = when (raw) {
        "MONDAY" -> "월"
        "TUESDAY" -> "화"
        "WEDNESDAY" -> "수"
        "THURSDAY" -> "목"
        "FRIDAY" -> "금"
        "SATURDAY" -> "토"
        else -> "일"
    }
}

