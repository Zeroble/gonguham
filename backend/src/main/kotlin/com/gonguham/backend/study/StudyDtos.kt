package com.gonguham.backend.study

import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.StudyType

data class StudyCardResponse(
    val studyId: Long,
    val type: String,
    val title: String,
    val description: String,
    val daysOfWeek: List<String>,
    val dayLabel: String,
    val timeLabel: String,
    val locationLabel: String,
    val tags: List<String>,
    val slotsLabel: String,
    val joined: Boolean,
)

data class StudyDetailResponse(
    val studyId: Long,
    val type: String,
    val title: String,
    val description: String,
    val leaderUserId: Long,
    val leaderNickname: String,
    val daysOfWeek: List<String>,
    val dayLabel: String,
    val timeLabel: String,
    val locationLabel: String,
    val rulesText: String,
    val suppliesText: String,
    val cautionText: String,
    val tags: List<String>,
    val slotsLabel: String,
    val joined: Boolean,
    val sessions: List<StudyDetailSession>,
    val notice: StudyFeedResponse?,
    val posts: List<StudyFeedResponse>,
)

data class StudyDetailSession(
    val sessionId: Long,
    val sessionNo: Int,
    val title: String,
    val scheduledAt: String,
    val sessionType: String,
)

data class StudyFeedResponse(
    val postId: Long,
    val authorUserId: Long,
    val type: String,
    val title: String,
    val content: String,
    val authorNickname: String,
    val createdAt: String,
)

data class PostDetailResponse(
    val postId: Long,
    val authorUserId: Long,
    val type: String,
    val title: String,
    val content: String,
    val authorNickname: String,
    val createdAt: String,
    val comments: List<PostCommentResponse>,
)

data class PostCommentResponse(
    val commentId: Long,
    val authorUserId: Long,
    val authorNickname: String,
    val content: String,
    val createdAt: String,
)

data class CreateStudyRequest(
    val type: StudyType,
    val title: String,
    val description: String,
    val daysOfWeek: List<String>,
    val startTime: String,
    val endTime: String,
    val startDate: String,
    val endDate: String,
    val maxMembers: Int,
    val locationType: LocationType,
    val locationText: String,
    val rulesText: String,
    val suppliesText: String,
    val cautionText: String,
    val tags: List<String>,
    val sessions: List<CreateStudySessionRequest>,
)

data class CreateStudySessionRequest(
    val title: String,
    val scheduledAt: String,
    val sessionType: SessionType,
    val placeText: String? = null,
)

data class UpdateStudySessionsRequest(
    val sessions: List<UpdateStudySessionRequest>,
)

data class UpdateStudySessionRequest(
    val sessionId: Long,
    val title: String,
    val scheduledAt: String,
    val sessionType: SessionType,
)

data class UpdateStudySessionsResult(
    val studyId: Long,
)

data class UpdateParticipationRequest(
    val planned: Boolean,
)

data class AttendanceEntryRequest(
    val userId: Long,
    val status: String,
)

data class AttendanceRequest(
    val entries: List<AttendanceEntryRequest>,
)

data class AttendanceResult(
    val sessionId: Long,
    val awardedUserIds: List<Long>,
)

data class SessionAttendancePanelResponse(
    val sessionId: Long,
    val sessionLabel: String,
    val roster: List<SessionAttendanceRosterItem>,
)

data class SessionAttendanceRosterItem(
    val userId: Long,
    val nickname: String,
    val planned: Boolean,
    val attendanceStatus: String?,
)

data class DashboardParticipationResult(
    val sessionId: Long,
    val planned: Boolean,
)

data class CreatePostRequest(
    val type: PostType,
    val title: String,
    val content: String,
)

data class CreateCommentRequest(
    val content: String,
)
