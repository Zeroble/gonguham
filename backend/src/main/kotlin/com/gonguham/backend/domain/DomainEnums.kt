package com.gonguham.backend.domain

enum class StudyType {
    MOGAKGONG,
    TOPIC,
    FLASH,
}

enum class RepeatType {
    WEEKLY,
    BIWEEKLY,
    ONCE,
}

enum class LocationType {
    ONLINE,
    OFFLINE,
}

enum class StudyStatus {
    OPEN,
    CLOSED,
}

enum class MembershipRole {
    LEADER,
    MEMBER,
}

enum class MembershipStatus {
    ACTIVE,
    LEFT,
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
}

enum class PostType {
    POST,
    NOTICE,
}

enum class AvatarCategory {
    HAIR,
    TOP,
    BOTTOM,
}

enum class AvatarRarity {
    BASIC,
    POINT,
    SIGNATURE,
}

enum class CheckChangeType {
    EARN,
    SPEND,
}

enum class CheckReason {
    ATTENDANCE,
    ITEM_PURCHASE,
}
