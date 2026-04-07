package com.gonguham.backend.study

import com.gonguham.backend.check.CheckLedger
import com.gonguham.backend.check.CheckLedgerRepository
import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.domain.AttendanceStatus
import com.gonguham.backend.domain.CheckChangeType
import com.gonguham.backend.domain.CheckReason
import com.gonguham.backend.domain.MembershipRole
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.RepeatType
import com.gonguham.backend.domain.StudyStatus
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class StudyService(
    private val studyRepository: StudyRepository,
    private val studyTagRepository: StudyTagRepository,
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studySessionRepository: StudySessionRepository,
    private val sessionParticipationRepository: SessionParticipationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val checkLedgerRepository: CheckLedgerRepository,
    private val levelPolicy: LevelPolicy,
) {
    fun listStudies(user: User, keyword: String?, type: String?): List<StudyCardResponse> {
        val joinedStudyIds = studyMembershipRepository.findAllByUserIdAndStatus(user.id!!, MembershipStatus.ACTIVE)
            .map { it.studyId }
            .toSet()
        val studies = studyRepository.findAll()
            .filter { it.status == StudyStatus.OPEN }
            .filter { keyword.isNullOrBlank() || it.title.contains(keyword, ignoreCase = true) || it.description.contains(keyword, ignoreCase = true) }
            .filter { type.isNullOrBlank() || it.type.name == type }
            .sortedWith(compareBy({ it.dayOfWeek.value }, { it.startTime }))
        val tagsByStudyId = studyTagRepository.findAllByStudyIdIn(studies.mapNotNull { it.id }).groupBy { it.studyId }

        return studies.map { study ->
            StudyCardResponse(
                studyId = study.id!!,
                type = studyTypeLabel(study.type.name),
                title = study.title,
                description = study.description,
                dayLabel = dayLabel(study.dayOfWeek.name),
                timeLabel = "${study.startTime} - ${study.endTime}",
                locationLabel = "${locationLabel(study.locationType.name)} · ${study.locationText}",
                tags = tagsByStudyId[study.id].orEmpty().map { it.name },
                slotsLabel = "${studyMembershipRepository.countByStudyIdAndStatus(study.id!!, MembershipStatus.ACTIVE)} / ${study.maxMembers}명",
                joined = joinedStudyIds.contains(study.id!!),
            )
        }
    }

    fun studyDetail(user: User, studyId: Long): StudyDetailResponse {
        val study = getStudy(studyId)
        val leader = userRepository.findById(study.leaderUserId).orElseThrow()
        val tags = studyTagRepository.findAllByStudyId(studyId).map { it.name }
        val sessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(studyId)
        val posts = postRepository.findAllByStudyIdOrderByCreatedAtDesc(studyId)
        val joined = studyMembershipRepository.findByStudyIdAndUserId(studyId, user.id!!) != null

        return StudyDetailResponse(
            studyId = study.id!!,
            type = studyTypeLabel(study.type.name),
            title = study.title,
            description = study.description,
            leaderNickname = leader.nickname,
            dayLabel = dayLabel(study.dayOfWeek.name),
            timeLabel = "${study.startTime} - ${study.endTime}",
            locationLabel = "${locationLabel(study.locationType.name)} · ${study.locationText}",
            rulesText = study.rulesText,
            suppliesText = study.suppliesText,
            cautionText = study.cautionText,
            tags = tags,
            slotsLabel = "${studyMembershipRepository.countByStudyIdAndStatus(studyId, MembershipStatus.ACTIVE)} / ${study.maxMembers}명",
            joined = joined,
            sessions = sessions.map {
                StudyDetailSession(
                    sessionId = it.id!!,
                    sessionNo = it.sessionNo,
                    title = it.title,
                    scheduledAt = it.scheduledAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
                )
            },
            notice = posts.firstOrNull { it.type == PostType.NOTICE }?.toFeed(),
            posts = posts.filter { it.type == PostType.POST }.take(5).map { it.toFeed() },
        )
    }

    @Transactional
    fun createStudy(user: User, request: CreateStudyRequest): StudyDetailResponse {
        val study = studyRepository.save(
            Study(
                leaderUserId = user.id!!,
                type = request.type,
                title = request.title,
                description = request.description,
                dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek),
                startTime = LocalTime.parse(request.startTime),
                endTime = LocalTime.parse(request.endTime),
                startDate = LocalDate.parse(request.startDate),
                endDate = LocalDate.parse(request.endDate),
                repeatType = if (request.startDate == request.endDate) RepeatType.ONCE else RepeatType.WEEKLY,
                maxMembers = request.maxMembers,
                locationType = request.locationType,
                locationText = request.locationText,
                rulesText = request.rulesText,
                suppliesText = request.suppliesText,
                cautionText = request.cautionText,
                status = StudyStatus.OPEN,
                createdAt = LocalDateTime.now(),
            ),
        )
        studyMembershipRepository.save(
            StudyMembership(
                studyId = study.id!!,
                userId = user.id!!,
                role = MembershipRole.LEADER,
                status = MembershipStatus.ACTIVE,
                joinedAt = LocalDateTime.now(),
            ),
        )
        studyTagRepository.saveAll(request.tags.filter { it.isNotBlank() }.map { StudyTag(studyId = study.id!!, name = it.trim()) })
        val baseDate = LocalDate.parse(request.startDate)
        val baseTime = LocalTime.parse(request.startTime)
        studySessionRepository.saveAll(
            request.sessions.filter { it.isNotBlank() }.mapIndexed { index, title ->
                StudySession(
                    studyId = study.id!!,
                    sessionNo = index + 1,
                    title = title.trim(),
                    scheduledAt = LocalDateTime.of(baseDate.plusWeeks(index.toLong()), baseTime),
                    placeText = request.locationText,
                )
            },
        )
        return studyDetail(user, study.id!!)
    }

    @Transactional
    fun joinStudy(user: User, studyId: Long): StudyDetailResponse {
        val study = getStudy(studyId)
        val existing = studyMembershipRepository.findByStudyIdAndUserId(studyId, user.id!!)
        if (existing == null) {
            val currentCount = studyMembershipRepository.countByStudyIdAndStatus(studyId, MembershipStatus.ACTIVE)
            if (currentCount >= study.maxMembers) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "정원이 가득 찼습니다.")
            }
            studyMembershipRepository.save(
                StudyMembership(
                    studyId = studyId,
                    userId = user.id!!,
                    role = MembershipRole.MEMBER,
                    status = MembershipStatus.ACTIVE,
                    joinedAt = LocalDateTime.now(),
                ),
            )
        } else if (existing.status == MembershipStatus.LEFT) {
            existing.status = MembershipStatus.ACTIVE
            existing.joinedAt = LocalDateTime.now()
            studyMembershipRepository.save(existing)
        }

        studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(studyId)
            .firstOrNull { it.scheduledAt.isAfter(LocalDateTime.now()) }
            ?.let { session ->
                if (sessionParticipationRepository.findBySessionIdAndUserId(session.id!!, user.id!!) == null) {
                    sessionParticipationRepository.save(
                        SessionParticipation(
                            sessionId = session.id!!,
                            userId = user.id!!,
                            planned = true,
                            updatedAt = LocalDateTime.now(),
                        ),
                    )
                }
            }

        return studyDetail(user, studyId)
    }

    @Transactional
    fun updateParticipation(user: User, sessionId: Long, planned: Boolean): DashboardParticipationResult {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "회차를 찾을 수 없습니다.")
        }
        ensureMember(session.studyId, user.id!!)
        val participation = sessionParticipationRepository.findBySessionIdAndUserId(sessionId, user.id!!)
            ?: SessionParticipation(sessionId = sessionId, userId = user.id!!, planned = planned, updatedAt = LocalDateTime.now())
        participation.planned = planned
        participation.updatedAt = LocalDateTime.now()
        sessionParticipationRepository.save(participation)
        return DashboardParticipationResult(sessionId = sessionId, planned = planned)
    }

    @Transactional
    fun updateAttendance(leader: User, sessionId: Long, request: AttendanceRequest): AttendanceResult {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "회차를 찾을 수 없습니다.")
        }
        ensureLeader(session.studyId, leader.id!!)
        val awarded = mutableListOf<Long>()

        request.entries.forEach { entry ->
            ensureMember(session.studyId, entry.userId)
            val attendance = attendanceRepository.findBySessionIdAndUserId(sessionId, entry.userId)
                ?: Attendance(sessionId = sessionId, userId = entry.userId, checkedByUserId = leader.id!!)
            attendance.status = AttendanceStatus.valueOf(entry.status)
            attendance.checkedByUserId = leader.id!!
            attendance.checkedAt = LocalDateTime.now()
            attendanceRepository.save(attendance)

            if (attendance.status == AttendanceStatus.PRESENT &&
                !checkLedgerRepository.existsByUserIdAndReasonAndRefTypeAndRefId(entry.userId, CheckReason.ATTENDANCE, "SESSION", sessionId)
            ) {
                val member = userRepository.findById(entry.userId).orElseThrow()
                member.currentChecks += 2
                member.totalEarnedChecks += 2
                member.level = levelPolicy.levelFor(member.totalEarnedChecks)
                userRepository.save(member)
                checkLedgerRepository.save(
                    CheckLedger(
                        userId = member.id!!,
                        changeType = CheckChangeType.EARN,
                        amount = 2,
                        reason = CheckReason.ATTENDANCE,
                        refType = "SESSION",
                        refId = sessionId,
                        createdAt = LocalDateTime.now(),
                    ),
                )
                awarded += member.id!!
            }
        }

        return AttendanceResult(sessionId = sessionId, awardedUserIds = awarded)
    }

    fun posts(user: User, studyId: Long, type: PostType?): List<StudyFeedResponse> {
        ensureMember(studyId, user.id!!)
        val posts = if (type == null) {
            postRepository.findAllByStudyIdOrderByCreatedAtDesc(studyId)
        } else {
            postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(studyId, type)
        }
        return posts.map { it.toFeed() }
    }

    @Transactional
    fun createPost(user: User, studyId: Long, request: CreatePostRequest): StudyFeedResponse {
        ensureMember(studyId, user.id!!)
        if (request.type == PostType.NOTICE) {
            ensureLeader(studyId, user.id!!)
        }
        val post = postRepository.save(
            Post(
                studyId = studyId,
                authorUserId = user.id!!,
                type = request.type,
                title = request.title,
                content = request.content,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
        )
        return post.toFeed()
    }

    private fun getStudy(studyId: Long): Study = studyRepository.findById(studyId).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다.")
    }

    private fun ensureMember(studyId: Long, userId: Long) {
        val membership = studyMembershipRepository.findByStudyIdAndUserId(studyId, userId)
        if (membership == null || membership.status != MembershipStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "스터디 멤버만 접근할 수 있습니다.")
        }
    }

    private fun ensureLeader(studyId: Long, userId: Long) {
        val membership = studyMembershipRepository.findByStudyIdAndUserId(studyId, userId)
        if (membership == null || membership.status != MembershipStatus.ACTIVE || membership.role != MembershipRole.LEADER) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "스터디장만 처리할 수 있습니다.")
        }
    }

    private fun Post.toFeed(): StudyFeedResponse {
        val author = userRepository.findById(authorUserId).orElseThrow()
        return StudyFeedResponse(
            postId = id!!,
            type = type.name,
            title = title,
            content = content,
            authorNickname = author.nickname,
            createdAt = createdAt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")),
        )
    }

    private fun studyTypeLabel(raw: String): String = when (raw) {
        "TOPIC" -> "주제"
        "MOGAKGONG" -> "모각공"
        else -> "반짝"
    }

    private fun dayLabel(raw: String): String = when (raw) {
        "MONDAY" -> "월요일"
        "TUESDAY" -> "화요일"
        "WEDNESDAY" -> "수요일"
        "THURSDAY" -> "목요일"
        "FRIDAY" -> "금요일"
        "SATURDAY" -> "토요일"
        else -> "일요일"
    }

    private fun locationLabel(raw: String): String = if (raw == "ONLINE") "온라인" else "오프라인"
}
