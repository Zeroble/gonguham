package com.gonguham.backend.dashboard

import com.gonguham.backend.domain.MembershipRole
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.study.AttendanceRepository
import com.gonguham.backend.study.PostRepository
import com.gonguham.backend.study.SessionParticipationRepository
import com.gonguham.backend.study.StudyMembershipRepository
import com.gonguham.backend.study.StudyRepository
import com.gonguham.backend.study.StudySessionRepository
import com.gonguham.backend.study.Study
import com.gonguham.backend.study.StudyMembership
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
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
    fun dashboardFor(user: User, selectedStudyId: Long? = null): DashboardResponse {
        val memberships = studyMembershipRepository.findAllByUserIdAndStatus(user.id!!, MembershipStatus.ACTIVE)
        val membershipsByStudyId = memberships.associateBy { it.studyId }
        val studies = studyRepository.findAllById(memberships.map { it.studyId }).sortedBy { it.createdAt }
        val today = LocalDate.now()
        val sessions = studies.flatMap { study ->
            studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(study.id!!)
                .filter { it.sessionType == SessionType.REGULAR }
        }

        val joinedStudies = studies.map { study ->
            JoinedStudyCard(
                studyId = study.id!!,
                typeLabel = typeLabel(study.type.name),
                title = study.title,
                timeLabel = "${dayLabel(study.daysOfWeek)} ${study.startTime}",
                locationLabel = study.locationText,
            )
        }

        val defaultStudyId = studies.firstOrNull { it.id == selectedStudyId }?.id ?: studies.firstOrNull()?.id
        val studyPanels = studies.mapNotNull { study ->
            membershipsByStudyId[study.id!!]?.let { membership ->
                buildStudyPanel(user, study, membership)
            }
        }

        return DashboardResponse(
            todayScheduledCount = sessions.count { it.scheduledAt.toLocalDate() == today },
            defaultStudyId = defaultStudyId,
            joinedStudies = joinedStudies,
            studyPanels = studyPanels,
        )
    }

    fun studyHomePanel(user: User, studyId: Long): ActiveStudyPanel {
        val membership = studyMembershipRepository.findByStudyIdAndUserId(studyId, user.id!!)
            ?.takeIf { it.status == MembershipStatus.ACTIVE }
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "스터디 멤버만 조회할 수 있습니다.")
        val study = studyRepository.findById(studyId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다.")
        }
        return buildStudyPanel(user, study, membership)
    }

    private fun typeLabel(raw: String): String = when (raw) {
        "TOPIC" -> "주제"
        "MOGAKGONG" -> "모각공"
        else -> "반짝"
    }

    private fun dayLabel(days: Collection<java.time.DayOfWeek>): String =
        days.sortedBy { it.value }.joinToString("·") { day ->
            when (day.name) {
                "MONDAY" -> "월"
                "TUESDAY" -> "화"
                "WEDNESDAY" -> "수"
                "THURSDAY" -> "목"
                "FRIDAY" -> "금"
                "SATURDAY" -> "토"
                else -> "일"
            }
        }

    private fun resolveCurrentSession(
        sessions: List<com.gonguham.backend.study.StudySession>,
    ) = sessions.firstOrNull {
        it.sessionType == SessionType.REGULAR && it.attendanceClosedAt == null
    }

    private fun buildStudyPanel(
        user: User,
        study: Study,
        membership: StudyMembership,
    ): ActiveStudyPanel {
        val studySessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(study.id!!)
        val currentSession = resolveCurrentSession(studySessions)
        val currentSessionId = currentSession?.id
        val orderedSessions = studySessionRepository.findAllByStudyIdOrderBySessionNoDesc(study.id!!)
        val members = studyMembershipRepository.findAllByStudyIdAndStatus(study.id!!, MembershipStatus.ACTIVE)
        val usersById = userRepository.findAllById(members.map { it.userId }).associateBy { it.id!! }
        val notice = postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(study.id!!, PostType.NOTICE).firstOrNull()
        val posts = postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(study.id!!, PostType.POST).take(5)

        return ActiveStudyPanel(
            studyId = study.id!!,
            title = study.title,
            description = study.description,
            locationText = study.locationText,
            isLeader = membership.role == MembershipRole.LEADER,
            currentSessionId = currentSessionId,
            attendanceSessionId = currentSessionId,
            attendanceSessionLabel = currentSession?.let { "${it.sessionNo}회차 ${it.title}" },
            sessions = orderedSessions.map { session ->
                val attendance = if (session.sessionType == SessionType.BREAK) {
                    null
                } else {
                    attendanceRepository.findBySessionIdAndUserId(session.id!!, user.id!!)
                }
                val participation = if (session.sessionType == SessionType.BREAK) {
                    null
                } else {
                    sessionParticipationRepository.findBySessionIdAndUserId(session.id!!, user.id!!)
                }
                val planned = if (session.sessionType == SessionType.BREAK) {
                    false
                } else {
                    participation?.planned == true
                }
                val isCurrent = session.id == currentSessionId
                val isClosed = session.attendanceClosedAt != null
                val isFuture = !isCurrent && !isClosed && session.sessionType == SessionType.REGULAR
                val nodeState = when {
                    session.sessionType == SessionType.BREAK -> "BREAK"
                    isCurrent -> "CURRENT"
                    isFuture -> "FUTURE"
                    attendance?.status?.name == "PRESENT" -> "ATTENDED"
                    else -> "ABSENT"
                }
                TimelineSession(
                    sessionId = session.id!!,
                    sessionNo = session.sessionNo,
                    roundLabel = "${session.sessionNo}회차",
                    title = session.title,
                    nodeState = nodeState,
                    scheduledAt = session.scheduledAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                    scheduledAtValue = session.scheduledAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
                    planned = planned,
                    sessionType = session.sessionType.name,
                    editable = !isClosed,
                )
            },
            notice = notice?.let {
                FeedItem(
                    postId = it.id!!,
                    authorUserId = it.authorUserId,
                    title = it.title,
                    content = it.content,
                    authorNickname = usersById[it.authorUserId]?.nickname ?: "?????놁쓬",
                    createdAt = it.createdAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                )
            },
            posts = posts.map {
                FeedItem(
                    postId = it.id!!,
                    authorUserId = it.authorUserId,
                    title = it.title,
                    content = it.content,
                    authorNickname = usersById[it.authorUserId]?.nickname ?: "?????놁쓬",
                    createdAt = it.createdAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                )
            },
            attendanceRoster = currentSession?.let { session ->
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
}

