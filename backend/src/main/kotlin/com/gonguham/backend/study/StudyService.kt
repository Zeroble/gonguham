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
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.domain.StudyStatus
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

private const val BREAK_TITLE = "쉬어가는 회차"

private const val LEADER_START_WINDOW_MINUTES = 30L
private const val LEADER_START_WINDOW_MESSAGE = "예정 시간 30분 전부터 스터디를 시작할 수 있어요."

private data class PlannedSession(
    val title: String,
    val scheduledAt: LocalDateTime,
    val sessionType: SessionType,
    val placeText: String?,
)

private data class PlannedSessionUpdate(
    val source: StudySession,
    val title: String,
    val scheduledAt: LocalDateTime,
    val sessionType: SessionType,
)

@Service
class StudyService(
    private val studyRepository: StudyRepository,
    private val studyTagRepository: StudyTagRepository,
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studySessionRepository: StudySessionRepository,
    private val sessionParticipationRepository: SessionParticipationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
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
            .filter {
                keyword.isNullOrBlank() ||
                    it.title.contains(keyword, ignoreCase = true) ||
                    it.description.contains(keyword, ignoreCase = true)
            }
            .filter { type.isNullOrBlank() || it.type.name == type }
            .sortedWith(compareBy({ primaryDayValue(it.daysOfWeek) }, { it.startTime }))
        val tagsByStudyId = studyTagRepository.findAllByStudyIdIn(studies.mapNotNull { it.id })
            .groupBy { it.studyId }

        return studies.map { study ->
            StudyCardResponse(
                studyId = study.id!!,
                type = studyTypeLabel(study.type.name),
                title = study.title,
                description = study.description,
                daysOfWeek = study.daysOfWeek.sortedBy { it.value }.map { it.name },
                dayLabel = dayLabel(study.daysOfWeek),
                timeLabel = "${study.startTime} - ${study.endTime}",
                locationLabel = "${locationLabel(study.locationType.name)} / ${study.locationText}",
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
        val joined =
            studyMembershipRepository.findByStudyIdAndUserId(studyId, user.id!!)
                ?.status == MembershipStatus.ACTIVE

        return StudyDetailResponse(
            studyId = study.id!!,
            type = studyTypeLabel(study.type.name),
            title = study.title,
            description = study.description,
            leaderUserId = leader.id!!,
            leaderNickname = leader.nickname,
            daysOfWeek = study.daysOfWeek.sortedBy { it.value }.map { it.name },
            dayLabel = dayLabel(study.daysOfWeek),
            timeLabel = "${study.startTime} - ${study.endTime}",
            locationLabel = "${locationLabel(study.locationType.name)} / ${study.locationText}",
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
                    sessionType = it.sessionType.name,
                )
            },
            notice = posts.firstOrNull { it.type == PostType.NOTICE }?.toFeed(),
            posts = posts.filter { it.type == PostType.POST }.take(5).map { it.toFeed() },
        )
    }

    @Transactional
    fun createStudy(user: User, request: CreateStudyRequest): StudyDetailResponse {
        val parsedDaysOfWeek = request.daysOfWeek
            .map { DayOfWeek.valueOf(it) }
            .toSortedSet(compareBy(DayOfWeek::getValue))
        val parsedStartDate = LocalDate.parse(request.startDate)
        val parsedEndDate = LocalDate.parse(request.endDate)
        val parsedStartTime = LocalTime.parse(request.startTime)
        val parsedEndTime = LocalTime.parse(request.endTime)
        val normalizedSessions = normalizeSessions(request, parsedStartDate, parsedEndDate)

        validateCreateStudyRequest(
            request = request,
            parsedDaysOfWeek = parsedDaysOfWeek,
            parsedStartDate = parsedStartDate,
            parsedEndDate = parsedEndDate,
            parsedStartTime = parsedStartTime,
            parsedEndTime = parsedEndTime,
            normalizedSessions = normalizedSessions,
        )

        val study = studyRepository.save(
            Study(
                leaderUserId = user.id!!,
                type = request.type,
                title = request.title.trim(),
                description = request.description.trim(),
                daysOfWeek = parsedDaysOfWeek.toMutableSet(),
                startTime = parsedStartTime,
                endTime = parsedEndTime,
                startDate = parsedStartDate,
                endDate = parsedEndDate,
                repeatType = if (normalizedSessions.count { it.sessionType == SessionType.REGULAR } <= 1) RepeatType.ONCE else RepeatType.WEEKLY,
                maxMembers = request.maxMembers,
                locationType = request.locationType,
                locationText = request.locationText.trim(),
                rulesText = request.rulesText.trim(),
                suppliesText = request.suppliesText.trim(),
                cautionText = request.cautionText.trim(),
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
        studyTagRepository.saveAll(
            request.tags
                .filter { it.isNotBlank() }
                .map { StudyTag(studyId = study.id!!, name = it.trim()) },
        )
        studySessionRepository.saveAll(
            normalizedSessions.mapIndexed { index, session ->
                StudySession(
                    studyId = study.id!!,
                    sessionNo = index + 1,
                    title = if (session.sessionType == SessionType.BREAK) BREAK_TITLE else session.title,
                    scheduledAt = session.scheduledAt,
                    sessionType = session.sessionType,
                    placeText = session.placeText ?: request.locationText.trim(),
                )
            },
        )
        return studyDetail(user, study.id!!)
    }

    @Transactional
    fun updateStudySessions(
        user: User,
        studyId: Long,
        request: UpdateStudySessionsRequest,
    ): UpdateStudySessionsResult {
        ensureLeader(studyId, user.id!!)
        val study = getStudy(studyId)
        val existingSessions = studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(studyId)
        val normalizedUpdates = normalizeSessionUpdates(study, existingSessions, request)

        // Move session numbers out of the way first so swaps do not violate the
        // unique `(study_id, session_no)` constraint while updates are flushed.
        normalizedUpdates.forEachIndexed { index, update ->
            update.source.sessionNo = -(index + 1)
        }
        studySessionRepository.saveAll(normalizedUpdates.map { it.source })
        studySessionRepository.flush()

        normalizedUpdates.forEachIndexed { index, update ->
            update.source.sessionNo = index + 1
            update.source.title = if (update.sessionType == SessionType.BREAK) BREAK_TITLE else update.title
            update.source.scheduledAt = update.scheduledAt
            update.source.sessionType = update.sessionType
            if (update.source.placeText.isNullOrBlank()) {
                update.source.placeText = study.locationText
            }
        }

        study.repeatType =
            if (normalizedUpdates.count { it.sessionType == SessionType.REGULAR } <= 1) {
                RepeatType.ONCE
            } else {
                RepeatType.WEEKLY
            }

        studyRepository.save(study)
        studySessionRepository.saveAll(normalizedUpdates.map { it.source })

        return UpdateStudySessionsResult(studyId = studyId)
    }

    @Transactional
    fun joinStudy(user: User, studyId: Long): StudyDetailResponse {
        val study = getStudy(studyId)
        if (study.status != StudyStatus.OPEN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 스터디에는 참여할 수 없습니다.")
        }
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

        resolveOpenRegularSession(studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(studyId))
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
    fun leaveStudy(user: User, studyId: Long): StudyActionResult {
        val membership = activeMembership(studyId, user.id!!)
        if (membership.role == MembershipRole.LEADER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "스터디장은 스터디 종료를 이용해주세요.")
        }

        membership.status = MembershipStatus.LEFT
        studyMembershipRepository.save(membership)

        return StudyActionResult(studyId = studyId, status = MembershipStatus.LEFT.name)
    }

    @Transactional
    fun closeStudy(user: User, studyId: Long): StudyActionResult {
        ensureLeader(studyId, user.id!!)
        val study = getStudy(studyId)
        study.status = StudyStatus.CLOSED
        studyRepository.save(study)

        val memberships = studyMembershipRepository.findAllByStudyIdAndStatus(studyId, MembershipStatus.ACTIVE)
        memberships.forEach { it.status = MembershipStatus.LEFT }
        studyMembershipRepository.saveAll(memberships)

        return StudyActionResult(studyId = studyId, status = StudyStatus.CLOSED.name)
    }

    @Transactional
    fun updateParticipation(user: User, sessionId: Long, planned: Boolean): DashboardParticipationResult {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "회차를 찾을 수 없습니다.")
        }
        if (session.sessionType == SessionType.BREAK) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "휴차 회차는 참여 예정 상태를 변경할 수 없습니다.")
        }
        ensureMember(session.studyId, user.id!!)
        val currentSessionId = resolveOpenRegularSession(
            studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(session.studyId),
        )?.id
        if (session.id != currentSessionId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 회차만 참여 상태를 변경할 수 있습니다.")
        }
        val participation = sessionParticipationRepository.findBySessionIdAndUserId(sessionId, user.id!!)
            ?: SessionParticipation(
                sessionId = sessionId,
                userId = user.id!!,
                planned = planned,
                updatedAt = LocalDateTime.now(),
            )
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
        if (session.sessionType == SessionType.BREAK) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "휴차 회차는 출석 체크를 진행할 수 없습니다.")
        }
        ensureLeader(session.studyId, leader.id!!)
        ensureLeaderCanManageOpenAttendance(session)
        val awarded = mutableListOf<Long>()

        request.entries.forEach { entry ->
            ensureMember(session.studyId, entry.userId)
            val attendance = attendanceRepository.findBySessionIdAndUserId(sessionId, entry.userId)
                ?: Attendance(sessionId = sessionId, userId = entry.userId, checkedByUserId = leader.id!!)
            attendance.status = AttendanceStatus.valueOf(entry.status)
            attendance.checkedByUserId = leader.id!!
            attendance.checkedAt = LocalDateTime.now()
            attendanceRepository.save(attendance)

            if (
                attendance.status == AttendanceStatus.PRESENT &&
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

        session.attendanceClosedAt = LocalDateTime.now()
        studySessionRepository.save(session)

        return AttendanceResult(sessionId = sessionId, awardedUserIds = awarded)
    }

    fun attendanceRoster(leader: User, sessionId: Long): SessionAttendancePanelResponse {
        val session = studySessionRepository.findById(sessionId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "회차를 찾을 수 없습니다.")
        }
        if (session.sessionType == SessionType.BREAK) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "휴차 회차는 출석 명단을 확인할 수 없습니다.")
        }
        ensureLeader(session.studyId, leader.id!!)
        ensureLeaderCanManageOpenAttendance(session)
        val members = studyMembershipRepository.findAllByStudyIdAndStatus(session.studyId, MembershipStatus.ACTIVE)
        val usersById = userRepository.findAllById(members.map { it.userId }).associateBy { it.id!! }
        val participationMap = sessionParticipationRepository.findAllBySessionId(session.id!!).associateBy { it.userId }
        val attendanceMap = attendanceRepository.findAllBySessionId(session.id!!).associateBy { it.userId }

        return SessionAttendancePanelResponse(
            sessionId = session.id!!,
            sessionLabel = "${session.sessionNo}회차 ${session.title}",
            roster = members.map { member ->
                SessionAttendanceRosterItem(
                    userId = member.userId,
                    nickname = usersById[member.userId]?.nickname ?: "알 수 없음",
                    planned = participationMap[member.userId]?.planned ?: false,
                    attendanceStatus = attendanceMap[member.userId]?.status?.name,
                )
            },
        )
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

    fun postDetail(user: User, postId: Long): PostDetailResponse {
        val post = getPost(postId)
        ensureMember(post.studyId, user.id!!)

        val comments = postCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId)
        val usersById = userRepository.findAllById(
            buildSet {
                add(post.authorUserId)
                comments.forEach { add(it.authorUserId) }
            },
        ).associateBy { it.id!! }

        return PostDetailResponse(
            postId = post.id!!,
            authorUserId = post.authorUserId,
            type = post.type.name,
            title = post.title,
            content = post.content,
            authorNickname = usersById[post.authorUserId]?.nickname ?: "알 수 없음",
            createdAt = formatDateTime(post.createdAt),
            comments = comments.map { comment ->
                comment.toResponse(usersById[comment.authorUserId]?.nickname ?: "알 수 없음")
            },
        )
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

    @Transactional
    fun createComment(user: User, postId: Long, request: CreateCommentRequest): PostCommentResponse {
        val post = getPost(postId)
        ensureMember(post.studyId, user.id!!)

        val content = request.content.trim()
        if (content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글 내용을 입력해주세요.")
        }

        val now = LocalDateTime.now()
        val comment = postCommentRepository.save(
            PostComment(
                postId = postId,
                authorUserId = user.id!!,
                content = content,
                createdAt = now,
                updatedAt = now,
            ),
        )

        return comment.toResponse(user.nickname)
    }

    private fun normalizeSessions(
        request: CreateStudyRequest,
        parsedStartDate: LocalDate,
        parsedEndDate: LocalDate,
    ): List<PlannedSession> =
        request.sessions.map { session ->
            val parsedScheduledAt = try {
                LocalDateTime.parse(session.scheduledAt)
            } catch (_: DateTimeParseException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "회차 날짜 형식이 올바르지 않습니다.")
            }

            PlannedSession(
                title = if (session.sessionType == SessionType.BREAK) BREAK_TITLE else session.title.trim(),
                scheduledAt = parsedScheduledAt,
                sessionType = session.sessionType,
                placeText = session.placeText?.trim()?.takeIf { it.isNotBlank() },
            )
        }
            .filter { !it.scheduledAt.toLocalDate().isBefore(parsedStartDate) }
            .filter { !it.scheduledAt.toLocalDate().isAfter(parsedEndDate) }
            .sortedBy { it.scheduledAt }

    private fun validateCreateStudyRequest(
        request: CreateStudyRequest,
        parsedDaysOfWeek: Set<DayOfWeek>,
        parsedStartDate: LocalDate,
        parsedEndDate: LocalDate,
        parsedStartTime: LocalTime,
        parsedEndTime: LocalTime,
        normalizedSessions: List<PlannedSession>,
    ) {
        if (request.title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "스터디 제목을 입력해주세요.")
        }
        if (parsedDaysOfWeek.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "요일을 하나 이상 선택해주세요.")
        }
        if (parsedEndDate.isBefore(parsedStartDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.")
        }
        if (!parsedEndTime.isAfter(parsedStartTime)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "종료 시간은 시작 시간보다 늦어야 합니다.")
        }
        if (request.maxMembers < 2) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "모집 인원은 2명 이상이어야 합니다.")
        }
        if (normalizedSessions.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "생성할 회차가 없습니다.")
        }
        if (normalizedSessions.none { it.sessionType == SessionType.REGULAR }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 회차는 최소 1개 이상 있어야 합니다.")
        }
        normalizedSessions.forEach { session ->
            if (session.scheduledAt.toLocalDate().isBefore(parsedStartDate) || session.scheduledAt.toLocalDate().isAfter(parsedEndDate)) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "회차 날짜는 운영 기간 안에 있어야 합니다.")
            }
            if (session.sessionType == SessionType.REGULAR && session.title.isBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 회차 제목을 입력해주세요.")
            }
        }
    }

    private fun normalizeSessionUpdates(
        study: Study,
        existingSessions: List<StudySession>,
        request: UpdateStudySessionsRequest,
    ): List<PlannedSessionUpdate> {
        if (request.sessions.size != existingSessions.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "모든 회차 정보를 함께 보내주세요.")
        }

        if (request.sessions.map { it.sessionId }.toSet().size != existingSessions.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "회차 정보가 중복되거나 누락되었습니다.")
        }

        val existingById = existingSessions.associateBy { it.id!! }
        val normalized = request.sessions.map { session ->
            val existing = existingById[session.sessionId]
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 회차가 포함되어 있습니다.")
            val parsedScheduledAt = try {
                LocalDateTime.parse(session.scheduledAt)
            } catch (_: DateTimeParseException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "회차 날짜 형식이 올바르지 않습니다.")
            }

            PlannedSessionUpdate(
                source = existing,
                title = if (session.sessionType == SessionType.BREAK) BREAK_TITLE else session.title.trim(),
                scheduledAt = parsedScheduledAt,
                sessionType = session.sessionType,
            )
        }.sortedBy { it.scheduledAt }

        if (normalized.none { it.sessionType == SessionType.REGULAR }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 회차는 최소 1개 이상 있어야 합니다.")
        }

        normalized.forEach { update ->
            val scheduledDate = update.scheduledAt.toLocalDate()
            val scheduledDateChanged = update.source.scheduledAt.toLocalDate() != scheduledDate
            if (scheduledDateChanged && (scheduledDate.isBefore(study.startDate) || scheduledDate.isAfter(study.endDate))) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "회차 날짜는 운영 기간 안에 있어야 합니다.")
            }
            if (update.sessionType == SessionType.REGULAR && update.title.isBlank()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 회차 제목을 입력해주세요.")
            }

            val sourceTitle =
                if (update.source.sessionType == SessionType.BREAK) BREAK_TITLE else update.source.title
            val sourceScheduledAt = update.source.scheduledAt.truncatedTo(ChronoUnit.MINUTES)
            val changed =
                sourceTitle != update.title ||
                    sourceScheduledAt != update.scheduledAt ||
                    update.source.sessionType != update.sessionType

            if (update.source.attendanceClosedAt != null && changed) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "완료된 회차는 수정할 수 없습니다.")
            }
        }

        return normalized
    }

    private fun resolveOpenRegularSession(sessions: List<StudySession>): StudySession? =
        sessions.firstOrNull { it.sessionType == SessionType.REGULAR && it.attendanceClosedAt == null }

    private fun ensureLeaderCanManageOpenAttendance(session: StudySession) {
        if (session.attendanceClosedAt != null) {
            return
        }

        val currentSession = resolveOpenRegularSession(
            studySessionRepository.findAllByStudyIdOrderBySessionNoAsc(session.studyId),
        ) ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 진행 중인 회차가 없습니다.")

        if (session.id != currentSession.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 회차만 출석을 시작할 수 있습니다.")
        }

        if (LocalDateTime.now().isBefore(session.scheduledAt.minusMinutes(LEADER_START_WINDOW_MINUTES))) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, LEADER_START_WINDOW_MESSAGE)
        }
    }

    private fun getStudy(studyId: Long): Study = studyRepository.findById(studyId).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다.")
    }

    private fun getPost(postId: Long): Post = postRepository.findById(postId).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.")
    }

    private fun ensureMember(studyId: Long, userId: Long) {
        activeMembership(studyId, userId)
    }

    private fun ensureLeader(studyId: Long, userId: Long) {
        val membership = activeMembership(studyId, userId)
        if (membership.role != MembershipRole.LEADER) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "스터디장만 처리할 수 있습니다.")
        }
    }

    private fun activeMembership(studyId: Long, userId: Long): StudyMembership {
        val membership = studyMembershipRepository.findByStudyIdAndUserId(studyId, userId)
        if (membership == null || membership.status != MembershipStatus.ACTIVE) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "스터디 멤버만 이용할 수 있습니다.")
        }

        return membership
    }

    private fun Post.toFeed(): StudyFeedResponse {
        val author = userRepository.findById(authorUserId).orElseThrow()
        return StudyFeedResponse(
            postId = id!!,
            authorUserId = authorUserId,
            type = type.name,
            title = title,
            content = content,
            authorNickname = author.nickname,
            createdAt = formatDateTime(createdAt),
        )
    }

    private fun PostComment.toResponse(authorNickname: String): PostCommentResponse =
        PostCommentResponse(
            commentId = id!!,
            authorUserId = authorUserId,
            authorNickname = authorNickname,
            content = content,
            createdAt = formatDateTime(createdAt),
        )

    private fun formatDateTime(value: LocalDateTime): String =
        value.format(DateTimeFormatter.ofPattern("MM.dd HH:mm"))

    private fun studyTypeLabel(raw: String): String = when (raw) {
        "TOPIC" -> "주제"
        "MOGAKGONG" -> "모각공"
        else -> "반짝"
    }

    private fun dayLabel(days: Collection<DayOfWeek>): String =
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

    private fun locationLabel(raw: String): String = if (raw == "ONLINE") "온라인" else "오프라인"

    private fun primaryDayValue(days: Collection<DayOfWeek>): Int =
        days.minOfOrNull { it.value } ?: DayOfWeek.SUNDAY.value
}
