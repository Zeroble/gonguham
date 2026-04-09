package com.gonguham.backend.common

import com.gonguham.backend.avatar.AvatarAssetCatalog
import com.gonguham.backend.avatar.AvatarFreeAppearance
import com.gonguham.backend.avatar.AvatarItemRepository
import com.gonguham.backend.avatar.UserAvatarItem
import com.gonguham.backend.avatar.UserAvatarItemRepository
import com.gonguham.backend.check.CheckLedger
import com.gonguham.backend.check.CheckLedgerRepository
import com.gonguham.backend.common.support.LevelPolicy
import com.gonguham.backend.domain.AttendanceStatus
import com.gonguham.backend.domain.CheckChangeType
import com.gonguham.backend.domain.CheckReason
import com.gonguham.backend.domain.LocationType
import com.gonguham.backend.domain.MembershipRole
import com.gonguham.backend.domain.MembershipStatus
import com.gonguham.backend.domain.PostType
import com.gonguham.backend.domain.RepeatType
import com.gonguham.backend.domain.SessionType
import com.gonguham.backend.domain.StudyStatus
import com.gonguham.backend.domain.StudyType
import com.gonguham.backend.study.Attendance
import com.gonguham.backend.study.AttendanceRepository
import com.gonguham.backend.study.Post
import com.gonguham.backend.study.PostComment
import com.gonguham.backend.study.PostCommentRepository
import com.gonguham.backend.study.PostRepository
import com.gonguham.backend.study.SessionParticipation
import com.gonguham.backend.study.SessionParticipationRepository
import com.gonguham.backend.study.Study
import com.gonguham.backend.study.StudyMembership
import com.gonguham.backend.study.StudyMembershipRepository
import com.gonguham.backend.study.StudyRepository
import com.gonguham.backend.study.StudySession
import com.gonguham.backend.study.StudySessionRepository
import com.gonguham.backend.study.StudyTag
import com.gonguham.backend.study.StudyTagRepository
import com.gonguham.backend.user.AvatarProfile
import com.gonguham.backend.user.AvatarProfileRepository
import com.gonguham.backend.user.User
import com.gonguham.backend.user.UserRepository
import jakarta.transaction.Transactional
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class Seeder(
    private val userRepository: UserRepository,
    private val avatarProfileRepository: AvatarProfileRepository,
    private val avatarItemRepository: AvatarItemRepository,
    private val userAvatarItemRepository: UserAvatarItemRepository,
    private val checkLedgerRepository: CheckLedgerRepository,
    private val studyRepository: StudyRepository,
    private val studyTagRepository: StudyTagRepository,
    private val studyMembershipRepository: StudyMembershipRepository,
    private val studySessionRepository: StudySessionRepository,
    private val sessionParticipationRepository: SessionParticipationRepository,
    private val attendanceRepository: AttendanceRepository,
    private val postRepository: PostRepository,
    private val postCommentRepository: PostCommentRepository,
    private val levelPolicy: LevelPolicy,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {
    @Transactional
    override fun run(vararg args: String) {
        if (userRepository.count() > 0L) return

        val now = LocalDateTime.now()
        val defaultAppearance = AvatarAssetCatalog.defaultAppearance()

        val leader = userRepository.save(
            User(
                nickname = "정다솔",
                email = "leader@gonguham.app",
                passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD)!!,
                totalEarnedChecks = 27,
                currentChecks = 14,
                level = levelPolicy.levelFor(27),
                createdAt = now.minusDays(20),
            ),
        )
        val member = userRepository.save(
            User(
                nickname = "김민수",
                email = "member@gonguham.app",
                passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD)!!,
                totalEarnedChecks = 9,
                currentChecks = 9,
                level = levelPolicy.levelFor(9),
                createdAt = now.minusDays(12),
            ),
        )
        val guest = userRepository.save(
            User(
                nickname = "박서연",
                email = "guest@gonguham.app",
                passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD)!!,
                totalEarnedChecks = 4,
                currentChecks = 2,
                level = levelPolicy.levelFor(4),
                createdAt = now.minusDays(4),
            ),
        )

        val items = avatarItemRepository.saveAll(AvatarAssetCatalog.buildSeedItems())

        val defaultHair = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_HAIR_ITEM_KEY }
        val ownedHair = items.first { it.assetKey == "hair-06-a" }
        val defaultTop = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_TOP_ITEM_KEY }
        val ownedTop = items.first { it.assetKey == "top-06" }
        val defaultBottom = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_BOTTOM_ITEM_KEY }
        val ownedBottom = items.first { it.assetKey == "bottom-04" }
        val ownedShoes = items.first { it.assetKey == "shoes-02" }
        val defaultPupil = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_PUPIL_ITEM_KEY }
        val ownedPupil = items.first { it.assetKey == "pupil-03" }
        val defaultEyebrow = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_EYEBROW_ITEM_KEY }
        val ownedEyebrow = items.first { it.assetKey == "eyebrow-02" }
        val defaultEyelash = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_EYELASH_ITEM_KEY }
        val ownedEyelash = items.first { it.assetKey == "eyelash-02" }
        val defaultMouth = items.first { it.assetKey == AvatarAssetCatalog.DEFAULT_MOUTH_ITEM_KEY }
        val ownedMouth = items.first { it.assetKey == "mouth-04" }
        val avatarDefaults = AvatarDefaults(
            hair = defaultHair,
            top = defaultTop,
            bottom = defaultBottom,
            pupil = defaultPupil,
            eyebrow = defaultEyebrow,
            eyelash = defaultEyelash,
            mouth = defaultMouth,
        )

        userAvatarItemRepository.saveAll(
            listOf(
                defaultHair,
                ownedHair,
                defaultTop,
                ownedTop,
                defaultBottom,
                ownedBottom,
                ownedShoes,
                defaultPupil,
                ownedPupil,
                defaultEyebrow,
                ownedEyebrow,
                defaultEyelash,
                ownedEyelash,
                defaultMouth,
                ownedMouth,
            ).distinctBy { it.id }.map {
                UserAvatarItem(userId = leader.id!!, avatarItemId = it.id!!, purchasedAt = now.minusDays(2))
            },
        )
        userAvatarItemRepository.saveAll(
            listOf(
                defaultHair,
                defaultTop,
                defaultBottom,
                defaultPupil,
                defaultEyebrow,
                defaultEyelash,
                defaultMouth,
            ).map {
                UserAvatarItem(userId = member.id!!, avatarItemId = it.id!!, purchasedAt = now.minusDays(10))
            },
        )

        avatarProfileRepository.save(
            AvatarProfile(
                userId = leader.id!!,
                bodyAssetKey = "body-05",
                pupilAssetKey = "pupil-03",
                eyebrowAssetKey = "eyebrow-02",
                eyelashAssetKey = "eyelash-02",
                mouthAssetKey = "mouth-04",
                equippedHairItemId = ownedHair.id,
                equippedTopItemId = ownedTop.id,
                equippedBottomItemId = ownedBottom.id,
                equippedShoesItemId = ownedShoes.id,
                equippedPupilItemId = ownedPupil.id,
                equippedEyebrowItemId = ownedEyebrow.id,
                equippedEyelashItemId = ownedEyelash.id,
                equippedMouthItemId = ownedMouth.id,
            ),
        )
        avatarProfileRepository.save(
            AvatarProfile(
                userId = member.id!!,
                bodyAssetKey = defaultAppearance.bodyAssetKey,
                pupilAssetKey = defaultAppearance.pupilAssetKey,
                eyebrowAssetKey = defaultAppearance.eyebrowAssetKey,
                eyelashAssetKey = defaultAppearance.eyelashAssetKey,
                mouthAssetKey = defaultAppearance.mouthAssetKey,
                equippedHairItemId = defaultHair.id,
                equippedTopItemId = defaultTop.id,
                equippedBottomItemId = defaultBottom.id,
                equippedPupilItemId = defaultPupil.id,
                equippedEyebrowItemId = defaultEyebrow.id,
                equippedEyelashItemId = defaultEyelash.id,
                equippedMouthItemId = defaultMouth.id,
            ),
        )

        val topicStudy = studyRepository.save(
            Study(
                leaderUserId = leader.id!!,
                type = StudyType.TOPIC,
                title = "자료구조 같이 끝내는 주제 스터디",
                description = "연결 리스트부터 그래프까지 개념과 문제 풀이를 함께 가져갑니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                startTime = LocalTime.of(18, 30),
                endTime = LocalTime.of(20, 0),
                startDate = LocalDate.now().minusWeeks(3),
                endDate = LocalDate.now().plusWeeks(5),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 8,
                locationType = LocationType.OFFLINE,
                locationText = "새천년관 3층 세미나실 B",
                rulesText = "결석 시 하루 전까지 게시판에 알려주세요.",
                suppliesText = "노트북, 교재 5장, 필기 도구",
                cautionText = "스터디 자료는 내부 공유를 원칙으로 해요.",
                status = StudyStatus.OPEN,
                createdAt = now.minusWeeks(4),
            ),
        )
        val mogakStudy = studyRepository.save(
            Study(
                leaderUserId = guest.id!!,
                type = StudyType.MOGAKGONG,
                title = "도서관 저녁 모각공",
                description = "각자 할 일을 들고 와서 2시간 집중하고 마지막 10분만 공유해요.",
                daysOfWeek = mutableSetOf(DayOfWeek.WEDNESDAY),
                startTime = LocalTime.of(19, 0),
                endTime = LocalTime.of(21, 0),
                startDate = LocalDate.now().minusWeeks(1),
                endDate = LocalDate.now().plusWeeks(6),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 10,
                locationType = LocationType.OFFLINE,
                locationText = "중앙도서관 2층",
                rulesText = "서로의 몰입을 방해하지 않기",
                suppliesText = "개인 할 일, 이어폰",
                cautionText = "중간 입퇴실 자유",
                status = StudyStatus.OPEN,
                createdAt = now.minusWeeks(2),
            ),
        )
        val flashStudy = studyRepository.save(
            Study(
                leaderUserId = member.id!!,
                type = StudyType.FLASH,
                title = "사용자 조사 짧은 실습반",
                description = "인터뷰 질문 짜기부터 인사이트 정리까지 빠르게 실습해봅니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.FRIDAY),
                startTime = LocalTime.of(17, 0),
                endTime = LocalTime.of(18, 30),
                startDate = LocalDate.now().plusDays(3),
                endDate = LocalDate.now().plusDays(3),
                repeatType = RepeatType.ONCE,
                maxMembers = 6,
                locationType = LocationType.ONLINE,
                locationText = "구글 밋",
                rulesText = "서로 인터뷰 연습 파트너가 되어주기",
                suppliesText = "노트북, 메모 앱",
                cautionText = "회차 1회짜리 반짝 스터디",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(3),
            ),
        )

        studyTagRepository.saveAll(
            listOf(
                StudyTag(studyId = topicStudy.id!!, name = "컴공"),
                StudyTag(studyId = topicStudy.id!!, name = "자료구조"),
                StudyTag(studyId = mogakStudy.id!!, name = "집중"),
                StudyTag(studyId = mogakStudy.id!!, name = "루틴"),
                StudyTag(studyId = flashStudy.id!!, name = "사용자조사"),
                StudyTag(studyId = flashStudy.id!!, name = "실습"),
            ),
        )

        studyMembershipRepository.saveAll(
            listOf(
                StudyMembership(studyId = topicStudy.id!!, userId = leader.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusWeeks(4)),
                StudyMembership(studyId = topicStudy.id!!, userId = member.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusWeeks(2)),
                StudyMembership(studyId = mogakStudy.id!!, userId = guest.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusWeeks(2)),
                StudyMembership(studyId = mogakStudy.id!!, userId = leader.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(10)),
                StudyMembership(studyId = flashStudy.id!!, userId = member.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(3)),
            ),
        )

        val topicSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = topicStudy.id!!, sessionNo = 8, title = "이진 탐색 트리", scheduledAt = now.minusWeeks(2), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 9, title = "그래프 BFS/DFS", scheduledAt = now.minusWeeks(1), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 10, title = "힙과 우선순위 큐", scheduledAt = now.plusDays(1), placeText = topicStudy.locationText, noticeText = "오늘 스터디는 새천년관 3층 세미나실 B에서 진행해요."),
                StudySession(studyId = topicStudy.id!!, sessionNo = 11, title = "쉬어가는 회차", scheduledAt = now.plusWeeks(1), sessionType = SessionType.BREAK, placeText = topicStudy.locationText),
            ),
        )
        val extraTopicSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = topicStudy.id!!, sessionNo = 6, title = "배열과 리스트 복습", scheduledAt = now.minusWeeks(4), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 7, title = "스택과 큐 문제 풀이", scheduledAt = now.minusWeeks(3), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 12, title = "동적 계획법 입문", scheduledAt = now.plusWeeks(2), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 13, title = "트리와 힙 정리", scheduledAt = now.plusWeeks(3), placeText = topicStudy.locationText),
            ),
        )

        val mogakSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = mogakStudy.id!!, sessionNo = 1, title = "첫 집중 루틴 세팅", scheduledAt = now.minusWeeks(2), placeText = mogakStudy.locationText),
                StudySession(studyId = mogakStudy.id!!, sessionNo = 2, title = "중간 점검과 할 일 정리", scheduledAt = now.minusWeeks(1), placeText = mogakStudy.locationText),
                StudySession(studyId = mogakStudy.id!!, sessionNo = 3, title = "오늘 할 일 공유", scheduledAt = now.plusDays(2), placeText = mogakStudy.locationText),
                StudySession(studyId = mogakStudy.id!!, sessionNo = 4, title = "루틴 유지 모각공", scheduledAt = now.plusWeeks(1), placeText = mogakStudy.locationText),
                StudySession(studyId = mogakStudy.id!!, sessionNo = 5, title = "시험 전 마감 집중", scheduledAt = now.plusWeeks(2), placeText = mogakStudy.locationText),
            ),
        )
        studySessionRepository.save(
            StudySession(studyId = flashStudy.id!!, sessionNo = 1, title = "인터뷰 질문 설계", scheduledAt = now.plusDays(3), placeText = flashStudy.locationText),
        )

        val earlyTopicSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = topicStudy.id!!, sessionNo = 1, title = "OT 및 스터디 세팅", scheduledAt = now.minusWeeks(9), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 2, title = "배열과 리스트 워밍업", scheduledAt = now.minusWeeks(8), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 3, title = "스택과 큐 훈련", scheduledAt = now.minusWeeks(7), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 4, title = "트리 순회 기초", scheduledAt = now.minusWeeks(6), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 5, title = "그래프 입문 연습", scheduledAt = now.minusWeeks(5), placeText = topicStudy.locationText),
            ),
        )

        val upcomingSession = topicSessions.first { it.sessionNo == 10 }
        val completedSession = topicSessions.first { it.sessionNo == 9 }
        val olderCompletedSession = topicSessions.first { it.sessionNo == 8 }
        val archiveSession = extraTopicSessions.first { it.sessionNo == 6 }
        val missedSession = extraTopicSessions.first { it.sessionNo == 7 }
        val futurePlannedSession = extraTopicSessions.first { it.sessionNo == 12 }
        val kickoffSession = earlyTopicSessions.first { it.sessionNo == 1 }
        val secondSession = earlyTopicSessions.first { it.sessionNo == 2 }
        val drillSession = earlyTopicSessions.first { it.sessionNo == 3 }
        val fourthSession = earlyTopicSessions.first { it.sessionNo == 4 }
        val graphIntroSession = earlyTopicSessions.first { it.sessionNo == 5 }
        val mogakArchiveSession = mogakSessions.first { it.sessionNo == 1 }
        val mogakCompletedSession = mogakSessions.first { it.sessionNo == 2 }
        val mogakCurrentSession = mogakSessions.first { it.sessionNo == 3 }
        val mogakFutureSession = mogakSessions.first { it.sessionNo == 4 }

        studySessionRepository.saveAll(
            listOf(
                kickoffSession,
                secondSession,
                drillSession,
                fourthSession,
                graphIntroSession,
                archiveSession,
                missedSession,
                olderCompletedSession,
                completedSession,
            ).onEach { it.attendanceClosedAt = now.minusMinutes(30) },
        )
        studySessionRepository.saveAll(
            listOf(
                mogakArchiveSession,
                mogakCompletedSession,
            ).onEach { it.attendanceClosedAt = now.minusMinutes(30) },
        )

        sessionParticipationRepository.saveAll(
            listOf(
                SessionParticipation(sessionId = upcomingSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusHours(5)),
                SessionParticipation(sessionId = upcomingSession.id!!, userId = member.id!!, planned = true, updatedAt = now.minusHours(3)),
                SessionParticipation(sessionId = futurePlannedSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusHours(1)),
                SessionParticipation(sessionId = kickoffSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(9)),
                SessionParticipation(sessionId = drillSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(7)),
                SessionParticipation(sessionId = drillSession.id!!, userId = member.id!!, planned = true, updatedAt = now.minusWeeks(7)),
                SessionParticipation(sessionId = graphIntroSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(5)),
                SessionParticipation(sessionId = completedSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(1)),
                SessionParticipation(sessionId = completedSession.id!!, userId = member.id!!, planned = true, updatedAt = now.minusWeeks(1)),
                SessionParticipation(sessionId = olderCompletedSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(2)),
                SessionParticipation(sessionId = olderCompletedSession.id!!, userId = member.id!!, planned = true, updatedAt = now.minusWeeks(2)),
                SessionParticipation(sessionId = archiveSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(4)),
                SessionParticipation(sessionId = missedSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(3)),
                SessionParticipation(sessionId = mogakArchiveSession.id!!, userId = guest.id!!, planned = true, updatedAt = now.minusWeeks(2)),
                SessionParticipation(sessionId = mogakArchiveSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusWeeks(2)),
                SessionParticipation(sessionId = mogakCompletedSession.id!!, userId = guest.id!!, planned = true, updatedAt = now.minusWeeks(1)),
                SessionParticipation(sessionId = mogakCompletedSession.id!!, userId = leader.id!!, planned = false, updatedAt = now.minusWeeks(1)),
                SessionParticipation(sessionId = mogakCurrentSession.id!!, userId = guest.id!!, planned = true, updatedAt = now.minusHours(8)),
                SessionParticipation(sessionId = mogakCurrentSession.id!!, userId = leader.id!!, planned = true, updatedAt = now.minusHours(2)),
                SessionParticipation(sessionId = mogakFutureSession.id!!, userId = guest.id!!, planned = true, updatedAt = now.minusHours(1)),
            ),
        )

        attendanceRepository.saveAll(
            listOf(
                Attendance(sessionId = kickoffSession.id!!, userId = leader.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(9)),
                Attendance(sessionId = drillSession.id!!, userId = leader.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(7)),
                Attendance(sessionId = drillSession.id!!, userId = member.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(7)),
                Attendance(sessionId = graphIntroSession.id!!, userId = leader.id!!, status = AttendanceStatus.ABSENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(5)),
                Attendance(sessionId = archiveSession.id!!, userId = leader.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(4)),
                Attendance(sessionId = olderCompletedSession.id!!, userId = leader.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(2)),
                Attendance(sessionId = olderCompletedSession.id!!, userId = member.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(2)),
                Attendance(sessionId = missedSession.id!!, userId = leader.id!!, status = AttendanceStatus.ABSENT, checkedByUserId = leader.id!!, checkedAt = now.minusWeeks(3)),
                Attendance(sessionId = completedSession.id!!, userId = leader.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusDays(6)),
                Attendance(sessionId = completedSession.id!!, userId = member.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = leader.id!!, checkedAt = now.minusDays(6)),
                Attendance(sessionId = mogakArchiveSession.id!!, userId = guest.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = guest.id!!, checkedAt = now.minusWeeks(2)),
                Attendance(sessionId = mogakArchiveSession.id!!, userId = leader.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = guest.id!!, checkedAt = now.minusWeeks(2)),
                Attendance(sessionId = mogakCompletedSession.id!!, userId = guest.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = guest.id!!, checkedAt = now.minusWeeks(1)),
                Attendance(sessionId = mogakCompletedSession.id!!, userId = leader.id!!, status = AttendanceStatus.ABSENT, checkedByUserId = guest.id!!, checkedAt = now.minusWeeks(1)),
            ),
        )

        checkLedgerRepository.saveAll(
            listOf(
                CheckLedger(userId = leader.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = completedSession.id!!, createdAt = now.minusDays(6)),
                CheckLedger(userId = member.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = completedSession.id!!, createdAt = now.minusDays(6)),
                CheckLedger(userId = leader.id!!, changeType = CheckChangeType.SPEND, amount = -5, reason = CheckReason.ITEM_PURCHASE, refType = "ITEM", refId = ownedTop.id!!, createdAt = now.minusDays(2)),
            ),
        )

        postRepository.saveAll(
            listOf(
                Post(studyId = topicStudy.id!!, authorUserId = leader.id!!, type = PostType.NOTICE, title = "오늘 스터디 안내", content = "오늘 스터디는 새천년관 3층 세미나실 B에서 진행해요. 실습 자료는 아래 게시글 링크에서 확인해주세요.", createdAt = now.minusHours(8), updatedAt = now.minusHours(8)),
                Post(studyId = topicStudy.id!!, authorUserId = member.id!!, type = PostType.POST, title = "오늘 스터디 끝나고 남아서 같이 복습하실 분 있나요?", content = "한 시간 정도 더 남아서 문제 같이 풀어보실 분 구합니다.", createdAt = now.minusHours(4), updatedAt = now.minusHours(4)),
                Post(studyId = topicStudy.id!!, authorUserId = leader.id!!, type = PostType.POST, title = "지난 회차 정리본 공유", content = "그래프 BFS/DFS 요약 노트 업로드했습니다.", createdAt = now.minusDays(1), updatedAt = now.minusDays(1)),
            ),
        )
        seedExtendedData(
            now = now,
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
            items = items,
        )
    }

    private fun seedExtendedData(
        now: LocalDateTime,
        defaultAppearance: AvatarFreeAppearance,
        avatarDefaults: AvatarDefaults,
        items: List<com.gonguham.backend.avatar.AvatarItem>,
    ) {
        val hana = createSeedUser(
            email = "hana@gonguham.app",
            nickname = "하나",
            totalEarnedChecks = 31,
            currentChecks = 18,
            createdAt = now.minusDays(28),
            bodyAssetKey = "body-08",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )
        val minho = createSeedUser(
            email = "minho@gonguham.app",
            nickname = "민호",
            totalEarnedChecks = 22,
            currentChecks = 11,
            createdAt = now.minusDays(24),
            bodyAssetKey = "body-12",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )
        val sora = createSeedUser(
            email = "sora@gonguham.app",
            nickname = "소라",
            totalEarnedChecks = 17,
            currentChecks = 9,
            createdAt = now.minusDays(19),
            bodyAssetKey = "body-16",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )
        val daniel = createSeedUser(
            email = "daniel@gonguham.app",
            nickname = "도윤",
            totalEarnedChecks = 14,
            currentChecks = 6,
            createdAt = now.minusDays(15),
            bodyAssetKey = "body-20",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )
        val yuna = createSeedUser(
            email = "yuna@gonguham.app",
            nickname = "유나",
            totalEarnedChecks = 11,
            currentChecks = 7,
            createdAt = now.minusDays(10),
            bodyAssetKey = "body-24",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )
        val jihun = createSeedUser(
            email = "jihun@gonguham.app",
            nickname = "지훈",
            totalEarnedChecks = 26,
            currentChecks = 13,
            createdAt = now.minusDays(21),
            bodyAssetKey = "body-11",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )
        val eunchae = createSeedUser(
            email = "eunchae@gonguham.app",
            nickname = "은채",
            totalEarnedChecks = 19,
            currentChecks = 8,
            createdAt = now.minusDays(17),
            bodyAssetKey = "body-14",
            defaultAppearance = defaultAppearance,
            avatarDefaults = avatarDefaults,
        )

        val featuredHair = items.first { it.assetKey == "hair-03-c" }
        val featuredTop = items.first { it.assetKey == "top-03" }
        val featuredShoes = items.first { it.assetKey == "shoes-01" }
        userAvatarItemRepository.saveAll(
            listOf(featuredHair, featuredTop, featuredShoes).map { avatarItem ->
                UserAvatarItem(
                    userId = hana.id!!,
                    avatarItemId = avatarItem.id!!,
                    purchasedAt = now.minusDays(7),
                )
            },
        )
        avatarProfileRepository.findByUserId(hana.id!!)?.let { profile ->
            profile.equippedHairItemId = featuredHair.id
            profile.equippedTopItemId = featuredTop.id
            profile.equippedShoesItemId = featuredShoes.id
            avatarProfileRepository.save(profile)
        }

        val backendLab = studyRepository.save(
            Study(
                leaderUserId = hana.id!!,
                type = StudyType.TOPIC,
                title = "백엔드 리팩터링 연구실",
                description = "매주 실제 스프링 기능을 가져와 더 읽기 쉽고 안전한 코드로 다듬는 스터디입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                startTime = LocalTime.of(19, 30),
                endTime = LocalTime.of(21, 30),
                startDate = LocalDate.now().minusWeeks(2),
                endDate = LocalDate.now().plusWeeks(6),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 6,
                locationType = LocationType.OFFLINE,
                locationText = "강남 스터디룸 A",
                rulesText = "이번 주 발표 담당자는 모임 전까지 초안 PR이나 코드 조각을 꼭 공유합니다.",
                suppliesText = "노트북, 리뷰 메모, 함께 볼 로그나 쿼리 결과를 준비해 주세요.",
                cautionText = "정시에 시작하고, 첫 15분은 가벼운 근황 공유만 진행합니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(18),
            ),
        )
        val sprintClub = studyRepository.save(
            Study(
                leaderUserId = minho.id!!,
                type = StudyType.MOGAKGONG,
                title = "토요 알고리즘 스프린트",
                description = "조용한 몰입 시간과 실전 모의테스트, 짧은 회고로 리듬을 만드는 주말 알고리즘 스터디입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.SATURDAY),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(12, 30),
                startDate = LocalDate.now().minusWeeks(3),
                endDate = LocalDate.now().plusWeeks(5),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 8,
                locationType = LocationType.OFFLINE,
                locationText = "신촌 스터디 라운지",
                rulesText = "휴대폰은 무음, 소리 재생 금지, 모의테스트는 10시 30분 정각에 시작합니다.",
                suppliesText = "노트, 펜, 물, 이번 주에 막힌 문제 하나를 가져와 주세요.",
                cautionText = "지각하면 첫 집중 블록은 건너뛰고 두 번째 블록부터 합류합니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(16),
            ),
        )
        val designCircle = studyRepository.save(
            Study(
                leaderUserId = sora.id!!,
                type = StudyType.TOPIC,
                title = "프로덕트 디자인 독서모임",
                description = "디자인 책을 함께 읽고 실무에 바로 써먹을 수 있는 크리틱 질문으로 연결하는 모임입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.TUESDAY),
                startTime = LocalTime.of(20, 0),
                endTime = LocalTime.of(21, 30),
                startDate = LocalDate.now().minusWeeks(1),
                endDate = LocalDate.now().plusWeeks(7),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 5,
                locationType = LocationType.ONLINE,
                locationText = "줌",
                rulesText = "지정 챕터를 읽고, 같이 볼 화면 예시를 하나씩 준비합니다.",
                suppliesText = "스크린샷, 메모, 읽으면서 체크한 포인트를 가져와 주세요.",
                cautionText = "읽기 시간에는 즉석 리디자인을 하지 않고 워크숍 시간에만 진행합니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(12),
            ),
        )
        val portfolioClub = studyRepository.save(
            Study(
                leaderUserId = daniel.id!!,
                type = StudyType.FLASH,
                title = "포트폴리오 피드백 클럽",
                description = "짧고 밀도 있게 포트폴리오 피드백을 주고받는 취업 준비 스터디입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.FRIDAY),
                startTime = LocalTime.of(19, 0),
                endTime = LocalTime.of(20, 30),
                startDate = LocalDate.now().plusDays(2),
                endDate = LocalDate.now().plusWeeks(4),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 5,
                locationType = LocationType.ONLINE,
                locationText = "구글 밋",
                rulesText = "가장 자신 있는 케이스 스터디 하나와 가장 고민되는 지점을 같이 공유합니다.",
                suppliesText = "포트폴리오 링크, 지원 직무, 최근 지원 메모를 준비해 주세요.",
                cautionText = "피드백은 솔직하게 하되 구체적으로, 한 사람당 시간을 길게 끌지 않습니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(5),
            ),
        )
        val onboardingLab = studyRepository.save(
            Study(
                leaderUserId = yuna.id!!,
                type = StudyType.TOPIC,
                title = "모바일 온보딩 해부 모임",
                description = "회원가입과 첫 사용 경험을 뜯어보며 이탈 포인트를 줄이는 방법을 함께 실험하는 스터디입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.MONDAY),
                startTime = LocalTime.of(20, 0),
                endTime = LocalTime.of(21, 30),
                startDate = LocalDate.now().minusWeeks(1),
                endDate = LocalDate.now().plusWeeks(6),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 6,
                locationType = LocationType.ONLINE,
                locationText = "디스코드",
                rulesText = "매주 한 서비스의 온보딩 화면을 가져와 개선 포인트를 세 가지 이상 정리합니다.",
                suppliesText = "캡처 화면, 가입 흐름 메모, 인상 깊었던 문구를 준비해 주세요.",
                cautionText = "비판보다 관찰을 먼저 하고, 해결책 제안은 근거와 함께 정리합니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(9),
            ),
        )
        val interviewClinic = studyRepository.save(
            Study(
                leaderUserId = jihun.id!!,
                type = StudyType.FLASH,
                title = "기술면접 말하기 클리닉",
                description = "짧은 답변을 또렷하게 말하는 연습과 꼬리 질문 대응을 함께 다듬는 취업 준비 스터디입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.THURSDAY),
                startTime = LocalTime.of(21, 0),
                endTime = LocalTime.of(22, 20),
                startDate = LocalDate.now().minusWeeks(1),
                endDate = LocalDate.now().plusWeeks(5),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 6,
                locationType = LocationType.ONLINE,
                locationText = "구글 밋",
                rulesText = "답변은 90초 안에 마무리하고, 피드백은 바로 한 줄 요약으로 남깁니다.",
                suppliesText = "자기소개, 프로젝트 트러블슈팅 사례, 자주 막히는 질문 한 개를 준비해 주세요.",
                cautionText = "답을 길게 늘이지 않고, 핵심 문장부터 먼저 말하는 연습에 집중합니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(8),
            ),
        )
        val paperClub = studyRepository.save(
            Study(
                leaderUserId = eunchae.id!!,
                type = StudyType.TOPIC,
                title = "아침 논문 읽기 모임",
                description = "출근 전 1시간 동안 논문 초록과 실험 설계를 빠르게 읽고 핵심만 남기는 가벼운 아침 스터디입니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.WEDNESDAY),
                startTime = LocalTime.of(7, 30),
                endTime = LocalTime.of(8, 40),
                startDate = LocalDate.now().minusWeeks(2),
                endDate = LocalDate.now().plusWeeks(8),
                repeatType = RepeatType.WEEKLY,
                maxMembers = 5,
                locationType = LocationType.ONLINE,
                locationText = "줌",
                rulesText = "논문 전체를 번역하려 하지 않고, 오늘의 질문 하나에 답하는 데 집중합니다.",
                suppliesText = "초록 하이라이트, 궁금한 용어, 다시 읽고 싶은 그림 한 장을 준비해 주세요.",
                cautionText = "아침 스터디라 녹화는 하지 않고 정시 시작, 정시 종료를 지킵니다.",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(11),
            ),
        )

        studyTagRepository.saveAll(
            listOf(
                StudyTag(studyId = backendLab.id!!, name = "백엔드"),
                StudyTag(studyId = backendLab.id!!, name = "스프링"),
                StudyTag(studyId = backendLab.id!!, name = "리팩터링"),
                StudyTag(studyId = sprintClub.id!!, name = "알고리즘"),
                StudyTag(studyId = sprintClub.id!!, name = "몰입"),
                StudyTag(studyId = designCircle.id!!, name = "디자인"),
                StudyTag(studyId = designCircle.id!!, name = "독서"),
                StudyTag(studyId = portfolioClub.id!!, name = "취업"),
                StudyTag(studyId = portfolioClub.id!!, name = "포트폴리오"),
                StudyTag(studyId = onboardingLab.id!!, name = "온보딩"),
                StudyTag(studyId = onboardingLab.id!!, name = "UX"),
                StudyTag(studyId = onboardingLab.id!!, name = "모바일"),
                StudyTag(studyId = interviewClinic.id!!, name = "면접"),
                StudyTag(studyId = interviewClinic.id!!, name = "말하기"),
                StudyTag(studyId = interviewClinic.id!!, name = "취업"),
                StudyTag(studyId = paperClub.id!!, name = "논문"),
                StudyTag(studyId = paperClub.id!!, name = "아침"),
                StudyTag(studyId = paperClub.id!!, name = "읽기"),
            ),
        )

        studyMembershipRepository.saveAll(
            listOf(
                StudyMembership(studyId = backendLab.id!!, userId = hana.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(18)),
                StudyMembership(studyId = backendLab.id!!, userId = minho.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(17)),
                StudyMembership(studyId = backendLab.id!!, userId = sora.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(16)),
                StudyMembership(studyId = sprintClub.id!!, userId = minho.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(16)),
                StudyMembership(studyId = sprintClub.id!!, userId = hana.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(15)),
                StudyMembership(studyId = sprintClub.id!!, userId = daniel.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(15)),
                StudyMembership(studyId = sprintClub.id!!, userId = yuna.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(14)),
                StudyMembership(studyId = designCircle.id!!, userId = sora.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(12)),
                StudyMembership(studyId = designCircle.id!!, userId = hana.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(11)),
                StudyMembership(studyId = designCircle.id!!, userId = yuna.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(11)),
                StudyMembership(studyId = portfolioClub.id!!, userId = daniel.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(5)),
                StudyMembership(studyId = portfolioClub.id!!, userId = minho.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(4)),
                StudyMembership(studyId = portfolioClub.id!!, userId = yuna.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(4)),
                StudyMembership(studyId = onboardingLab.id!!, userId = yuna.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(9)),
                StudyMembership(studyId = onboardingLab.id!!, userId = sora.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(8)),
                StudyMembership(studyId = onboardingLab.id!!, userId = eunchae.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(8)),
                StudyMembership(studyId = onboardingLab.id!!, userId = hana.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(7)),
                StudyMembership(studyId = interviewClinic.id!!, userId = jihun.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(8)),
                StudyMembership(studyId = interviewClinic.id!!, userId = minho.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(7)),
                StudyMembership(studyId = interviewClinic.id!!, userId = daniel.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(7)),
                StudyMembership(studyId = interviewClinic.id!!, userId = yuna.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(6)),
                StudyMembership(studyId = paperClub.id!!, userId = eunchae.id!!, role = MembershipRole.LEADER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(11)),
                StudyMembership(studyId = paperClub.id!!, userId = hana.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(10)),
                StudyMembership(studyId = paperClub.id!!, userId = jihun.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(10)),
                StudyMembership(studyId = paperClub.id!!, userId = sora.id!!, role = MembershipRole.MEMBER, status = MembershipStatus.ACTIVE, joinedAt = now.minusDays(9)),
            ),
        )

        val backendLabSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = backendLab.id!!, sessionNo = 1, title = "서비스 경계 점검", scheduledAt = now.minusDays(14).withHour(19).withMinute(30).withSecond(0).withNano(0), placeText = backendLab.locationText),
                StudySession(studyId = backendLab.id!!, sessionNo = 2, title = "쿼리 플랜 클리닉", scheduledAt = now.minusDays(9).withHour(19).withMinute(30).withSecond(0).withNano(0), placeText = backendLab.locationText),
                StudySession(studyId = backendLab.id!!, sessionNo = 3, title = "에러 응답 정리", scheduledAt = now.plusDays(1).withHour(19).withMinute(30).withSecond(0).withNano(0), placeText = backendLab.locationText, noticeText = "단순화하고 싶은 엔드포인트 하나를 가져와 주세요."),
                StudySession(studyId = backendLab.id!!, sessionNo = 4, title = "캐시 실험 공유", scheduledAt = now.plusDays(8).withHour(19).withMinute(30).withSecond(0).withNano(0), placeText = backendLab.locationText),
            ),
        )
        val sprintClubSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = sprintClub.id!!, sessionNo = 1, title = "몸풀기 세트와 페이스 체크", scheduledAt = now.minusDays(20).withHour(9).withMinute(0).withSecond(0).withNano(0), placeText = sprintClub.locationText),
                StudySession(studyId = sprintClub.id!!, sessionNo = 2, title = "그리디 모의테스트", scheduledAt = now.minusDays(13).withHour(9).withMinute(0).withSecond(0).withNano(0), placeText = sprintClub.locationText),
                StudySession(studyId = sprintClub.id!!, sessionNo = 3, title = "DP 집중 블록", scheduledAt = now.plusDays(3).withHour(9).withMinute(0).withSecond(0).withNano(0), placeText = sprintClub.locationText, noticeText = "첫 45분은 각자 문제 풀이에 몰입합니다."),
                StudySession(studyId = sprintClub.id!!, sessionNo = 4, title = "휴식 주간", scheduledAt = now.plusDays(10).withHour(9).withMinute(0).withSecond(0).withNano(0), sessionType = SessionType.BREAK, placeText = sprintClub.locationText),
            ),
        )
        val designCircleSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = designCircle.id!!, sessionNo = 1, title = "독서 노트 싱크", scheduledAt = now.minusDays(7).withHour(20).withMinute(0).withSecond(0).withNano(0), placeText = designCircle.locationText),
                StudySession(studyId = designCircle.id!!, sessionNo = 2, title = "크리틱 질문 워크숍", scheduledAt = now.plusDays(4).withHour(20).withMinute(0).withSecond(0).withNano(0), placeText = designCircle.locationText),
                StudySession(studyId = designCircle.id!!, sessionNo = 3, title = "휴리스틱 뜯어보기", scheduledAt = now.plusDays(11).withHour(20).withMinute(0).withSecond(0).withNano(0), placeText = designCircle.locationText),
            ),
        )
        val portfolioClubSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = portfolioClub.id!!, sessionNo = 1, title = "케이스 스터디 피드백 루프", scheduledAt = now.plusDays(2).withHour(19).withMinute(0).withSecond(0).withNano(0), placeText = portfolioClub.locationText),
                StudySession(studyId = portfolioClub.id!!, sessionNo = 2, title = "이력서-포트폴리오 정렬", scheduledAt = now.plusDays(9).withHour(19).withMinute(0).withSecond(0).withNano(0), placeText = portfolioClub.locationText),
                StudySession(studyId = portfolioClub.id!!, sessionNo = 3, title = "오퍼 회고 미니 세션", scheduledAt = now.plusDays(16).withHour(19).withMinute(0).withSecond(0).withNano(0), placeText = portfolioClub.locationText),
            ),
        )
        val onboardingLabSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = onboardingLab.id!!, sessionNo = 1, title = "첫 화면 이탈 포인트 수집", scheduledAt = now.minusDays(5).withHour(20).withMinute(0).withSecond(0).withNano(0), placeText = onboardingLab.locationText),
                StudySession(studyId = onboardingLab.id!!, sessionNo = 2, title = "가입 플로우 문장 다듬기", scheduledAt = now.plusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0), placeText = onboardingLab.locationText, noticeText = "최근 써본 앱 하나의 회원가입 화면을 캡처해 와 주세요."),
                StudySession(studyId = onboardingLab.id!!, sessionNo = 3, title = "실패 경험 줄이는 패턴", scheduledAt = now.plusDays(9).withHour(20).withMinute(0).withSecond(0).withNano(0), placeText = onboardingLab.locationText),
            ),
        )
        val interviewClinicSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = interviewClinic.id!!, sessionNo = 1, title = "자기소개 90초 압축", scheduledAt = now.minusDays(6).withHour(21).withMinute(0).withSecond(0).withNano(0), placeText = interviewClinic.locationText),
                StudySession(studyId = interviewClinic.id!!, sessionNo = 2, title = "트러블슈팅 말하기", scheduledAt = now.plusDays(1).withHour(21).withMinute(0).withSecond(0).withNano(0), placeText = interviewClinic.locationText, noticeText = "가장 설명하기 어려웠던 프로젝트 상황을 짧게 정리해 와 주세요."),
                StudySession(studyId = interviewClinic.id!!, sessionNo = 3, title = "협업 경험 질문 실전", scheduledAt = now.plusDays(8).withHour(21).withMinute(0).withSecond(0).withNano(0), placeText = interviewClinic.locationText),
            ),
        )
        val paperClubSessions = studySessionRepository.saveAll(
            listOf(
                StudySession(studyId = paperClub.id!!, sessionNo = 1, title = "추천 시스템 논문 훑기", scheduledAt = now.minusDays(15).withHour(7).withMinute(30).withSecond(0).withNano(0), placeText = paperClub.locationText),
                StudySession(studyId = paperClub.id!!, sessionNo = 2, title = "초록만으로 핵심 잡기", scheduledAt = now.minusDays(8).withHour(7).withMinute(30).withSecond(0).withNano(0), placeText = paperClub.locationText),
                StudySession(studyId = paperClub.id!!, sessionNo = 3, title = "실험 설계 읽는 법", scheduledAt = now.plusDays(6).withHour(7).withMinute(30).withSecond(0).withNano(0), placeText = paperClub.locationText, noticeText = "그림이나 표 하나를 골라 왜 중요한지 메모해 와 주세요."),
                StudySession(studyId = paperClub.id!!, sessionNo = 4, title = "레퍼런스 따라가기", scheduledAt = now.plusDays(13).withHour(7).withMinute(30).withSecond(0).withNano(0), placeText = paperClub.locationText),
            ),
        )

        studySessionRepository.saveAll(
            listOf(
                backendLabSessions[0],
                backendLabSessions[1],
                sprintClubSessions[0],
                sprintClubSessions[1],
                designCircleSessions[0],
                onboardingLabSessions[0],
                interviewClinicSessions[0],
                paperClubSessions[0],
                paperClubSessions[1],
            ).onEach { it.attendanceClosedAt = now.minusHours(12) },
        )

        sessionParticipationRepository.saveAll(
            listOf(
                SessionParticipation(sessionId = backendLabSessions[0].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(14)),
                SessionParticipation(sessionId = backendLabSessions[0].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusDays(14)),
                SessionParticipation(sessionId = backendLabSessions[0].id!!, userId = sora.id!!, planned = true, updatedAt = now.minusDays(14)),
                SessionParticipation(sessionId = backendLabSessions[1].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(9)),
                SessionParticipation(sessionId = backendLabSessions[1].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusDays(9)),
                SessionParticipation(sessionId = backendLabSessions[1].id!!, userId = sora.id!!, planned = false, updatedAt = now.minusDays(9)),
                SessionParticipation(sessionId = backendLabSessions[2].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusHours(10)),
                SessionParticipation(sessionId = backendLabSessions[2].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusHours(8)),
                SessionParticipation(sessionId = sprintClubSessions[0].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusDays(20)),
                SessionParticipation(sessionId = sprintClubSessions[0].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(20)),
                SessionParticipation(sessionId = sprintClubSessions[0].id!!, userId = daniel.id!!, planned = true, updatedAt = now.minusDays(20)),
                SessionParticipation(sessionId = sprintClubSessions[1].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusDays(13)),
                SessionParticipation(sessionId = sprintClubSessions[1].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(13)),
                SessionParticipation(sessionId = sprintClubSessions[1].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusDays(13)),
                SessionParticipation(sessionId = sprintClubSessions[2].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusHours(14)),
                SessionParticipation(sessionId = sprintClubSessions[2].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusHours(12)),
                SessionParticipation(sessionId = designCircleSessions[0].id!!, userId = sora.id!!, planned = true, updatedAt = now.minusDays(7)),
                SessionParticipation(sessionId = designCircleSessions[0].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(7)),
                SessionParticipation(sessionId = designCircleSessions[0].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusDays(7)),
                SessionParticipation(sessionId = designCircleSessions[1].id!!, userId = sora.id!!, planned = true, updatedAt = now.minusHours(6)),
                SessionParticipation(sessionId = designCircleSessions[1].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusHours(5)),
                SessionParticipation(sessionId = portfolioClubSessions[0].id!!, userId = daniel.id!!, planned = true, updatedAt = now.minusHours(4)),
                SessionParticipation(sessionId = portfolioClubSessions[0].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusHours(3)),
                SessionParticipation(sessionId = portfolioClubSessions[0].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusHours(2)),
                SessionParticipation(sessionId = onboardingLabSessions[0].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusDays(5)),
                SessionParticipation(sessionId = onboardingLabSessions[0].id!!, userId = sora.id!!, planned = true, updatedAt = now.minusDays(5)),
                SessionParticipation(sessionId = onboardingLabSessions[0].id!!, userId = eunchae.id!!, planned = true, updatedAt = now.minusDays(5)),
                SessionParticipation(sessionId = onboardingLabSessions[0].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(5)),
                SessionParticipation(sessionId = onboardingLabSessions[1].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusHours(9)),
                SessionParticipation(sessionId = onboardingLabSessions[1].id!!, userId = sora.id!!, planned = true, updatedAt = now.minusHours(8)),
                SessionParticipation(sessionId = onboardingLabSessions[1].id!!, userId = eunchae.id!!, planned = true, updatedAt = now.minusHours(7)),
                SessionParticipation(sessionId = interviewClinicSessions[0].id!!, userId = jihun.id!!, planned = true, updatedAt = now.minusDays(6)),
                SessionParticipation(sessionId = interviewClinicSessions[0].id!!, userId = minho.id!!, planned = true, updatedAt = now.minusDays(6)),
                SessionParticipation(sessionId = interviewClinicSessions[0].id!!, userId = daniel.id!!, planned = true, updatedAt = now.minusDays(6)),
                SessionParticipation(sessionId = interviewClinicSessions[1].id!!, userId = jihun.id!!, planned = true, updatedAt = now.minusHours(5)),
                SessionParticipation(sessionId = interviewClinicSessions[1].id!!, userId = yuna.id!!, planned = true, updatedAt = now.minusHours(4)),
                SessionParticipation(sessionId = paperClubSessions[0].id!!, userId = eunchae.id!!, planned = true, updatedAt = now.minusDays(15)),
                SessionParticipation(sessionId = paperClubSessions[0].id!!, userId = hana.id!!, planned = true, updatedAt = now.minusDays(15)),
                SessionParticipation(sessionId = paperClubSessions[0].id!!, userId = jihun.id!!, planned = true, updatedAt = now.minusDays(15)),
                SessionParticipation(sessionId = paperClubSessions[1].id!!, userId = eunchae.id!!, planned = true, updatedAt = now.minusDays(8)),
                SessionParticipation(sessionId = paperClubSessions[1].id!!, userId = sora.id!!, planned = true, updatedAt = now.minusDays(8)),
                SessionParticipation(sessionId = paperClubSessions[1].id!!, userId = hana.id!!, planned = false, updatedAt = now.minusDays(8)),
                SessionParticipation(sessionId = paperClubSessions[2].id!!, userId = eunchae.id!!, planned = true, updatedAt = now.minusHours(6)),
                SessionParticipation(sessionId = paperClubSessions[2].id!!, userId = jihun.id!!, planned = true, updatedAt = now.minusHours(5)),
            ),
        )

        attendanceRepository.saveAll(
            listOf(
                Attendance(sessionId = backendLabSessions[0].id!!, userId = hana.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = hana.id!!, checkedAt = now.minusDays(14)),
                Attendance(sessionId = backendLabSessions[0].id!!, userId = minho.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = hana.id!!, checkedAt = now.minusDays(14)),
                Attendance(sessionId = backendLabSessions[0].id!!, userId = sora.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = hana.id!!, checkedAt = now.minusDays(14)),
                Attendance(sessionId = backendLabSessions[1].id!!, userId = hana.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = hana.id!!, checkedAt = now.minusDays(9)),
                Attendance(sessionId = backendLabSessions[1].id!!, userId = minho.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = hana.id!!, checkedAt = now.minusDays(9)),
                Attendance(sessionId = sprintClubSessions[0].id!!, userId = minho.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = minho.id!!, checkedAt = now.minusDays(20)),
                Attendance(sessionId = sprintClubSessions[0].id!!, userId = hana.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = minho.id!!, checkedAt = now.minusDays(20)),
                Attendance(sessionId = sprintClubSessions[0].id!!, userId = daniel.id!!, status = AttendanceStatus.ABSENT, checkedByUserId = minho.id!!, checkedAt = now.minusDays(20)),
                Attendance(sessionId = sprintClubSessions[1].id!!, userId = minho.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = minho.id!!, checkedAt = now.minusDays(13)),
                Attendance(sessionId = sprintClubSessions[1].id!!, userId = hana.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = minho.id!!, checkedAt = now.minusDays(13)),
                Attendance(sessionId = sprintClubSessions[1].id!!, userId = yuna.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = minho.id!!, checkedAt = now.minusDays(13)),
                Attendance(sessionId = designCircleSessions[0].id!!, userId = sora.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = sora.id!!, checkedAt = now.minusDays(7)),
                Attendance(sessionId = designCircleSessions[0].id!!, userId = hana.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = sora.id!!, checkedAt = now.minusDays(7)),
                Attendance(sessionId = designCircleSessions[0].id!!, userId = yuna.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = sora.id!!, checkedAt = now.minusDays(7)),
                Attendance(sessionId = onboardingLabSessions[0].id!!, userId = yuna.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = yuna.id!!, checkedAt = now.minusDays(5)),
                Attendance(sessionId = onboardingLabSessions[0].id!!, userId = sora.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = yuna.id!!, checkedAt = now.minusDays(5)),
                Attendance(sessionId = onboardingLabSessions[0].id!!, userId = eunchae.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = yuna.id!!, checkedAt = now.minusDays(5)),
                Attendance(sessionId = onboardingLabSessions[0].id!!, userId = hana.id!!, status = AttendanceStatus.ABSENT, checkedByUserId = yuna.id!!, checkedAt = now.minusDays(5)),
                Attendance(sessionId = interviewClinicSessions[0].id!!, userId = jihun.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = jihun.id!!, checkedAt = now.minusDays(6)),
                Attendance(sessionId = interviewClinicSessions[0].id!!, userId = minho.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = jihun.id!!, checkedAt = now.minusDays(6)),
                Attendance(sessionId = interviewClinicSessions[0].id!!, userId = daniel.id!!, status = AttendanceStatus.ABSENT, checkedByUserId = jihun.id!!, checkedAt = now.minusDays(6)),
                Attendance(sessionId = paperClubSessions[0].id!!, userId = eunchae.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = eunchae.id!!, checkedAt = now.minusDays(15)),
                Attendance(sessionId = paperClubSessions[0].id!!, userId = hana.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = eunchae.id!!, checkedAt = now.minusDays(15)),
                Attendance(sessionId = paperClubSessions[0].id!!, userId = jihun.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = eunchae.id!!, checkedAt = now.minusDays(15)),
                Attendance(sessionId = paperClubSessions[1].id!!, userId = eunchae.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = eunchae.id!!, checkedAt = now.minusDays(8)),
                Attendance(sessionId = paperClubSessions[1].id!!, userId = sora.id!!, status = AttendanceStatus.PRESENT, checkedByUserId = eunchae.id!!, checkedAt = now.minusDays(8)),
            ),
        )

        checkLedgerRepository.saveAll(
            listOf(
                CheckLedger(userId = hana.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = backendLabSessions[1].id!!, createdAt = now.minusDays(9)),
                CheckLedger(userId = minho.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = backendLabSessions[1].id!!, createdAt = now.minusDays(9)),
                CheckLedger(userId = sora.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = designCircleSessions[0].id!!, createdAt = now.minusDays(7)),
                CheckLedger(userId = yuna.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = sprintClubSessions[1].id!!, createdAt = now.minusDays(13)),
                CheckLedger(userId = hana.id!!, changeType = CheckChangeType.SPEND, amount = -15, reason = CheckReason.ITEM_PURCHASE, refType = "ITEM", refId = featuredTop.id!!, createdAt = now.minusDays(7)),
                CheckLedger(userId = yuna.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = onboardingLabSessions[0].id!!, createdAt = now.minusDays(5)),
                CheckLedger(userId = jihun.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = interviewClinicSessions[0].id!!, createdAt = now.minusDays(6)),
                CheckLedger(userId = eunchae.id!!, changeType = CheckChangeType.EARN, amount = 2, reason = CheckReason.ATTENDANCE, refType = "SESSION", refId = paperClubSessions[1].id!!, createdAt = now.minusDays(8)),
            ),
        )

        val backendPosts = postRepository.saveAll(
            listOf(
                Post(studyId = backendLab.id!!, authorUserId = hana.id!!, type = PostType.NOTICE, title = "이번 주 주제: API 에러 응답", content = "각자 정리하고 싶은 컨트롤러 응답 하나씩 가져와 주세요.", createdAt = now.minusHours(20), updatedAt = now.minusHours(20)),
                Post(studyId = backendLab.id!!, authorUserId = minho.id!!, type = PostType.POST, title = "쿼리 플랜 비교 자료", content = "지난주에 봤던 N+1 이슈 전후 실행 계획 캡처를 정리해서 올렸어요.", createdAt = now.minusHours(16), updatedAt = now.minusHours(16)),
            ),
        )
        val sprintPosts = postRepository.saveAll(
            listOf(
                Post(studyId = sprintClub.id!!, authorUserId = minho.id!!, type = PostType.NOTICE, title = "이번 토요일 진행 방식 안내", content = "이번 주는 DP 세트가 조금 어려워서 첫 집중 블록을 더 길게 가져갑니다.", createdAt = now.minusHours(12), updatedAt = now.minusHours(12)),
                Post(studyId = sprintClub.id!!, authorUserId = yuna.id!!, type = PostType.POST, title = "모의테스트 회고", content = "그리디는 괜찮았는데 마지막 두 문제에서 시간 관리가 무너졌어요.", createdAt = now.minusHours(9), updatedAt = now.minusHours(9)),
            ),
        )
        val designPosts = postRepository.saveAll(
            listOf(
                Post(studyId = designCircle.id!!, authorUserId = sora.id!!, type = PostType.NOTICE, title = "화요일 전까지 4장 읽어오기", content = "크리틱 질문에 집중해서 보고, 같이 뜯어볼 모바일 화면 하나씩 준비해 주세요.", createdAt = now.minusHours(7), updatedAt = now.minusHours(7)),
                Post(studyId = designCircle.id!!, authorUserId = hana.id!!, type = PostType.POST, title = "잘 먹혔던 크리틱 질문 세 가지", content = "어제 온보딩 화면 리뷰에 썼던 질문 세트를 짧은 예시와 함께 정리했어요.", createdAt = now.minusHours(5), updatedAt = now.minusHours(5)),
            ),
        )
        val portfolioPosts = postRepository.saveAll(
            listOf(
                Post(studyId = portfolioClub.id!!, authorUserId = daniel.id!!, type = PostType.NOTICE, title = "강한 케이스 하나 가져오기", content = "처음 10분은 목표를 맞추고, 이후에는 빠르게 돌아가며 피드백을 주고받을게요.", createdAt = now.minusHours(3), updatedAt = now.minusHours(3)),
                Post(studyId = portfolioClub.id!!, authorUserId = yuna.id!!, type = PostType.POST, title = "서사 구조 메모", content = "프로덕트 케이스 스터디를 정리할 때 쓸 수 있는 간단한 전개 틀을 적어봤어요.", createdAt = now.minusHours(2), updatedAt = now.minusHours(2)),
            ),
        )
        val onboardingPosts = postRepository.saveAll(
            listOf(
                Post(studyId = onboardingLab.id!!, authorUserId = yuna.id!!, type = PostType.NOTICE, title = "이번 주엔 가입 완료 화면까지 같이 볼게요", content = "첫 화면만 보지 말고, 가입이 끝난 뒤 다음 행동으로 이어지는 흐름까지 캡처해 와 주세요.", createdAt = now.minusHours(6), updatedAt = now.minusHours(6)),
                Post(studyId = onboardingLab.id!!, authorUserId = sora.id!!, type = PostType.POST, title = "이탈 포인트 메모 양식 공유", content = "문구, 필수 입력, 에러 경험, 완료 후 다음 행동까지 한 번에 적을 수 있는 템플릿을 올렸어요.", createdAt = now.minusHours(4), updatedAt = now.minusHours(4)),
            ),
        )
        val interviewPosts = postRepository.saveAll(
            listOf(
                Post(studyId = interviewClinic.id!!, authorUserId = jihun.id!!, type = PostType.NOTICE, title = "이번 주 질문은 트러블슈팅 중심입니다", content = "상황, 원인, 해결, 배운 점 순서로 말해보는 연습을 할 예정이에요.", createdAt = now.minusHours(5), updatedAt = now.minusHours(5)),
                Post(studyId = interviewClinic.id!!, authorUserId = minho.id!!, type = PostType.POST, title = "90초 답변 초안 공유", content = "자기소개와 프로젝트 소개를 각각 90초 안으로 줄인 버전을 올립니다.", createdAt = now.minusHours(3), updatedAt = now.minusHours(3)),
            ),
        )
        val paperPosts = postRepository.saveAll(
            listOf(
                Post(studyId = paperClub.id!!, authorUserId = eunchae.id!!, type = PostType.NOTICE, title = "다음 주엔 그림 하나만 깊게 읽습니다", content = "표나 그래프 한 장만 골라 왜 중요한지 설명해 보는 시간으로 갈게요.", createdAt = now.minusHours(8), updatedAt = now.minusHours(8)),
                Post(studyId = paperClub.id!!, authorUserId = jihun.id!!, type = PostType.POST, title = "초록 읽을 때 남긴 질문", content = "용어를 다 이해하지 못해도 중심 문제와 가설만 먼저 잡는 방식이 도움이 됐어요.", createdAt = now.minusHours(6), updatedAt = now.minusHours(6)),
            ),
        )

        postCommentRepository.saveAll(
            listOf(
                PostComment(postId = backendPosts[1].id!!, authorUserId = sora.id!!, content = "실행 계획 캡처 덕분에 느린 쿼리 원인을 설명하기 쉬웠어요.", createdAt = now.minusHours(13), updatedAt = now.minusHours(13)),
                PostComment(postId = backendPosts[1].id!!, authorUserId = hana.id!!, content = "다음 주엔 검증 로직 정리 전후도 같이 비교해봐요.", createdAt = now.minusHours(12), updatedAt = now.minusHours(12)),
                PostComment(postId = sprintPosts[1].id!!, authorUserId = hana.id!!, content = "회고 읽고 나니 마지막 구간에서 왜 흔들렸는지 바로 보였어요.", createdAt = now.minusHours(8), updatedAt = now.minusHours(8)),
                PostComment(postId = designPosts[1].id!!, authorUserId = yuna.id!!, content = "두 번째 질문은 모바일 온보딩 리뷰에 딱 맞는 것 같아요.", createdAt = now.minusHours(4), updatedAt = now.minusHours(4)),
                PostComment(postId = portfolioPosts[1].id!!, authorUserId = minho.id!!, content = "이 틀 그대로 세션 전에 한 번 써봐도 될 것 같아요.", createdAt = now.minusHours(1), updatedAt = now.minusHours(1)),
                PostComment(postId = onboardingPosts[1].id!!, authorUserId = eunchae.id!!, content = "이 양식 덕분에 어디서 막혔는지 설명하기 훨씬 쉬워졌어요.", createdAt = now.minusHours(3), updatedAt = now.minusHours(3)),
                PostComment(postId = onboardingPosts[1].id!!, authorUserId = yuna.id!!, content = "다음엔 완료 화면 이후 추천 행동도 같이 적어보면 좋겠어요.", createdAt = now.minusHours(2), updatedAt = now.minusHours(2)),
                PostComment(postId = interviewPosts[1].id!!, authorUserId = daniel.id!!, content = "첫 문장을 더 짧게 끊으니까 훨씬 또렷하게 들렸어요.", createdAt = now.minusHours(2), updatedAt = now.minusHours(2)),
                PostComment(postId = interviewPosts[1].id!!, authorUserId = yuna.id!!, content = "경험 설명 뒤에 배운 점을 붙이니 마무리가 좋아졌어요.", createdAt = now.minusHours(1), updatedAt = now.minusHours(1)),
                PostComment(postId = paperPosts[1].id!!, authorUserId = hana.id!!, content = "용어를 다 몰라도 질문부터 잡자는 방식이 마음이 편했어요.", createdAt = now.minusHours(5), updatedAt = now.minusHours(5)),
                PostComment(postId = paperPosts[1].id!!, authorUserId = sora.id!!, content = "그림 한 장만 깊게 보는 방식은 디자인 리서치 읽을 때도 써먹을 수 있겠네요.", createdAt = now.minusHours(4), updatedAt = now.minusHours(4)),
            ),
        )
    }

    private fun createSeedUser(
        email: String,
        nickname: String,
        totalEarnedChecks: Int,
        currentChecks: Int,
        createdAt: LocalDateTime,
        bodyAssetKey: String,
        defaultAppearance: AvatarFreeAppearance,
        avatarDefaults: AvatarDefaults,
    ): User {
        val user = userRepository.save(
            User(
                email = email,
                passwordHash = passwordEncoder.encode(DEFAULT_PASSWORD)!!,
                nickname = nickname,
                totalEarnedChecks = totalEarnedChecks,
                currentChecks = currentChecks,
                level = levelPolicy.levelFor(totalEarnedChecks),
                createdAt = createdAt,
            ),
        )

        userAvatarItemRepository.saveAll(
            avatarDefaults.allItems.map { avatarItem ->
                UserAvatarItem(
                    userId = user.id!!,
                    avatarItemId = avatarItem.id!!,
                    purchasedAt = createdAt,
                )
            },
        )

        avatarProfileRepository.save(
            AvatarProfile(
                userId = user.id!!,
                bodyAssetKey = bodyAssetKey,
                pupilAssetKey = defaultAppearance.pupilAssetKey,
                eyebrowAssetKey = defaultAppearance.eyebrowAssetKey,
                eyelashAssetKey = defaultAppearance.eyelashAssetKey,
                mouthAssetKey = defaultAppearance.mouthAssetKey,
                equippedHairItemId = avatarDefaults.hair.id,
                equippedTopItemId = avatarDefaults.top.id,
                equippedBottomItemId = avatarDefaults.bottom.id,
                equippedPupilItemId = avatarDefaults.pupil.id,
                equippedEyebrowItemId = avatarDefaults.eyebrow.id,
                equippedEyelashItemId = avatarDefaults.eyelash.id,
                equippedMouthItemId = avatarDefaults.mouth.id,
            ),
        )

        return user
    }

    private data class AvatarDefaults(
        val hair: com.gonguham.backend.avatar.AvatarItem,
        val top: com.gonguham.backend.avatar.AvatarItem,
        val bottom: com.gonguham.backend.avatar.AvatarItem,
        val pupil: com.gonguham.backend.avatar.AvatarItem,
        val eyebrow: com.gonguham.backend.avatar.AvatarItem,
        val eyelash: com.gonguham.backend.avatar.AvatarItem,
        val mouth: com.gonguham.backend.avatar.AvatarItem,
    ) {
        val allItems: List<com.gonguham.backend.avatar.AvatarItem>
            get() = listOf(hair, top, bottom, pupil, eyebrow, eyelash, mouth)
    }

    companion object {
        const val DEFAULT_PASSWORD = "gonguham123!"
    }
}
