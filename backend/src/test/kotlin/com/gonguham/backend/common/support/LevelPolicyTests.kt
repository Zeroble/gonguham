package com.gonguham.backend.common.support

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class LevelPolicyTests {
    private val levelPolicy = LevelPolicy()

    @Test
    fun `level thresholds follow the cumulative growth curve`() {
        assertEquals(1, levelPolicy.levelFor(0))
        assertEquals(1, levelPolicy.levelFor(9))
        assertEquals(2, levelPolicy.levelFor(10))
        assertEquals(3, levelPolicy.levelFor(22))
        assertEquals(4, levelPolicy.levelFor(36))
        assertEquals(5, levelPolicy.levelFor(52))
        assertEquals(6, levelPolicy.levelFor(70))
    }

    @Test
    fun `progress exposes current start next target and remaining checks`() {
        val progress = levelPolicy.progressFor(27)

        assertEquals(3, progress.level)
        assertEquals(22, progress.currentLevelStartTotalChecks)
        assertEquals(36, progress.nextLevelTargetTotalChecks)
        assertEquals(9, progress.remainingChecksToNextLevel)
        assertEquals(36, progress.progressPercent)
    }
}
