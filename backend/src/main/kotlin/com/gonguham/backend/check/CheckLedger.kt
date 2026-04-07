package com.gonguham.backend.check

import com.gonguham.backend.domain.CheckChangeType
import com.gonguham.backend.domain.CheckReason
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
@Table(name = "check_ledger")
class CheckLedger(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var userId: Long = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var changeType: CheckChangeType = CheckChangeType.EARN,
    @Column(nullable = false)
    var amount: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var reason: CheckReason = CheckReason.ATTENDANCE,
    @Column(nullable = false)
    var refType: String = "",
    var refId: Long? = null,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)

interface CheckLedgerRepository : JpaRepository<CheckLedger, Long> {
    fun existsByUserIdAndReasonAndRefTypeAndRefId(
        userId: Long,
        reason: CheckReason,
        refType: String,
        refId: Long?,
    ): Boolean
}
