package com.gonguham.backend.study

import com.gonguham.backend.common.support.CurrentUserService
import com.gonguham.backend.domain.PostType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class StudyController(
    private val currentUserService: CurrentUserService,
    private val studyService: StudyService,
) {
    @GetMapping("/studies")
    fun studies(
        request: HttpServletRequest,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) type: String?,
    ): List<StudyCardResponse> =
        studyService.listStudies(currentUserService.currentUser(request), keyword, type)

    @GetMapping("/studies/{studyId}")
    fun detail(
        request: HttpServletRequest,
        @PathVariable studyId: Long,
    ): StudyDetailResponse =
        studyService.studyDetail(currentUserService.currentUser(request), studyId)

    @PostMapping("/studies")
    fun create(
        request: HttpServletRequest,
        @RequestBody body: CreateStudyRequest,
    ): StudyDetailResponse =
        studyService.createStudy(currentUserService.currentUser(request), body)

    @PostMapping("/studies/{studyId}/join")
    fun join(
        request: HttpServletRequest,
        @PathVariable studyId: Long,
    ): StudyDetailResponse =
        studyService.joinStudy(currentUserService.currentUser(request), studyId)

    @PatchMapping("/sessions/{sessionId}/participation")
    fun updateParticipation(
        request: HttpServletRequest,
        @PathVariable sessionId: Long,
        @RequestBody body: UpdateParticipationRequest,
    ): DashboardParticipationResult =
        studyService.updateParticipation(currentUserService.currentUser(request), sessionId, body.planned)

    @PostMapping("/sessions/{sessionId}/attendance")
    fun updateAttendance(
        request: HttpServletRequest,
        @PathVariable sessionId: Long,
        @RequestBody body: AttendanceRequest,
    ): AttendanceResult =
        studyService.updateAttendance(currentUserService.currentUser(request), sessionId, body)

    @GetMapping("/sessions/{sessionId}/attendance-roster")
    fun attendanceRoster(
        request: HttpServletRequest,
        @PathVariable sessionId: Long,
    ): SessionAttendancePanelResponse =
        studyService.attendanceRoster(currentUserService.currentUser(request), sessionId)

    @GetMapping("/studies/{studyId}/posts")
    fun posts(
        request: HttpServletRequest,
        @PathVariable studyId: Long,
        @RequestParam(required = false) type: PostType?,
    ): List<StudyFeedResponse> =
        studyService.posts(currentUserService.currentUser(request), studyId, type)

    @PostMapping("/studies/{studyId}/posts")
    fun createPost(
        request: HttpServletRequest,
        @PathVariable studyId: Long,
        @RequestBody body: CreatePostRequest,
    ): StudyFeedResponse =
        studyService.createPost(currentUserService.currentUser(request), studyId, body)

    @GetMapping("/posts/{postId}")
    fun postDetail(
        request: HttpServletRequest,
        @PathVariable postId: Long,
    ): PostDetailResponse =
        studyService.postDetail(currentUserService.currentUser(request), postId)

    @PostMapping("/posts/{postId}/comments")
    fun createComment(
        request: HttpServletRequest,
        @PathVariable postId: Long,
        @RequestBody body: CreateCommentRequest,
    ): PostCommentResponse =
        studyService.createComment(currentUserService.currentUser(request), postId, body)
}
