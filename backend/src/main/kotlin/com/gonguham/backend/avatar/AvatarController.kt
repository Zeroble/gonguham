package com.gonguham.backend.avatar

import com.gonguham.backend.auth.SessionUserResponse
import com.gonguham.backend.common.support.CurrentUserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/avatar")
class AvatarController(
    private val currentUserService: CurrentUserService,
    private val avatarService: AvatarService,
) {
    @GetMapping
    fun summary(request: HttpServletRequest): AvatarSummaryResponse =
        avatarService.summary(currentUserService.currentUser(request))

    @GetMapping("/shop")
    fun shop(
        request: HttpServletRequest,
        @RequestParam(required = false) category: String?,
    ): List<AvatarShopItemResponse> =
        avatarService.shop(currentUserService.currentUser(request), category)

    @PostMapping("/items/{itemId}/purchase")
    fun purchase(
        request: HttpServletRequest,
        @PathVariable itemId: Long,
    ): SessionUserResponse =
        avatarService.purchase(currentUserService.currentUser(request), itemId)

    @PostMapping("/equip")
    fun equip(
        request: HttpServletRequest,
        @RequestBody body: EquipAvatarRequest,
    ): AvatarSummaryResponse =
        avatarService.equip(currentUserService.currentUser(request), body)
}
