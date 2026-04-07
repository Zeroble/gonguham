package com.gonguham.backend.study

import com.gonguham.backend.domain.AttendanceStatus
import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.MembershipRole
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.RepeatType
import com.gonguham.backend.domain.StudyStatus
import com.gonguham.backend.domain.StudyType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "studies")
class Study(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var leaderUserId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: StudyType = StudyType.TOPIC,
    @Column(nullable = false)
    var title: String = "",
    @Column(nullable = false, length = 2000)
    var description: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    @Column(nullable = false)
    var startTime: LocalTime = LocalTime.NOON,
    @Column(nullable = false)
    var endTime: LocalTime = LocalTime.NOON.plusHours(1),
    @Column(nullable = false)
    var startDate: LocalDate = LocalDate.now(),
    @Column(nullable = false)
    var endDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var repeatType: RepeatType = RepeatType.WEEKLY,
    @Column(nullable = false)
    var maxMembers: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var locationType: LocationType = LocationType.OFFLINE,
    @Column(nullable = false)
    var locationText: String = "",
    @Column(nullable = false, length = 1000)
    var rulesText: String = "",
    @Column(nullable = false, length = 1000)
    var suppliesText: String = "",
    @Column(nullable = false, length = 1000)
    var cautionText: String = "",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StudyStatus = StudyStatus.OPEN,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "study_tags")
class StudyTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var studyId: Long = 0,
    @Column(nullable = false)
    var name: String = "",
)

@Entity
@Table(name = "study_memberships")
class StudyMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var studyId: Long = 0,
    @Column(nullable = false)
    var userId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: MembershipRole = MembershipRole.MEMBER,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MembershipStatus = MembershipStatus.ACTIVE,
    @Column(nullable = false)
    var joinedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "study_sessions")
class StudySession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var studyId: Long = 0,
    @Column(nullable = false)
    var sessionNo: Int = 1,
    @Column(nullable = false)
    var title: String = "",
    @Column(nullable = false)
    var scheduledAt: LocalDateTime = LocalDateTime.now(),
    var placeText: String? = null,
    @Column(length = 1000)
    var noticeText: String? = null,
    var attendanceClosedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "session_participations")
class SessionParticipation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var sessionId: Long = 0,
    @Column(nullable = false)
    var userId: Long = 0,
    @Column(nullable = false)
    var planned: Boolean = true,
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "attendances")
class Attendance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var sessionId: Long = 0,
    @Column(nullable = false)
    var userId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AttendanceStatus = AttendanceStatus.PRESENT,
    @Column(nullable = false)
    var checkedByUserId: Long = 0,
    @Column(nullable = false)
    var checkedAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "posts")
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var studyId: Long = 0,
    @Column(nullable = false)
    var authorUserId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: PostType = PostType.POST,
    @Column(nullable = false)
    var title: String = "",
    @Column(nullable = false, length = 3000)
    var content: String = "",
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

interface StudyRepository : JpaRepository<Study, Long>

interface StudyTagRepository : JpaRepository<StudyTag, Long> {
    fun findAllByStudyIdIn(studyIds: Collection<Long>): List<StudyTag>
    fun findAllByStudyId(studyId: Long): List<StudyTag>
}

interface StudyMembershipRepository : JpaRepository<StudyMembership, Long> {
    fun findAllByUserIdAndStatus(userId: Long, status: MembershipStatus): List<StudyMembership>
    fun findAllByStudyIdAndStatus(studyId: Long, status: MembershipStatus): List<StudyMembership>
    fun findByStudyIdAndUserId(studyId: Long, userId: Long): StudyMembership?
    fun countByStudyIdAndStatus(studyId: Long, status: MembershipStatus): Long
}

interface StudySessionRepository : JpaRepository<StudySession, Long> {
    fun findAllByStudyIdOrderBySessionNoAsc(studyId: Long): List<StudySession>
}

interface SessionParticipationRepository : JpaRepository<SessionParticipation, Long> {
    fun findBySessionIdAndUserId(sessionId: Long, userId: Long): SessionParticipation?
    fun findAllBySessionId(sessionId: Long): List<SessionParticipation>
}

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun findAllBySessionId(sessionId: Long): List<Attendance>
    fun findBySessionIdAndUserId(sessionId: Long, userId: Long): Attendance?
}

interface PostRepository : JpaRepository<Post, Long> {
    fun findAllByStudyIdAndTypeOrderByCreatedAtDesc(studyId: Long, type: PostType): List<Post>
    fun findAllByStudyIdOrderByCreatedAtDesc(studyId: Long): List<Post>
}
