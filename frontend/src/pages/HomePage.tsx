import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useOutletContext, useSearchParams } from 'react-router-dom'
import {
  api,
  type AttendanceRosterEntry,
  type FeedItem,
  type PostDetail,
  type SessionAttendancePanel,
  type StudyDetail,
  type StudyHomePanel,
  type UpdateStudySessionInput,
} from '../app/api'
import { useApp } from '../app/useApp'
import { ProfileNameButton } from '../features/profile/ProfileNameButton'
import { StudyOverviewSheet } from '../features/study/StudyOverviewSheet'
import { type AppShellOutletContext } from '../layouts/appShellDashboard'

const POSTS_PER_PAGE = 5
const POST_PAGINATION_WINDOW = 5
const BREAK_TITLE = '쉬어가는 회차'

const LEADER_START_WINDOW_MS = 30 * 60 * 1000
const LEADER_START_WINDOW_TOAST = '예정 시간 30분 전부터 스터디를 시작할 수 있어요.'

type RenderedTimelineItem = {
  sessionId: number | string
  roundLabel: string
  title: string
  nodeState: string
  scheduledAt: string
  scheduledAtValue: string
  planned: boolean
  sessionType: string
  placeholder?: boolean
}

type RenderedPostItem = {
  postId: number | string
  authorUserId: number | null
  title: string
  authorNickname: string
  createdAt: string
  placeholder?: boolean
}

type SessionEditorDraft = UpdateStudySessionInput & {
  editable: boolean
}

type HomeDetailMotionDirection = 'left' | 'right'

function toRenderedPosts(posts: FeedItem[]) {
  return posts.map((post) => ({
    postId: post.postId,
    authorUserId: post.authorUserId,
    title: post.title,
    authorNickname: post.authorNickname,
    createdAt: post.createdAt,
  }))
}

function buildPaginationWindow(totalPages: number, currentPage: number) {
  if (totalPages <= POST_PAGINATION_WINDOW) {
    return Array.from({ length: totalPages }, (_, index) => index + 1)
  }

  let start = Math.max(1, currentPage - Math.floor(POST_PAGINATION_WINDOW / 2))
  const end = Math.min(totalPages, start + POST_PAGINATION_WINDOW - 1)

  start = Math.max(1, end - POST_PAGINATION_WINDOW + 1)

  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
}

function getTimelineToneByState(nodeState: string) {
  if (nodeState === 'BREAK') return 'break'
  if (nodeState === 'CURRENT') return 'planned'
  if (nodeState === 'ATTENDED') return 'done'
  if (nodeState === 'ABSENT') return 'missed'
  if (nodeState === 'FUTURE') return 'pending'
  return 'muted'
}

function getTimelineNodeClass(nodeState: string) {
  if (nodeState === 'BREAK') return 'home-timeline-node is-break'
  if (nodeState === 'CURRENT') return 'home-timeline-node is-highlighted'
  if (nodeState === 'ATTENDED') return 'home-timeline-node is-attended'
  if (nodeState === 'ABSENT') return 'home-timeline-node is-absent'
  if (nodeState === 'FUTURE') return 'home-timeline-node is-future'
  return 'home-timeline-node'
}

function getStudyBadgeTone(label: string) {
  if (label.includes('주제')) return 'topic'
  if (label.includes('반짝')) return 'flash'
  return 'mogak'
}

function getMemberCurrentChipLabel(planned: boolean) {
  return planned ? '참여 예정' : '불참 예정'
}

function toAttendanceMap(roster: AttendanceRosterEntry[] = []) {
  return Object.fromEntries(
    roster.map((member) => [
      member.userId,
      member.attendanceStatus ?? (member.planned ? 'PRESENT' : 'ABSENT'),
    ]),
  )
}

function sortSessionEditorDrafts(sessions: SessionEditorDraft[]) {
  return [...sessions].sort((left, right) => left.scheduledAt.localeCompare(right.scheduledAt))
}

function buildSessionEditorDrafts(sessions: StudyHomePanel['sessions']): SessionEditorDraft[] {
  return sortSessionEditorDrafts(
    sessions.map((session) => ({
      sessionId: session.sessionId,
      title: session.sessionType === 'BREAK' ? BREAK_TITLE : session.title,
      scheduledAt: session.scheduledAtValue,
      sessionType: session.sessionType as 'REGULAR' | 'BREAK',
      editable: session.editable,
    })),
  )
}

function buildDefaultEditableSessionTitle(index: number) {
  return `${index + 1}회차 주제`
}

function canLeaderStartSession(scheduledAtValue: string) {
  const scheduledAt = new Date(scheduledAtValue).getTime()
  if (Number.isNaN(scheduledAt)) {
    return false
  }

  return Date.now() >= scheduledAt - LEADER_START_WINDOW_MS
}

export function HomePage() {
  const { sessionUserId, refreshMe, showToast } = useApp()
  const { dashboard, isDashboardLoading, refreshDashboard } =
    useOutletContext<AppShellOutletContext>()
  const [searchParams, setSearchParams] = useSearchParams()
  const timelineViewportRef = useRef<HTMLDivElement | null>(null)
  const currentTimelineRowRef = useRef<HTMLDivElement | null>(null)
  const postListRequestTokenRef = useRef(0)
  const postRequestTokenRef = useRef(0)
  const overviewRequestTokenRef = useRef(0)
  const [draftTitle, setDraftTitle] = useState('')
  const [draftContent, setDraftContent] = useState('')
  const [draftType, setDraftType] = useState<'POST' | 'NOTICE'>('POST')
  const [studyPosts, setStudyPosts] = useState<RenderedPostItem[]>([])
  const [studyPostsStudyId, setStudyPostsStudyId] = useState<number | null>(null)
  const [postPage, setPostPage] = useState(1)
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [postDetail, setPostDetail] = useState<PostDetail | null>(null)
  const [commentDraft, setCommentDraft] = useState('')
  const [attendanceMap, setAttendanceMap] = useState<Record<number, string>>({})
  const [attendancePanel, setAttendancePanel] = useState<SessionAttendancePanel | null>(null)
  const [sessionDrafts, setSessionDrafts] = useState<SessionEditorDraft[]>([])
  const [overviewStudyDetail, setOverviewStudyDetail] = useState<StudyDetail | null>(null)
  const [isPostsLoading, setIsPostsLoading] = useState(false)
  const [isPostDetailLoading, setIsPostDetailLoading] = useState(false)
  const [isCommentSubmitting, setIsCommentSubmitting] = useState(false)
  const [isSessionSaving, setIsSessionSaving] = useState(false)
  const [isOverviewLoading, setIsOverviewLoading] = useState(false)
  const [isStudyActionPending, setIsStudyActionPending] = useState(false)
  const [showOverview, setShowOverview] = useState(false)
  const [showPostModal, setShowPostModal] = useState(false)
  const [showComposer, setShowComposer] = useState(false)
  const [showAttendance, setShowAttendance] = useState(false)
  const [showSessionEditor, setShowSessionEditor] = useState(false)
  const [detailMotionDirection, setDetailMotionDirection] =
    useState<HomeDetailMotionDirection | null>(null)
  const [detailMotionKey, setDetailMotionKey] = useState(0)
  const selectedStudyId = useMemo(() => {
    const value = Number(searchParams.get('studyId'))
    return Number.isFinite(value) && value > 0 ? value : null
  }, [searchParams])

  useEffect(() => {
    if (!dashboard?.defaultStudyId) {
      return
    }

    if (selectedStudyId && dashboard.joinedStudies.some((study) => study.studyId === selectedStudyId)) {
      return
    }

    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('studyId', String(dashboard.defaultStudyId))
    setSearchParams(nextParams, { replace: true })
  }, [dashboard?.defaultStudyId, dashboard?.joinedStudies, searchParams, selectedStudyId, setSearchParams])

  const activeStudy = useMemo(() => {
    if (!dashboard?.studyPanels.length) {
      return null
    }

    const targetStudyId = selectedStudyId ?? dashboard.defaultStudyId
    return dashboard.studyPanels.find((panel) => panel.studyId === targetStudyId) ?? dashboard.studyPanels[0]
  }, [dashboard?.defaultStudyId, dashboard?.studyPanels, selectedStudyId])

  function handleStudyCardClick(studyId: number) {
    if (!dashboard || !activeStudy || studyId === activeStudy.studyId) {
      return
    }

    const currentIndex = dashboard.joinedStudies.findIndex(
      (study) => study.studyId === activeStudy.studyId,
    )
    const nextIndex = dashboard.joinedStudies.findIndex((study) => study.studyId === studyId)

    if (currentIndex !== -1 && nextIndex !== -1 && currentIndex !== nextIndex) {
      setDetailMotionDirection(nextIndex > currentIndex ? 'right' : 'left')
      setDetailMotionKey((current) => current + 1)
    } else {
      setDetailMotionDirection(null)
    }

    setSearchParams({ studyId: String(studyId) })
  }

  const hasAttendanceStarted = useMemo(
    () => activeStudy?.attendanceRoster.some((member) => member.attendanceStatus !== null) ?? false,
    [activeStudy],
  )

  useEffect(() => {
    setAttendanceMap(toAttendanceMap(activeStudy?.attendanceRoster))
  }, [activeStudy])

  const currentTimelineSession = useMemo(() => {
    if (!activeStudy?.sessions.length) {
      return null
    }

    return (
      activeStudy.sessions.find((session) => session.sessionId === activeStudy.currentSessionId) ??
      activeStudy.sessions.find(
        (session) => session.nodeState === 'CURRENT' && session.sessionType !== 'BREAK',
      ) ??
      activeStudy.sessions.find((session) => session.sessionType === 'REGULAR') ??
      activeStudy.sessions[0]
    )
  }, [activeStudy])

  const scheduleLabel = useMemo(() => {
    if (!currentTimelineSession) {
      return activeStudy?.locationText ?? ''
    }

    return `${currentTimelineSession.scheduledAt} · ${activeStudy?.locationText ?? ''}`
  }, [activeStudy?.locationText, currentTimelineSession])

  const renderedTimeline = useMemo<RenderedTimelineItem[]>(() => {
    if (!activeStudy) {
      return []
    }

    return [
      ...activeStudy.sessions
    ]
  }, [activeStudy])

  const preloadedStudyPosts = useMemo(
    () => toRenderedPosts(activeStudy?.posts ?? []),
    [activeStudy?.posts],
  )

  const currentStudyPosts = useMemo(() => {
    if (!activeStudy) {
      return []
    }

    return studyPostsStudyId === activeStudy.studyId ? studyPosts : preloadedStudyPosts
  }, [activeStudy, preloadedStudyPosts, studyPosts, studyPostsStudyId])

  const sessionDraftRegularCount = useMemo(
    () => sessionDrafts.filter((session) => session.sessionType === 'REGULAR').length,
    [sessionDrafts],
  )

  const sessionDraftBreakCount = sessionDrafts.length - sessionDraftRegularCount

  useLayoutEffect(() => {
    const viewport = timelineViewportRef.current
    const currentRow = currentTimelineRowRef.current

    if (!viewport || !currentRow || !activeStudy?.currentSessionId) {
      return
    }

    const viewportRect = viewport.getBoundingClientRect()
    const rowRect = currentRow.getBoundingClientRect()
    const nextScrollTop = viewport.scrollTop + (rowRect.top - viewportRect.top)
    const maxScrollTop = viewport.scrollHeight - viewport.clientHeight

    viewport.scrollTop = Math.max(0, Math.min(nextScrollTop, maxScrollTop))
  }, [activeStudy?.currentSessionId, activeStudy?.studyId, renderedTimeline.length])

  const totalPostPages = useMemo(
    () => Math.max(1, Math.ceil(currentStudyPosts.length / POSTS_PER_PAGE)),
    [currentStudyPosts.length],
  )

  const currentStudyPostPage = useMemo(() => {
    if (!activeStudy) {
      return 1
    }

    return studyPostsStudyId === activeStudy.studyId ? postPage : 1
  }, [activeStudy, postPage, studyPostsStudyId])

  const visiblePostPages = useMemo(
    () => buildPaginationWindow(totalPostPages, currentStudyPostPage),
    [currentStudyPostPage, totalPostPages],
  )

  const pagedPosts = useMemo(() => {
    const startIndex = (currentStudyPostPage - 1) * POSTS_PER_PAGE
    return currentStudyPosts.slice(startIndex, startIndex + POSTS_PER_PAGE)
  }, [currentStudyPostPage, currentStudyPosts])

  const shouldShowLeadingFirstPage = visiblePostPages.length > 0 && visiblePostPages[0] > 1
  const shouldShowLeadingEllipsis = visiblePostPages.length > 0 && visiblePostPages[0] > 2
  const shouldShowTrailingLastPage =
    visiblePostPages.length > 0 && visiblePostPages[visiblePostPages.length - 1] < totalPostPages
  const shouldShowTrailingEllipsis =
    visiblePostPages.length > 0 && visiblePostPages[visiblePostPages.length - 1] < totalPostPages - 1

  const selectedPostSummary = useMemo(
    () =>
      currentStudyPosts.find(
        (post) => typeof post.postId === 'number' && post.postId === selectedPostId,
      ) ?? null,
    [currentStudyPosts, selectedPostId],
  )

  useEffect(() => {
    postListRequestTokenRef.current += 1
    overviewRequestTokenRef.current += 1
    setPostPage(1)
    postRequestTokenRef.current += 1
    setSessionDrafts([])
    setSelectedPostId(null)
    setPostDetail(null)
    setCommentDraft('')
    setIsPostsLoading(false)
    setIsPostDetailLoading(false)
    setIsCommentSubmitting(false)
    setIsSessionSaving(false)
    setOverviewStudyDetail(null)
    setIsOverviewLoading(false)
    setIsStudyActionPending(false)
    setShowOverview(false)
    setShowPostModal(false)
    setShowSessionEditor(false)
  }, [activeStudy?.studyId])

  useEffect(() => {
    setPostPage((currentPage) => Math.min(currentPage, totalPostPages))
  }, [totalPostPages])

  useEffect(() => {
    if (!sessionUserId || !activeStudy?.studyId) {
      return
    }

    let cancelled = false

    async function fetchStudyPosts() {
      const requestToken = ++postListRequestTokenRef.current
      setIsPostsLoading(true)

      try {
        const posts = await api.getStudyPosts(sessionUserId, activeStudy.studyId, 'POST')

        if (cancelled || requestToken !== postListRequestTokenRef.current) {
          return
        }

        setStudyPosts(toRenderedPosts(posts))
        setStudyPostsStudyId(activeStudy.studyId)
        setPostPage(1)
      } catch (error) {
        if (cancelled || requestToken !== postListRequestTokenRef.current) {
          return
        }

        showToast(error instanceof Error ? error.message : '게시글 목록을 불러오지 못했어요.')
      } finally {
        if (!cancelled && requestToken === postListRequestTokenRef.current) {
          setIsPostsLoading(false)
        }
      }
    }

    void fetchStudyPosts()

    return () => {
      cancelled = true
    }
  }, [activeStudy?.studyId, sessionUserId, showToast])

  async function reload() {
    await refreshDashboard()
  }

  function closeOverview() {
    overviewRequestTokenRef.current += 1
    setShowOverview(false)
    setOverviewStudyDetail(null)
    setIsOverviewLoading(false)
  }

  async function openOverview() {
    if (!sessionUserId || !activeStudy) {
      return
    }

    const requestToken = ++overviewRequestTokenRef.current
    setShowOverview(true)
    setOverviewStudyDetail(null)
    setIsOverviewLoading(true)

    try {
      const detail = await api.getStudyDetail(sessionUserId, activeStudy.studyId)

      if (requestToken !== overviewRequestTokenRef.current) {
        return
      }

      setOverviewStudyDetail(detail)
    } catch (error) {
      if (requestToken !== overviewRequestTokenRef.current) {
        return
      }

      showToast(error instanceof Error ? error.message : '스터디 개요를 불러오지 못했어요.')
    } finally {
      if (requestToken === overviewRequestTokenRef.current) {
        setIsOverviewLoading(false)
      }
    }
  }

  async function loadStudyPosts(studyId: number, page = 1) {
    if (!sessionUserId) {
      return
    }

    const requestToken = ++postListRequestTokenRef.current
    setIsPostsLoading(true)

    try {
      const posts = await api.getStudyPosts(sessionUserId, studyId, 'POST')

      if (requestToken !== postListRequestTokenRef.current) {
        return
      }

      setStudyPosts(toRenderedPosts(posts))
      setStudyPostsStudyId(studyId)
      setPostPage(page)
    } catch (error) {
      if (requestToken !== postListRequestTokenRef.current) {
        return
      }

      showToast(error instanceof Error ? error.message : '게시글 목록을 불러오지 못했어요.')
    } finally {
      if (requestToken === postListRequestTokenRef.current) {
        setIsPostsLoading(false)
      }
    }
  }

  function closePostModal() {
    postRequestTokenRef.current += 1
    setSelectedPostId(null)
    setPostDetail(null)
    setCommentDraft('')
    setIsPostDetailLoading(false)
    setIsCommentSubmitting(false)
    setShowPostModal(false)
  }

  async function loadPostDetail(postId: number) {
    if (!sessionUserId) {
      return
    }

    const requestToken = ++postRequestTokenRef.current
    setIsPostDetailLoading(true)

    try {
      const detail = await api.getPostDetail(sessionUserId, postId)

      if (requestToken !== postRequestTokenRef.current) {
        return
      }

      setPostDetail(detail)
    } catch (error) {
      if (requestToken !== postRequestTokenRef.current) {
        return
      }

      closePostModal()
      showToast(error instanceof Error ? error.message : '게시글을 불러오지 못했어요.')
    } finally {
      if (requestToken === postRequestTokenRef.current) {
        setIsPostDetailLoading(false)
      }
    }
  }

  async function openPostModal(postId: number) {
    if (!sessionUserId) {
      return
    }

    setSelectedPostId(postId)
    setPostDetail(null)
    setCommentDraft('')
    setShowPostModal(true)
    await loadPostDetail(postId)
  }

  async function openAttendanceModal(sessionId: number) {
    if (!sessionUserId) {
      return
    }

    try {
      const nextPanel = await api.getSessionAttendancePanel(sessionUserId, sessionId)
      setAttendancePanel(nextPanel)
      setAttendanceMap(toAttendanceMap(nextPanel.roster))
      setShowAttendance(true)
    } catch (error) {
      showToast(error instanceof Error ? error.message : '출석 명단을 불러오지 못했어요.')
    }
  }

  function openSessionEditor() {
    if (!activeStudy?.isLeader) {
      return
    }

    setSessionDrafts(buildSessionEditorDrafts(activeStudy.sessions))
    setShowSessionEditor(true)
  }

  function closeSessionEditor() {
    if (isSessionSaving) {
      return
    }

    setShowSessionEditor(false)
    setSessionDrafts([])
  }

  function updateSessionDraft(sessionId: number, patch: Partial<SessionEditorDraft>) {
    setSessionDrafts((current) =>
      sortSessionEditorDrafts(
        current.map((session) =>
          session.sessionId === sessionId ? { ...session, ...patch } : session,
        ),
      ),
    )
  }

  function handleSessionDraftTypeChange(sessionId: number, nextType: 'REGULAR' | 'BREAK') {
    setSessionDrafts((current) =>
      sortSessionEditorDrafts(
        current.map((session, index) => {
          if (session.sessionId !== sessionId || !session.editable) {
            return session
          }

          if (nextType === 'BREAK') {
            return {
              ...session,
              title: BREAK_TITLE,
              sessionType: 'BREAK',
            }
          }

          return {
            ...session,
            title:
              session.title === BREAK_TITLE
                ? buildDefaultEditableSessionTitle(index)
                : session.title,
            sessionType: 'REGULAR',
          }
        }),
      ),
    )
  }

  async function handleSessionEditorSave() {
    if (!sessionUserId || !activeStudy?.isLeader) {
      return
    }

    if (
      sessionDrafts.some(
        (session) => session.sessionType === 'REGULAR' && !session.title.trim(),
      )
    ) {
      showToast('진행 회차 제목을 입력해주세요.')
      return
    }

    try {
      setIsSessionSaving(true)
      await api.updateStudySessions(
        sessionUserId,
        activeStudy.studyId,
        sessionDrafts.map((session) => ({
          sessionId: session.sessionId,
          title: session.sessionType === 'BREAK' ? BREAK_TITLE : session.title.trim(),
          scheduledAt: session.scheduledAt,
          sessionType: session.sessionType,
        })),
      )
      await reload()
      setShowSessionEditor(false)
      setSessionDrafts([])
      showToast('회차 정보를 수정했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '회차 정보를 수정하지 못했어요.')
    } finally {
      setIsSessionSaving(false)
    }
  }

  async function handleParticipation(sessionId: number, planned: boolean) {
    if (!sessionUserId) {
      return
    }

    try {
      await api.updateParticipation(sessionUserId, sessionId, planned)
      await reload()
      showToast(planned ? '참여 예정으로 표시했어요.' : '참여 예정 표시를 해제했어요.')
    } catch (error) {
      showToast(
        error instanceof Error ? error.message : '참여 여부를 업데이트하지 못했어요.',
      )
    }
  }

  async function handleAttendanceSubmit() {
    if (!sessionUserId || !attendancePanel?.sessionId) {
      return
    }

    try {
      const entries = Object.entries(attendanceMap).map(([userId, status]) => ({
        userId: Number(userId),
        status,
      }))

      await api.updateAttendance(sessionUserId, attendancePanel.sessionId, entries)
      await refreshMe()
      await reload()
      setAttendancePanel(null)
      setShowAttendance(false)
      showToast('출석 체크를 반영했어요. 출석한 멤버에게 체크 2개가 지급됩니다.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '출석 체크를 저장하지 못했어요.')
    }
  }

  async function handleCreatePost() {
    if (!sessionUserId || !activeStudy || !draftTitle.trim() || !draftContent.trim()) {
      return
    }

    try {
      await api.createPost(sessionUserId, activeStudy.studyId, {
        type: draftType,
        title: draftTitle.trim(),
        content: draftContent.trim(),
      })
      setDraftTitle('')
      setDraftContent('')
      setDraftType('POST')
      setShowComposer(false)
      await reload()
      await loadStudyPosts(activeStudy.studyId, 1)
      showToast('게시글을 등록했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '게시글 등록에 실패했어요.')
    }
  }

  async function handleCreateComment() {
    if (!sessionUserId || !selectedPostId || !commentDraft.trim()) {
      return
    }

    try {
      setIsCommentSubmitting(true)
      await api.createPostComment(sessionUserId, selectedPostId, {
        content: commentDraft.trim(),
      })
      setCommentDraft('')
      await loadPostDetail(selectedPostId)
      showToast('댓글을 등록했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '댓글 등록에 실패했어요.')
    } finally {
      setIsCommentSubmitting(false)
    }
  }

  async function handleStudyAction() {
    if (!sessionUserId || !activeStudy || isStudyActionPending) {
      return
    }

    const isLeader = activeStudy.isLeader
    const confirmed = window.confirm(
      isLeader
        ? '스터디를 종료하면 모든 멤버의 참여가 종료되고 목록에서도 내려갑니다. 계속할까요?'
        : '정말 이 스터디에서 탈퇴할까요?',
    )

    if (!confirmed) {
      return
    }

    try {
      setIsStudyActionPending(true)
      closeOverview()

      if (isLeader) {
        await api.closeStudy(sessionUserId, activeStudy.studyId)
      } else {
        await api.leaveStudy(sessionUserId, activeStudy.studyId)
      }

      await refreshDashboard()
      showToast(isLeader ? '스터디를 종료했어요.' : '스터디에서 탈퇴했어요.')
    } catch (error) {
      showToast(
        error instanceof Error
          ? error.message
          : isLeader
            ? '스터디를 종료하지 못했어요.'
            : '스터디 탈퇴에 실패했어요.',
      )
    } finally {
      setIsStudyActionPending(false)
    }
  }

  if (isDashboardLoading && !dashboard) {
    return <div className="page-surface empty-surface">대시보드를 불러오는 중입니다.</div>
  }

  if (!dashboard) {
    return <div className="page-surface empty-surface">대시보드를 불러오지 못했어요.</div>
  }

  if (!activeStudy) {
    return <div className="page-surface empty-surface">가입한 스터디가 아직 없습니다.</div>
  }

  return (
    <section className="home-screen">
      <div className="home-content">
        <aside className="home-sidebar">
          <div className="home-sidebar__header">
            <h2>내가 가입한 스터디</h2>
          </div>

          {/* <div className="home-sidebar__tabs">
            <span className="filter-chip is-active">가입중</span>
            <span className="filter-chip">종료됨</span>
          </div> */}

          {/* <div className="home-sidebar__search">(아이콘) 검색 스터디 검색</div> */}

          <div className="home-sidebar__list">
            {dashboard?.joinedStudies.map((study) => (
              <article
                className={
                  study.studyId === activeStudy.studyId
                    ? 'home-study-card is-active'
                    : 'home-study-card'
                }
                key={study.studyId}
                onClick={() => handleStudyCardClick(study.studyId)}
              >
                <span
                  className={`study-type-badge is-${getStudyBadgeTone(study.typeLabel)}`}
                >
                  {study.typeLabel}
                </span>

                <div className="home-study-card__copy">
                  <strong>{study.title}</strong>
                  <span>{study.timeLabel}</span>
                  <span>{study.locationLabel}</span>
                </div>
              </article>
            ))}
          </div>
        </aside>

        <section
          className={
            detailMotionDirection
              ? `home-detail is-entering-from-${detailMotionDirection}`
              : 'home-detail'
          }
          key={`${activeStudy.studyId}-${detailMotionKey}`}
        >
          <header className="home-detail__header">
            <div className="home-detail__copy">
              <h2>{activeStudy.title}</h2>
              <p>{activeStudy.description}</p>
              <span>{scheduleLabel}</span>
            </div>

            <div className="home-detail__actions">
              <button
                className="soft-button"
                onClick={openOverview}
                type="button"
              >
                개요
              </button>
              {/* <button
                className="soft-button is-disabled"
                disabled
                title="공유 기능 준비 중"
                type="button"
              >
                공유
              </button> */}
              <button
                className="danger-button"
                disabled={isStudyActionPending}
                onClick={handleStudyAction}
                type="button"
              >
                {isStudyActionPending
                  ? activeStudy.isLeader
                    ? '종료 중...'
                    : '처리 중...'
                  : activeStudy.isLeader
                    ? '스터디 종료'
                    : '탈퇴'}
              </button>
            </div>
          </header>

          <div className="home-detail__body">
            <section className="timeline-column">
              <div className="timeline-column__header">
                <span className="section-kicker">SESSION TIMELINE</span>
                {activeStudy.isLeader ? (
                  <button className="soft-button" onClick={openSessionEditor} type="button">
                    수정
                  </button>
                ) : null}
              </div>

              <div className="home-timeline-viewport" ref={timelineViewportRef}>
                <div className="home-timeline-list">
                  {renderedTimeline.map((session) => {
                    const isBreak = session.sessionType === 'BREAK'
                    const isHighlighted =
                      !session.placeholder &&
                      !isBreak &&
                      session.sessionId === activeStudy.currentSessionId
                    const isPastSession =
                      !session.placeholder &&
                      !isBreak &&
                      !isHighlighted &&
                      session.nodeState !== 'FUTURE'
                    const tone = getTimelineToneByState(session.nodeState)
                    const canToggle = !isBreak && isHighlighted
                    const isInteractive =
                      !session.placeholder &&
                      !isBreak &&
                      (activeStudy.isLeader ? (isHighlighted || isPastSession) : canToggle)
                    const canLeaderOpenHighlightedSession =
                      isHighlighted && canLeaderStartSession(session.scheduledAtValue)
                    const memberChipTone =
                      isBreak
                        ? 'break'
                        : isHighlighted && !activeStudy.isLeader
                          ? session.planned ? 'planned' : 'missed'
                          : tone
                    const chipTone = isBreak
                      ? 'break'
                      : activeStudy.isLeader && isHighlighted
                        ? hasAttendanceStarted ? 'edit' : 'start'
                        : activeStudy.isLeader && isPastSession
                          ? 'edit'
                          : memberChipTone
                    const chipLabel = isBreak
                      ? '쉬어가는 회차'
                      : activeStudy.isLeader && isHighlighted
                        ? hasAttendanceStarted ? '수정' : '시작'
                        : activeStudy.isLeader && isPastSession
                          ? '수정'
                          : getMemberCurrentChipLabel(session.planned)
                    const showChip =
                      !isBreak &&
                      (session.placeholder ||
                        isHighlighted ||
                        (activeStudy.isLeader && isPastSession))
                    const cardClassName = [
                      'home-timeline-card',
                      isHighlighted ? 'is-highlighted' : '',
                      session.placeholder ? 'is-placeholder' : '',
                      isBreak ? 'is-break' : '',
                    ]
                      .filter(Boolean)
                      .join(' ')

                    return (
                      <div
                        className="home-timeline-row"
                        key={session.sessionId}
                        ref={isHighlighted ? currentTimelineRowRef : null}
                      >
                        <div className="home-timeline-row__rail" aria-hidden="true">
                          <span
                            className={getTimelineNodeClass(session.nodeState)}
                          />
                          <span className="home-timeline-connector" />
                        </div>

                        <div
                          className={cardClassName}
                          onClick={() => {
                            if (isBreak || session.placeholder) return
                            if (activeStudy.isLeader && typeof session.sessionId === 'number') {
                              if (isPastSession) {
                                return void openAttendanceModal(session.sessionId)
                              }
                              if (isHighlighted) {
                                if (!canLeaderOpenHighlightedSession) {
                                  showToast(LEADER_START_WINDOW_TOAST)
                                  return
                                }
                                return void openAttendanceModal(session.sessionId)
                              }
                            }
                            if (isInteractive && canToggle && typeof session.sessionId === 'number') return void handleParticipation(session.sessionId, !session.planned)
                          }}
                        >
                          <div className="home-timeline-card__meta">
                            <span>{session.roundLabel}</span>
                            <span>{session.scheduledAt}</span>
                          </div>
                          <strong>{session.title}</strong>
                          {showChip ? (
                            <span className={`status-chip is-${chipTone}`}>
                              {chipLabel}
                            </span>
                          ) : null}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </section>

            <aside className="board-column">
              <section className="home-notice-card">
                <div className="home-card__header">
                  <span className="section-kicker">공지</span>
                  {activeStudy.notice ? (
                    <span className="home-card__meta">{activeStudy.notice.createdAt}</span>
                  ) : null}
                </div>
                <strong>{activeStudy.notice?.title ?? '공지 없음'}</strong>
                <p>{activeStudy.notice?.content ?? '등록된 공지가 없습니다.'}</p>
              </section>

              <section className="home-posts-card">
                <div className="home-card__header">
                  <span className="section-kicker">게시글</span>
                  <button
                    className="soft-button"
                    onClick={() => setShowComposer(true)}
                    type="button"
                  >
                    글쓰기
                  </button>
                </div>

                <div className="home-posts-viewport">
                  {isPostsLoading && !currentStudyPosts.length ? (
                    <div className="home-posts-empty">게시글을 불러오는 중이에요</div>
                  ) : pagedPosts.length ? (
                    <div className="home-post-list">
                      {pagedPosts.map((post) => (
                        <article
                          className={
                            post.placeholder ? 'home-post-row is-placeholder' : 'home-post-row'
                          }
                          key={String(post.postId)}
                          onClick={() => {
                            if (!post.placeholder && typeof post.postId === 'number') {
                              void openPostModal(post.postId)
                            }
                          }}
                          onKeyDown={(event) => {
                            if (post.placeholder || typeof post.postId !== 'number') {
                              return
                            }

                            if (event.key === 'Enter' || event.key === ' ') {
                              event.preventDefault()
                              void openPostModal(post.postId)
                            }
                          }}
                          role="button"
                          tabIndex={post.placeholder ? -1 : 0}
                        >
                          <div className="home-post-row__copy">
                            <strong>{post.title}</strong>
                            {post.authorUserId ? (
                              <ProfileNameButton
                                className="profile-name-button is-inline"
                                nickname={post.authorNickname}
                                userId={post.authorUserId}
                              />
                            ) : (
                              <span>{post.authorNickname}</span>
                            )}
                          </div>
                          <time>{post.createdAt}</time>
                        </article>
                      ))}
                    </div>
                  ) : (
                    <div className="home-posts-empty">아직 게시글이 없어요</div>
                  )}
                </div>

                <div className="home-posts-divider" />

                <div className="pagination-row pagination-row--posts">
                  <div className="pagination-pages">
                    <button
                      className="pagination-ghost"
                      disabled={currentStudyPostPage === 1 || !currentStudyPosts.length}
                      onClick={() => setPostPage((currentPage) => Math.max(1, currentPage - 1))}
                      type="button"
                    >
                      이전
                    </button>

                    {currentStudyPosts.length ? (
                      <>
                        {shouldShowLeadingFirstPage ? (
                          <button
                            className="pagination-badge"
                            onClick={() => setPostPage(1)}
                            type="button"
                          >
                            1
                          </button>
                        ) : null}

                        {shouldShowLeadingEllipsis ? (
                          <span className="pagination-ellipsis" aria-hidden="true">
                            ...
                          </span>
                        ) : null}

                        {visiblePostPages.map((page) => (
                          <button
                            aria-current={page === currentStudyPostPage ? 'page' : undefined}
                            className={
                              page === currentStudyPostPage
                                ? 'pagination-badge is-active'
                                : 'pagination-badge'
                            }
                            disabled={page === currentStudyPostPage}
                            key={page}
                            onClick={() => setPostPage(page)}
                            type="button"
                          >
                            {page}
                          </button>
                        ))}

                        {shouldShowTrailingEllipsis ? (
                          <span className="pagination-ellipsis" aria-hidden="true">
                            ...
                          </span>
                        ) : null}

                        {shouldShowTrailingLastPage ? (
                          <button
                            className="pagination-badge"
                            onClick={() => setPostPage(totalPostPages)}
                            type="button"
                          >
                            {totalPostPages}
                          </button>
                        ) : null}
                      </>
                    ) : (
                      <span className="pagination-empty">페이지 없음</span>
                    )}

                    <button
                      className="pagination-ghost"
                      disabled={currentStudyPostPage === totalPostPages || !currentStudyPosts.length}
                      onClick={() => setPostPage((currentPage) => Math.min(totalPostPages, currentPage + 1))}
                      type="button"
                    >
                      다음
                    </button>
                  </div>
                  <button
                    className="soft-button"
                    onClick={() => setShowComposer(true)}
                    type="button"
                  >
                    글쓰기
                  </button>
                </div>
              </section>
            </aside>
          </div>
        </section>
      </div >

      {
        showSessionEditor ? (
          <div
            className="modal-backdrop"
            onClick={closeSessionEditor}
            role="presentation"
          >
            <article
              className="modal-card session-editor-modal"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="modal-card__header">
                <div>
                  <span className="section-kicker">회차 수정</span>
                  <h2>{activeStudy.title}</h2>
                  <p className="modal-description">
                    출석이 마감된 완료 회차는 잠금 상태로 보여요.
                  </p>
                </div>
                <button
                  className="soft-button"
                  onClick={closeSessionEditor}
                  type="button"
                >
                  닫기
                </button>
              </div>

              <div className="session-builder-actions session-editor-summary">
                <span className="count-badge">진행 {sessionDraftRegularCount}개</span>
                {sessionDraftBreakCount ? (
                  <span className="count-badge is-warm">휴차 {sessionDraftBreakCount}개</span>
                ) : null}
              </div>

              <div className="session-plan-list session-editor-list">
                {sessionDrafts.map((session, index) => {
                  const isBreak = session.sessionType === 'BREAK'
                  const cardClassName = [
                    'session-plan-card',
                    isBreak ? 'is-break' : '',
                    !session.editable ? 'is-locked' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')

                  return (
                    <article className={cardClassName} key={session.sessionId}>
                      <div className="session-plan-card__header">
                        <div>
                          <span className="field-label">{index + 1}회차</span>
                          {!session.editable ? (
                            <strong className="session-editor-lock-label">완료 회차</strong>
                          ) : null}
                        </div>

                        <div className="filter-chip-row">
                          <button
                            className={
                              !isBreak ? 'filter-chip is-active' : 'filter-chip'
                            }
                            disabled={!session.editable}
                            onClick={() =>
                              handleSessionDraftTypeChange(session.sessionId, 'REGULAR')
                            }
                            type="button"
                          >
                            진행
                          </button>
                          <button
                            className={
                              isBreak ? 'filter-chip is-active warm' : 'filter-chip warm'
                            }
                            disabled={!session.editable}
                            onClick={() =>
                              handleSessionDraftTypeChange(session.sessionId, 'BREAK')
                            }
                            type="button"
                          >
                            휴차
                          </button>
                        </div>
                      </div>

                      <div className="session-plan-card__body">
                        <label className="field-block">
                          <span className="field-label">일시</span>
                          <input
                            className="field-control"
                            disabled={!session.editable}
                            onChange={(event) =>
                              updateSessionDraft(session.sessionId, {
                                scheduledAt: event.target.value,
                              })
                            }
                            type="datetime-local"
                            value={session.scheduledAt}
                          />
                        </label>

                        <label className="field-block">
                          <span className="field-label">회차 제목</span>
                          <input
                            className="field-control"
                            disabled={!session.editable || isBreak}
                            onChange={(event) =>
                              updateSessionDraft(session.sessionId, {
                                title: event.target.value,
                              })
                            }
                            value={isBreak ? BREAK_TITLE : session.title}
                          />
                        </label>
                      </div>
                    </article>
                  )
                })}
              </div>

              <div className="session-editor-footer">
                <button className="soft-button" onClick={closeSessionEditor} type="button">
                  취소
                </button>
                <button
                  className="primary-button"
                  disabled={isSessionSaving}
                  onClick={handleSessionEditorSave}
                  type="button"
                >
                  {isSessionSaving ? '저장 중...' : '저장'}
                </button>
              </div>
            </article>
          </div>
        ) : null
      }

      {
        showOverview ? (
          <div
            className="modal-backdrop"
            onClick={closeOverview}
            role="presentation"
          >
            <article
              className="modal-card overview-modal study-overview-modal"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="modal-card__header">
                <span className="section-kicker">스터디 개요</span>
                <button
                  className="soft-button"
                  onClick={closeOverview}
                  type="button"
                >
                  닫기
                </button>
              </div>

              {isOverviewLoading ? (
                <div className="study-overview-modal__state">
                  {activeStudy.title} 개요를 불러오는 중이에요.
                </div>
              ) : overviewStudyDetail ? (
                <StudyOverviewSheet study={overviewStudyDetail} />
              ) : (
                <div className="study-overview-modal__state is-error">
                  스터디 개요를 불러오지 못했어요.
                </div>
              )}
            </article>
          </div>
        ) : null
      }

      {
        showPostModal ? (
          <div
            className="modal-backdrop"
            onClick={closePostModal}
            role="presentation"
          >
            <article
              className="modal-card post-detail-modal"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="modal-card__header">
                <div>
                  <span className="section-kicker">게시글 상세</span>
                  <h2>{postDetail?.title ?? selectedPostSummary?.title ?? '게시글'}</h2>
                  {postDetail ? (
                    <div className="post-detail-meta">
                      <ProfileNameButton
                        className="profile-name-button is-inline"
                        nickname={postDetail.authorNickname}
                        userId={postDetail.authorUserId}
                      />
                      <time>{postDetail.createdAt}</time>
                    </div>
                  ) : null}
                </div>
                <button
                  className="soft-button"
                  onClick={closePostModal}
                  type="button"
                >
                  닫기
                </button>
              </div>

              {isPostDetailLoading ? (
                <div className="post-detail-loading">게시글을 불러오는 중이에요.</div>
              ) : postDetail ? (
                <div className="post-detail-layout">
                  <section className="post-detail-body">
                    <span className="section-kicker">본문</span>
                    <div className="post-detail-body__content">
                      <p>{postDetail.content}</p>
                    </div>
                  </section>

                  <section className="post-comment-composer post-comment-composer-panel">
                    <div className="post-comments-panel__header">
                      <span className="section-kicker">댓글 작성</span>
                    </div>

                    <textarea
                      className="field-control textarea-field post-comment-textarea"
                      disabled={isCommentSubmitting}
                      onChange={(event) => setCommentDraft(event.target.value)}
                      placeholder="댓글을 입력하세요"
                      value={commentDraft}
                    />

                    <div className="post-comment-composer__footer">
                      <button
                        className="primary-button"
                        disabled={isCommentSubmitting || !commentDraft.trim()}
                        onClick={handleCreateComment}
                        type="button"
                      >
                        댓글 등록
                      </button>
                    </div>
                  </section>

                  <section className="post-comments-panel">
                    <div className="post-comments-panel__header">
                      <span className="section-kicker">댓글</span>
                      <span className="home-card__meta">{postDetail.comments.length}개</span>
                    </div>

                    <div className="post-comments-list">
                      {postDetail.comments.length ? (
                        postDetail.comments.map((comment) => (
                          <article className="post-comment-row" key={comment.commentId}>
                            <div className="post-comment-row__meta">
                              <ProfileNameButton
                                className="profile-name-button is-inline is-strong"
                                nickname={comment.authorNickname}
                                userId={comment.authorUserId}
                              />
                              <time>{comment.createdAt}</time>
                            </div>
                            <p>{comment.content}</p>
                          </article>
                        ))
                      ) : (
                        <div className="post-comments-empty">
                          아직 댓글이 없어요. 첫 댓글을 남겨보세요.
                        </div>
                      )}
                    </div>
                  </section>
                </div>
              ) : null}
            </article>
          </div>
        ) : null
      }

      {
        showComposer ? (
          <div
            className="modal-backdrop"
            onClick={() => setShowComposer(false)}
            role="presentation"
          >
            <article
              className="modal-card composer-modal"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="modal-card__header">
                <div>
                  <span className="section-kicker">게시글 작성</span>
                  <h2>스터디 보드에 글 남기기</h2>
                </div>
                <button
                  className="soft-button"
                  onClick={() => setShowComposer(false)}
                  type="button"
                >
                  닫기
                </button>
              </div>

              <div className="composer-layout">
                <select
                  className="field-control"
                  onChange={(event) => setDraftType(event.target.value as 'POST' | 'NOTICE')}
                  value={draftType}
                >
                  <option value="POST">게시글</option>
                  {activeStudy.isLeader ? <option value="NOTICE">공지</option> : null}
                </select>
                <input
                  className="field-control"
                  onChange={(event) => setDraftTitle(event.target.value)}
                  placeholder="제목"
                  value={draftTitle}
                />
                <textarea
                  className="field-control textarea-field"
                  onChange={(event) => setDraftContent(event.target.value)}
                  placeholder="내용을 입력해 주세요."
                  value={draftContent}
                />
                <button className="primary-button" onClick={handleCreatePost} type="button">
                  등록
                </button>
              </div>
            </article>
          </div>
        ) : null
      }

      {
        showAttendance ? (
          <div
            className="modal-backdrop"
            onClick={() => {
              setAttendancePanel(null)
              setShowAttendance(false)
            }}
            role="presentation"
          >
            <article
              className="modal-card attendance-modal"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="modal-card__header">
                <div>
                  <span className="section-kicker">출석 체크</span>
                  <h2>{attendancePanel?.sessionLabel ?? activeStudy.attendanceSessionLabel ?? '이번 회차'}</h2>
                </div>
                <button
                  className="soft-button"
                  onClick={() => {
                    setAttendancePanel(null)
                    setShowAttendance(false)
                  }}
                  type="button"
                >
                  닫기
                </button>
              </div>

              <div className="attendance-list">
                {(attendancePanel?.roster ?? activeStudy.attendanceRoster).map((member) => (
                  <div className="attendance-row" key={member.userId}>
                    <div className="attendance-row__member">
                      <ProfileNameButton
                        className="profile-name-button is-inline is-strong"
                        nickname={member.nickname}
                        userId={member.userId}
                      />
                    </div>

                    <div className="attendance-actions">
                      <button
                        className={
                          attendanceMap[member.userId] === 'PRESENT'
                            ? 'attendance-chip is-present is-selected'
                            : 'attendance-chip is-present'
                        }
                        onClick={() =>
                          setAttendanceMap((current) => ({
                            ...current,
                            [member.userId]: 'PRESENT',
                          }))
                        }
                        type="button"
                      >
                        출석
                      </button>
                      <button
                        className={
                          attendanceMap[member.userId] === 'ABSENT'
                            ? 'attendance-chip is-absent is-selected'
                            : 'attendance-chip is-absent'
                        }
                        onClick={() =>
                          setAttendanceMap((current) => ({
                            ...current,
                            [member.userId]: 'ABSENT',
                          }))
                        }
                        type="button"
                      >
                        결석
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              <button className="primary-button" onClick={handleAttendanceSubmit} type="button">
                출석 체크 완료
              </button>
            </article>
          </div>
        ) : null
      }
    </section >
  )
}
