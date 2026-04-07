package com.gonguham.backend.dashboard

import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    private val currentUserService: CurrentUserService,
    private val dashboardService: DashboardService,
) {
    @GetMapping
    fun dashboard(request: HttpServletRequest): DashboardResponse =
        dashboardService.dashboardFor(currentUserService.currentUser(request))
}
