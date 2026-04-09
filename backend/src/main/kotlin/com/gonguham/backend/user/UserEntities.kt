package com.gonguham.backend.user

import com.gonguham.backend.avatar.AvatarAssetCatalog
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
    @Column(nullable = false)
    var bodyAssetKey: String = AvatarAssetCatalog.DEFAULT_BODY_ASSET_KEY,
    @Column(nullable = false)
    var pupilAssetKey: String = AvatarAssetCatalog.DEFAULT_PUPIL_ASSET_KEY,
    @Column(nullable = false)
    var eyebrowAssetKey: String = AvatarAssetCatalog.DEFAULT_EYEBROW_ASSET_KEY,
    @Column(nullable = false)
    var eyelashAssetKey: String = AvatarAssetCatalog.DEFAULT_EYELASH_ASSET_KEY,
    @Column(nullable = false)
    var mouthAssetKey: String = AvatarAssetCatalog.DEFAULT_MOUTH_ASSET_KEY,
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
