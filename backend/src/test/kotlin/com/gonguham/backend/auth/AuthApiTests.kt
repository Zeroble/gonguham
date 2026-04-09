package com.gonguham.backend.auth

import com.gonguham.backend.common.Seeder
import com.gonguham.backend.support.PostgresIntegrationTest
import com.gonguham.backend.user.UserRepository
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTests @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
) : PostgresIntegrationTest() {
    @Test
    fun `signup creates user session and default avatar profile`() {
        val result = mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "new-user@gonguham.app",
                      "password": "strongpass123",
                      "nickname": "New User"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("New User"))
            .andReturn()

        val createdUser = userRepository.findByEmail("new-user@gonguham.app")
            ?: error("Expected created user to exist.")
        assertNotEquals("strongpass123", createdUser.passwordHash)

        val session = result.request.session as MockHttpSession

        mockMvc.perform(
            get("/api/v1/me").session(session),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(createdUser.id!!))

        mockMvc.perform(
            get("/api/v1/avatar").session(session),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.appearance.bodyAssetKey").value("body-01"))
    }

    @Test
    fun `signup rejects duplicate email`() {
        mockMvc.perform(
            post("/api/v1/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "leader@gonguham.app",
                      "password": "strongpass123",
                      "nickname": "Duplicate User"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `login rejects incorrect password`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "leader@gonguham.app",
                      "password": "wrong-password"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout invalidates existing session`() {
        val session = loginSession("leader@gonguham.app", Seeder.DEFAULT_PASSWORD)

        mockMvc.perform(
            post("/api/v1/auth/logout").session(session),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/v1/me").session(session),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `me endpoint requires an authenticated session`() {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized)
    }

    private fun loginSession(email: String, password: String): MockHttpSession =
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "$email",
                      "password": "$password"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andReturn()
            .request
            .session as MockHttpSession
}
