package com.gonguham.backend.study

import com.gonguham.backend.domain.PostType
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@SpringBootTest
@Transactional
class StudyPostApiTests @Autowired constructor(
    private val studyService: StudyService,
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
    private val userRepository: com.gonguham.backend.user.UserRepository,
) {
    @Test
    fun `study member can read post detail with comments ordered oldest first`() {
        val post = topicStudyPost()
        val member = userRepository.findById(2L).orElseThrow()
        val now = LocalDateTime.now()

        postCommentRepository.saveAll(
            listOf(
                PostComment(
                    postId = post.id!!,
                    authorUserId = 1L,
                    content = "First reply",
                    createdAt = now.minusMinutes(10),
                    updatedAt = now.minusMinutes(10),
                ),
                PostComment(
                    postId = post.id!!,
                    authorUserId = 2L,
                    content = "Second reply",
                    createdAt = now.minusMinutes(5),
                    updatedAt = now.minusMinutes(5),
                ),
            ),
        )

        val detail = studyService.postDetail(member, post.id!!)

        assertEquals(post.id, detail.postId)
        assertEquals(post.title, detail.title)
        assertEquals(post.content, detail.content)
        assertEquals("First reply", detail.comments[0].content)
        assertEquals("Second reply", detail.comments[1].content)
    }

    @Test
    fun `non member cannot view or comment on another study post`() {
        val post = topicStudyPost()
        val outsider = userRepository.findById(3L).orElseThrow()

        val detailException = assertFailsWith<ResponseStatusException> {
            studyService.postDetail(outsider, post.id!!)
        }
        assertEquals(HttpStatus.FORBIDDEN, detailException.statusCode)

        val createException = assertFailsWith<ResponseStatusException> {
            studyService.createComment(outsider, post.id!!, CreateCommentRequest(content = "Not allowed"))
        }
        assertEquals(HttpStatus.FORBIDDEN, createException.statusCode)
    }

    @Test
    fun `study member can create a comment and read it back`() {
        val post = topicStudyPost()
        val member = userRepository.findById(2L).orElseThrow()

        val created = studyService.createComment(
            member,
            post.id!!,
            CreateCommentRequest(content = "Looking forward to it"),
        )
        val detail = studyService.postDetail(member, post.id!!)

        assertEquals("Looking forward to it", created.content)
        assertEquals(1, detail.comments.size)
        assertEquals("Looking forward to it", detail.comments[0].content)
    }

    private fun topicStudyPost(): Post =
        postRepository.findAllByStudyIdAndTypeOrderByCreatedAtDesc(1L, PostType.POST).first()
}
