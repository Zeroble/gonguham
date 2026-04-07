package com.gonguham.backend.study

import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.StudyType

data class StudyCardResponse(
    val studyId: Long,
    val type: String,
    val title: String,
    val description: String,
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
    val leaderNickname: String,
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
)

data class StudyFeedResponse(
    val postId: Long,
    val type: String,
    val title: String,
    val content: String,
    val authorNickname: String,
    val createdAt: String,
)

data class CreateStudyRequest(
    val type: StudyType,
    val title: String,
    val description: String,
    val dayOfWeek: String,
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
    val sessions: List<String>,
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

data class DashboardParticipationResult(
    val sessionId: Long,
    val planned: Boolean,
)

data class CreatePostRequest(
    val type: PostType,
    val title: String,
    val content: String,
)
