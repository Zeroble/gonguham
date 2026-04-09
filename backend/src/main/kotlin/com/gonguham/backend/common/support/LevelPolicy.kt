package com.gonguham.backend.common.support

import org.springframework.stereotype.Component
import kotlin.math.roundToInt

data class LevelProgress(
    val level: Int,
    val currentLevelStartTotalChecks: Int,
    val nextLevelTargetTotalChecks: Int,
    val remainingChecksToNextLevel: Int,
    val progressPercent: Int,
)

@Component
class LevelPolicy {
    fun requiredChecksForNextLevel(level: Int): Int {
        require(level >= 1) { "Level must be at least 1." }
        return 8 + (level * 2)
    }

    fun totalChecksRequiredForLevel(level: Int): Int {
        require(level >= 1) { "Level must be at least 1." }
        return if (level == 1) 0 else (level - 1) * (level + 8)
    }

    fun levelFor(totalEarnedChecks: Int): Int {
        val sanitizedChecks = totalEarnedChecks.coerceAtLeast(0)
        var level = 1

        while (sanitizedChecks >= totalChecksRequiredForLevel(level + 1)) {
            level += 1
        }

        return level
    }

    fun progressFor(totalEarnedChecks: Int): LevelProgress {
        val sanitizedChecks = totalEarnedChecks.coerceAtLeast(0)
        val level = levelFor(sanitizedChecks)
        val currentLevelStartTotalChecks = totalChecksRequiredForLevel(level)
        val nextLevelTargetTotalChecks = totalChecksRequiredForLevel(level + 1)
        val span = (nextLevelTargetTotalChecks - currentLevelStartTotalChecks).coerceAtLeast(1)
        val completedChecks = (sanitizedChecks - currentLevelStartTotalChecks).coerceAtLeast(0)
        val progressPercent = ((completedChecks.toDouble() / span.toDouble()) * 100)
            .roundToInt()
            .coerceIn(0, 100)

        return LevelProgress(
            level = level,
            currentLevelStartTotalChecks = currentLevelStartTotalChecks,
            nextLevelTargetTotalChecks = nextLevelTargetTotalChecks,
            remainingChecksToNextLevel = (nextLevelTargetTotalChecks - sanitizedChecks).coerceAtLeast(0),
            progressPercent = progressPercent,
        )
    }
}
