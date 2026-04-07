package com.gonguham.backend.dashboard

data class DashboardResponse(
    val todayScheduledCount: Int,
    val joinedStudies: List<JoinedStudyCard>,
    val activeStudy: ActiveStudyPanel?,
)

data class JoinedStudyCard(
    val studyId: Long,
    val typeLabel: String,
    val title: String,
    val scheduleLabel: String,
)

data class ActiveStudyPanel(
    val studyId: Long,
    val title: String,
    val description: String,
    val locationText: String,
    val isLeader: Boolean,
    val attendanceSessionId: Long?,
    val attendanceSessionLabel: String?,
    val sessions: List<TimelineSession>,
    val notice: FeedItem?,
    val posts: List<FeedItem>,
    val attendanceRoster: List<AttendanceRosterItem>,
)

data class TimelineSession(
    val sessionId: Long,
    val roundLabel: String,
    val title: String,
    val statusLabel: String,
    val scheduledAt: String,
)

data class FeedItem(
    val postId: Long,
    val title: String,
    val content: String,
    val authorNickname: String,
    val createdAt: String,
)

data class AttendanceRosterItem(
    val userId: Long,
    val nickname: String,
    val planned: Boolean,
    val attendanceStatus: String?,
)
