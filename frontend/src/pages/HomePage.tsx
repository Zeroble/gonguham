import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  api,
  type AttendanceRosterEntry,
  type DashboardResponse,
  type PostDetail,
  type SessionAttendancePanel,
} from '../app/api'
import { useApp } from '../app/useApp'

type RenderedTimelineItem = {
  sessionId: number | string
  roundLabel: string
  title: string
  nodeState: string
  scheduledAt: string
  planned: boolean
  sessionType: string
  placeholder?: boolean
}

type RenderedPostItem = {
  postId: number | string
  title: string
  authorNickname: string
  createdAt: string
  placeholder?: boolean
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

export function HomePage() {
  const { sessionUserId, refreshMe, showToast } = useApp()
  const [searchParams, setSearchParams] = useSearchParams()
  const timelineViewportRef = useRef<HTMLDivElement | null>(null)
  const currentTimelineRowRef = useRef<HTMLDivElement | null>(null)
  const postRequestTokenRef = useRef(0)
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [draftTitle, setDraftTitle] = useState('')
  const [draftContent, setDraftContent] = useState('')
  const [draftType, setDraftType] = useState<'POST' | 'NOTICE'>('POST')
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [postDetail, setPostDetail] = useState<PostDetail | null>(null)
  const [commentDraft, setCommentDraft] = useState('')
  const [attendanceMap, setAttendanceMap] = useState<Record<number, string>>({})
  const [attendancePanel, setAttendancePanel] = useState<SessionAttendancePanel | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isPostDetailLoading, setIsPostDetailLoading] = useState(false)
  const [isCommentSubmitting, setIsCommentSubmitting] = useState(false)
  const [showOverview, setShowOverview] = useState(false)
  const [showPostModal, setShowPostModal] = useState(false)
  const [showComposer, setShowComposer] = useState(false)
  const [showAttendance, setShowAttendance] = useState(false)
  const selectedStudyId = useMemo(() => {
    const value = Number(searchParams.get('studyId'))
    return Number.isFinite(value) && value > 0 ? value : null
  }, [searchParams])

  useEffect(() => {
    if (!sessionUserId) {
      return
    }

    let cancelled = false

    async function fetchDashboard() {
      setIsLoading(true)

      try {
        const next = await api.getDashboard(sessionUserId)

        if (!cancelled) {
          setDashboard(next)
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void fetchDashboard()

    return () => {
      cancelled = true
    }
  }, [sessionUserId])

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

  const hasAttendanceStarted = useMemo(
    () => activeStudy?.attendanceRoster.some((member) => member.attendanceStatus !== null) ?? false,
    [activeStudy],
  )

  useEffect(() => {
    setAttendanceMap(toAttendanceMap(activeStudy?.attendanceRoster))
  }, [activeStudy])

  useEffect(() => {
    if (!sessionUserId || !activeStudy?.studyId) {
      return
    }

    let cancelled = false

    async function refreshStudyPanel() {
      const nextPanel = await api.getStudyHomePanel(sessionUserId, activeStudy.studyId)

      if (!cancelled) {
        setDashboard((current) =>
          current
            ? {
              ...current,
              studyPanels: current.studyPanels.map((panel) =>
                panel.studyId === nextPanel.studyId ? nextPanel : panel,
              ),
            }
            : current,
        )
      }
    }

    void refreshStudyPanel()

    return () => {
      cancelled = true
    }
  }, [activeStudy?.studyId, sessionUserId])

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

  const renderedPosts = useMemo<RenderedPostItem[]>(() => {
    if (!activeStudy) {
      return []
    }

    return [
      ...activeStudy.posts.map((post) => ({
        postId: post.postId,
        title: post.title,
        authorNickname: post.authorNickname,
        createdAt: post.createdAt,
      }))
    ]
  }, [activeStudy])

  const selectedPostSummary = useMemo(
    () => renderedPosts.find((post) => typeof post.postId === 'number' && post.postId === selectedPostId) ?? null,
    [renderedPosts, selectedPostId],
  )

  useEffect(() => {
    postRequestTokenRef.current += 1
    setSelectedPostId(null)
    setPostDetail(null)
    setCommentDraft('')
    setIsPostDetailLoading(false)
    setIsCommentSubmitting(false)
    setShowPostModal(false)
  }, [activeStudy?.studyId])

  async function reload() {
    if (!sessionUserId || !activeStudy?.studyId) {
      return
    }

    const nextPanel = await api.getStudyHomePanel(sessionUserId, activeStudy.studyId)
    setDashboard((current) =>
      current
        ? {
          ...current,
          studyPanels: current.studyPanels.map((panel) =>
            panel.studyId === nextPanel.studyId ? nextPanel : panel,
          ),
        }
        : current,
    )
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

  if (isLoading) {
    return <div className="page-surface empty-surface">대시보드를 불러오는 중입니다.</div>
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

          <div className="home-sidebar__tabs">
            <span className="filter-chip is-active">가입중</span>
            <span className="filter-chip">종료됨</span>
          </div>

          <div className="home-sidebar__search">(아이콘) 검색 스터디 검색</div>

          <div className="home-sidebar__list">
            {dashboard?.joinedStudies.map((study) => (
              <article
                className={
                  study.studyId === activeStudy.studyId
                    ? 'home-study-card is-active'
                    : 'home-study-card'
                }
                key={study.studyId}
                onClick={() => setSearchParams({ studyId: String(study.studyId) })}
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

        <section className="home-detail">
          <header className="home-detail__header">
            <div className="home-detail__copy">
              <h2>{activeStudy.title}</h2>
              <p>{activeStudy.description}</p>
              <span>{scheduleLabel}</span>
            </div>

            <div className="home-detail__actions">
              <button
                className="soft-button"
                onClick={() => setShowOverview(true)}
                type="button"
              >
                개요
              </button>
              <button
                className="soft-button"
                onClick={() => showToast('공유 기능은 데모에서 링크 복사로 연결할 예정이에요.')}
                type="button"
              >
                공유
              </button>
              <button
                className="soft-button"
                onClick={() => showToast('탈퇴 기능은 이번 데모 범위에서 제외했어요.')}
                type="button"
              >
                탈퇴
              </button>
            </div>
          </header>

          <div className="home-detail__body">
            <section className="timeline-column">
              <span className="section-kicker">SESSION TIMELINE</span>

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
                            if (activeStudy.isLeader && typeof session.sessionId === 'number' && (isHighlighted || isPastSession)) return void openAttendanceModal(session.sessionId)
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
              {activeStudy.notice ? (
                <section className="home-notice-card">
                  <div className="home-card__header">
                    <span className="section-kicker">공지</span>
                    <span className="home-card__meta">{activeStudy.notice.createdAt}</span>
                  </div>
                  <strong>{activeStudy.notice.title}</strong>
                  <p>{activeStudy.notice.content}</p>
                </section>
              ) : null}

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
                  <div className="home-post-list">
                    {renderedPosts.map((post) => (
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
                          <span>{post.authorNickname}</span>
                        </div>
                        <time>{post.createdAt}</time>
                      </article>
                    ))}
                  </div>
                </div>

                <div className="home-posts-divider" />

                <div className="pagination-row pagination-row--posts">
                  <div className="pagination-pages">
                    <button className="pagination-ghost" type="button">
                      이전
                    </button>
                    <span className="pagination-badge is-active">1</span>
                    <span className="pagination-badge">2</span>
                    <span className="pagination-badge">3</span>
                    <button className="pagination-ghost" type="button">
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
      </div>

      {showOverview ? (
        <div
          className="modal-backdrop"
          onClick={() => setShowOverview(false)}
          role="presentation"
        >
          <article
            className="modal-card overview-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="modal-card__header">
              <div>
                <span className="section-kicker">스터디 개요</span>
                <h2>{activeStudy.title}</h2>
              </div>
              <button
                className="soft-button"
                onClick={() => setShowOverview(false)}
                type="button"
              >
                닫기
              </button>
            </div>

            <p className="modal-description">{activeStudy.description}</p>

            <div className="info-grid">
              <div className="info-tile">
                <span>장소</span>
                <strong>{activeStudy.locationText}</strong>
              </div>
              <div className="info-tile">
                <span>운영 역할</span>
                <strong>{activeStudy.isLeader ? '스터디장' : '멤버'}</strong>
              </div>
              <div className="info-tile">
                <span>진행 일정</span>
                <strong>{scheduleLabel}</strong>
              </div>
              <div className="info-tile">
                <span>현재 게시글</span>
                <strong>{activeStudy.posts.length}개</strong>
              </div>
            </div>
          </article>
        </div>
      ) : null}

      {showPostModal ? (
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
                    <span>{postDetail.authorNickname}</span>
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
                  <p>{postDetail.content}</p>
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
                            <strong>{comment.authorNickname}</strong>
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

                  <div className="post-comment-composer">
                    <textarea
                      className="field-control textarea-field post-comment-textarea"
                      disabled={isCommentSubmitting}
                      onChange={(event) => setCommentDraft(event.target.value)}
                      placeholder="댓글을 입력하세요"
                      value={commentDraft}
                    />

                    <div className="post-comment-composer__footer">
                      <div style={{ flex: 1 }}></div>
                      <button
                        className="primary-button"
                        disabled={isCommentSubmitting || !commentDraft.trim()}
                        onClick={handleCreateComment}
                        type="button"
                      >
                        댓글 등록
                      </button>
                    </div>
                  </div>
                </section>
              </div>
            ) : null}
          </article>
        </div>
      ) : null}

      {showComposer ? (
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
      ) : null}

      {showAttendance ? (
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
                    <strong>{member.nickname}</strong>
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
      ) : null}
    </section>
  )
}
