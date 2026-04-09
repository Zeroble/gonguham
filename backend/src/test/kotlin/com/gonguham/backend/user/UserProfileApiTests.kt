package com.gonguham.backend.user

import com.gonguham.backend.common.support.CurrentUserService
import com.gonguham.backend.support.PostgresIntegrationTest
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.study.PostComment
import com.gonguham.backend.study.PostCommentRepository
import com.gonguham.backend.study.PostRepository
import java.time.LocalDateTime
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileApiTests @Autowired constructor(
    private val mockMvc: MockMvc,
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
    private val userRepository: UserRepository,
) : PostgresIntegrationTest() {
    @Test
    fun `profile endpoint returns level progress stats and avatar data`() {
        mockMvc.perform(
            get("/api/v1/users/1/profile")
                .session(authenticatedSession()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.nickname").isString)
            .andExpect(jsonPath("$.level").value(3))
            .andExpect(jsonPath("$.levelProgress.currentLevelStartTotalChecks").value(22))
            .andExpect(jsonPath("$.levelProgress.nextLevelTargetTotalChecks").value(36))
            .andExpect(jsonPath("$.stats.activeStudyCount").value(2))
            .andExpect(jsonPath("$.stats.currentChecks").value(14))
            .andExpect(jsonPath("$.stats.totalEarnedChecks").value(27))
            .andExpect(jsonPath("$.stats.totalPostCount").value(2))
            .andExpect(jsonPath("$.stats.totalCommentCount").value(0))
            .andExpect(jsonPath("$.avatar.appearance.bodyAssetKey").isString)
    }

    @Test
    fun `study and post responses expose profile lookup ids`() {
        val post = postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(1L, PostType.POST).first()
        val comment = postCommentRepository.save(
            PostComment(
                postId = post.id!!,
                authorUserId = 2L,
                content = "Profile API comment",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
        )

        mockMvc.perform(
            get("/api/v1/studies/1")
                .session(authenticatedSession()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.leaderUserId").value(greaterThan(0)))
            .andExpect(jsonPath("$.posts[0].authorUserId").value(greaterThan(0)))

        mockMvc.perform(
            get("/api/v1/posts/${post.id}")
                .session(authenticatedSession()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authorUserId").value(post.authorUserId))
            .andExpect(jsonPath("$.comments[0].authorUserId").value(comment.authorUserId))
    }

    @Test
    fun `nickname patch validates blank and overlong values and persists success`() {
        mockMvc.perform(
            patch("/api/v1/me")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"   "}"""),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            patch("/api/v1/me")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"${"a".repeat(21)}"}"""),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            patch("/api/v1/me")
                .session(authenticatedSession())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"ProfileTest"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("ProfileTest"))

        val updatedUser = userRepository.findById(1L).orElseThrow()
        org.junit.jupiter.api.Assertions.assertEquals("ProfileTest", updatedUser.nickname)
    }

    private fun authenticatedSession(userId: Long = 1L): MockHttpSession =
        MockHttpSession().apply {
            setAttribute(CurrentUserService.SESSION_KEY, userId)
        }
}
