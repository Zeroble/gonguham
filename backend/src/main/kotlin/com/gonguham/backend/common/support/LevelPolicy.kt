package com.gonguham.backend.common.support

import org.springframework.stereotype.Component

@Component
class LevelPolicy {
    fun levelFor(totalEarnedChecks: Int): Int = 1 + (totalEarnedChecks / 10)
}
