package com.gonguham.backend.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true)
    var kakaoId: String = "",
    @Column(nullable = false)
    var nickname: String = "",
    var email: String? = null,
    var profileImageUrl: String? = null,
    @Column(nullable = false)
    var totalEarnedChecks: Int = 0,
    @Column(nullable = false)
    var currentChecks: Int = 0,
    @Column(nullable = false)
    var level: Int = 1,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "avatar_profiles")
class AvatarProfile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true)
    var userId: Long = 0,
    var equippedHairItemId: Long? = null,
    var equippedTopItemId: Long? = null,
    var equippedBottomItemId: Long? = null,
)

interface UserRepository : JpaRepository<User, Long> {
    fun findByKakaoId(kakaoId: String): User?
}

interface AvatarProfileRepository : JpaRepository<AvatarProfile, Long> {
    fun findByUserId(userId: Long): AvatarProfile?
}
