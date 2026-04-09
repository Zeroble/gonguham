package com.gonguham.backend.common

import com.gonguham.backend.avatar.AvatarAssetCatalog
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
    private val levelPolicy: LevelPolicy,
) : CommandLineRunner {
    @Transactional
    override fun run(vararg args: String) {
        if (userRepository.count() > 0L) return

        val now = LocalDateTime.now()
        val defaultAppearance = AvatarAssetCatalog.defaultAppearance()

        val leader = userRepository.save(
            User(
                kakaoId = "kakao-101",
                nickname = "정다솔",
                email = "leader@gonguham.app",
                totalEarnedChecks = 27,
                currentChecks = 14,
                level = levelPolicy.levelFor(27),
                createdAt = now.minusDays(20),
            ),
        )
        val member = userRepository.save(
            User(
                kakaoId = "kakao-102",
                nickname = "김민수",
                email = "member@gonguham.app",
                totalEarnedChecks = 9,
                currentChecks = 9,
                level = levelPolicy.levelFor(9),
                createdAt = now.minusDays(12),
            ),
        )
        val guest = userRepository.save(
            User(
                kakaoId = "kakao-103",
                nickname = "박서연",
                email = "guest@gonguham.app",
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
                title = "UX 리서치 짧은 실습반",
                description = "인터뷰 질문 짜기부터 인사이트 정리까지 빠르게 실습해봅니다.",
                daysOfWeek = mutableSetOf(DayOfWeek.FRIDAY),
                startTime = LocalTime.of(17, 0),
                endTime = LocalTime.of(18, 30),
                startDate = LocalDate.now().plusDays(3),
                endDate = LocalDate.now().plusDays(3),
                repeatType = RepeatType.ONCE,
                maxMembers = 6,
                locationType = LocationType.ONLINE,
                locationText = "Google Meet",
                rulesText = "서로 인터뷰 연습 파트너가 되어주기",
                suppliesText = "노트북, 메모 앱",
                cautionText = "회차 1회짜리 반짝 스터디",
                status = StudyStatus.OPEN,
                createdAt = now.minusDays(3),
            ),
        )

        studyTagRepository.saveAll(
            listOf(
                StudyTag(studyId = topicStudy.id!!, name = "CS"),
                StudyTag(studyId = topicStudy.id!!, name = "자료구조"),
                StudyTag(studyId = mogakStudy.id!!, name = "집중"),
                StudyTag(studyId = mogakStudy.id!!, name = "루틴"),
                StudyTag(studyId = flashStudy.id!!, name = "UX"),
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
                StudySession(studyId = topicStudy.id!!, sessionNo = 1, title = "OT and study setup", scheduledAt = now.minusWeeks(9), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 2, title = "Array and list warmup", scheduledAt = now.minusWeeks(8), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 3, title = "Stack and queue drill", scheduledAt = now.minusWeeks(7), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 4, title = "Tree traversal basics", scheduledAt = now.minusWeeks(6), placeText = topicStudy.locationText),
                StudySession(studyId = topicStudy.id!!, sessionNo = 5, title = "Graph intro practice", scheduledAt = now.minusWeeks(5), placeText = topicStudy.locationText),
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
    }
}
