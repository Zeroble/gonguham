package com.gonguham.backend.avatar

import com.gonguham.backend.domain.AvatarCategory
import com.gonguham.backend.domain.AvatarRarity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

@Entity
@Table(name = "avatar_items")
class AvatarItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: AvatarCategory = AvatarCategory.HAIR,
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var priceChecks: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var rarity: AvatarRarity = AvatarRarity.BASIC,
    @Column(nullable = false)
    var assetKey: String = "",
    @Column(nullable = false)
    var isDefault: Boolean = false,
)

@Entity
@Table(name = "user_avatar_items")
class UserAvatarItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var userId: Long = 0,
    @Column(nullable = false)
    var avatarItemId: Long = 0,
    @Column(nullable = false)
    var purchasedAt: LocalDateTime = LocalDateTime.now(),
)

interface AvatarItemRepository : JpaRepository<AvatarItem, Long> {
    fun findAllByCategoryOrderByPriceChecksAscNameAsc(category: AvatarCategory): List<AvatarItem>
}

interface UserAvatarItemRepository : JpaRepository<UserAvatarItem, Long> {
    fun findAllByUserId(userId: Long): List<UserAvatarItem>
    fun existsByUserIdAndAvatarItemId(userId: Long, avatarItemId: Long): Boolean
}
