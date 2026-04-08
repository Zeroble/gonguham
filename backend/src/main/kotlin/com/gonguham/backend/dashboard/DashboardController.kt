package com.gonguham.backend.dashboard

import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val currentUserService: CurrentUserService,
    private val dashboardService: DashboardService,
) {
    @GetMapping
    fun dashboard(
        request: HttpServletRequest,
        @RequestParam(required = false) studyId: Long?,
    ): DashboardResponse =
        dashboardService.dashboardFor(currentUserService.currentUser(request), studyId)

    @GetMapping("/studies/{studyId}/panel")
    fun studyPanel(
        request: HttpServletRequest,
        @PathVariable studyId: Long,
    ): ActiveStudyPanel =
        dashboardService.studyHomePanel(currentUserService.currentUser(request), studyId)
}
