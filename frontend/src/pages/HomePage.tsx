import { useEffect, useMemo, useState } from 'react'
import { api, type DashboardResponse } from '../app/api'
import { useApp } from '../app/useApp'

type RenderedTimelineItem = {
  sessionId: number | string
  roundLabel: string
  title: string
  statusLabel: string
  scheduledAt: string
  placeholder?: boolean
}

function getTimelineTone(statusLabel: string) {
  if (statusLabel.includes('참여')) return 'planned'
  if (statusLabel.includes('출석')) return 'done'
  if (statusLabel.includes('결석')) return 'missed'
  if (statusLabel.includes('미응답')) return 'pending'
  return 'muted'
}

function getStudyBadgeTone(label: string) {
  if (label.includes('주제')) return 'topic'
  if (label.includes('반짝')) return 'flash'
  return 'mogak'
}

export function HomePage() {
  const { sessionUserId, refreshMe } = useApp()
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [draftTitle, setDraftTitle] = useState('')
  const [draftContent, setDraftContent] = useState('')
  const [draftType, setDraftType] = useState<'POST' | 'NOTICE'>('POST')
  const [attendanceMap, setAttendanceMap] = useState<Record<number, string>>({})
  const [message, setMessage] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [showOverview, setShowOverview] = useState(false)
  const [showComposer, setShowComposer] = useState(false)
  const [showAttendance, setShowAttendance] = useState(false)

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
          setAttendanceMap(
            Object.fromEntries(
              (next.activeStudy?.attendanceRoster ?? []).map((member) => [
                member.userId,
                member.attendanceStatus ?? (member.planned ? 'PRESENT' : 'ABSENT'),
              ]),
            ),
          )
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

  const activeStudy = dashboard?.activeStudy

  const scheduleLabel = useMemo(() => {
    if (!activeStudy?.sessions.length) {
      return activeStudy?.locationText ?? ''
    }

    return `${activeStudy.sessions[0].scheduledAt} · ${activeStudy.locationText}`
  }, [activeStudy])

  const renderedTimeline = useMemo<RenderedTimelineItem[]>(() => {
    if (!activeStudy) {
      return []
    }

    const placeholdersNeeded = Math.max(0, 7 - activeStudy.sessions.length)

    return [
      ...activeStudy.sessions,
      ...Array.from({ length: placeholdersNeeded }, (_, index) => ({
        sessionId: `placeholder-${index}`,
        roundLabel: `${activeStudy.sessions.length + index + 1}회차`,
        title: '예정된 회차',
        statusLabel: '준비중',
        scheduledAt: '추후 공개',
        placeholder: true,
      })),
    ]
  }, [activeStudy])

  const renderedPosts = useMemo(() => {
    if (!activeStudy) {
      return []
    }

    const placeholdersNeeded = Math.max(0, 7 - activeStudy.posts.length)

    return [
      ...activeStudy.posts.map((post) => ({ ...post, placeholder: false })),
      ...Array.from({ length: placeholdersNeeded }, (_, index) => ({
        postId: `placeholder-${index}`,
        title: '오늘 스터디 끝나고 남아서 같이 복습하실 분 있나요?',
        authorNickname: '김민수',
        createdAt: '오늘 17:20',
        placeholder: true,
      })),
    ]
  }, [activeStudy])

  async function reload() {
    if (!sessionUserId) {
      return
    }

    const next = await api.getDashboard(sessionUserId)
    setDashboard(next)
    setAttendanceMap(
      Object.fromEntries(
        (next.activeStudy?.attendanceRoster ?? []).map((member) => [
          member.userId,
          member.attendanceStatus ?? (member.planned ? 'PRESENT' : 'ABSENT'),
        ]),
      ),
    )
  }

  async function handleParticipation(sessionId: number, planned: boolean) {
    if (!sessionUserId) {
      return
    }

    try {
      await api.updateParticipation(sessionUserId, sessionId, planned)
      await reload()
      setMessage(planned ? '참여 예정으로 표시했어요.' : '참여 예정 표시를 해제했어요.')
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : '참여 여부를 업데이트하지 못했어요.',
      )
    }
  }

  async function handleAttendanceSubmit() {
    if (!sessionUserId || !activeStudy?.attendanceSessionId) {
      return
    }

    try {
      const entries = Object.entries(attendanceMap).map(([userId, status]) => ({
        userId: Number(userId),
        status,
      }))

      await api.updateAttendance(sessionUserId, activeStudy.attendanceSessionId, entries)
      await refreshMe()
      await reload()
      setShowAttendance(false)
      setMessage('출석 체크를 반영했어요. 출석한 멤버에게 체크 2개가 지급됩니다.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '출석 체크를 저장하지 못했어요.')
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
      setMessage('게시글을 등록했어요.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '게시글 등록에 실패했어요.')
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
      {message ? <div className="message-banner">{message}</div> : null}

      <div className="home-content-grid">
        <aside className="study-sidebar-card">
          <div className="surface-heading">
            <div>
              <h2>내가 가입한 스터디</h2>
              <p>가입중 / 종료됨</p>
            </div>
          </div>

          <div className="sidebar-toggle-row">
            <span className="filter-chip is-active">가입중</span>
            <span className="filter-chip">종료됨</span>
          </div>

          <div className="sidebar-searchbox">(아이콘) 검색 스터디 검색</div>

          <div className="joined-study-list">
            {dashboard?.joinedStudies.map((study) => (
              <article
                className={
                  study.studyId === activeStudy.studyId
                    ? 'joined-study-tile is-active'
                    : 'joined-study-tile'
                }
                key={study.studyId}
              >
                <span
                  className={`study-type-badge is-${getStudyBadgeTone(study.typeLabel)}`}
                >
                  {study.typeLabel}
                </span>
                <div className="joined-study-copy">
                  <strong>{study.title}</strong>
                  <span>{study.scheduleLabel}</span>
                </div>
              </article>
            ))}
          </div>
        </aside>

        <section className="study-detail-card">
          <header className="study-detail-header">
            <div className="study-hero__copy">
              <h2>{activeStudy.title}</h2>
              <span>{activeStudy.description}</span>
              <span>{scheduleLabel}</span>
            </div>

            <div className="hero-actions">
              <button
                className="soft-button"
                onClick={() => setShowOverview(true)}
                type="button"
              >
                개요
              </button>
              <button
                className="soft-button"
                onClick={() => setMessage('공유 기능은 데모에서 링크 복사로 연결할 예정이에요.')}
                type="button"
              >
                공유
              </button>
              <button
                className="soft-button"
                onClick={() => setMessage('탈퇴 기능은 이번 데모 범위에서 제외했어요.')}
                type="button"
              >
                탈퇴
              </button>
            </div>
          </header>

          <div className="study-detail-grid">
            <section className="timeline-pane notice-panel">
              <span className="section-kicker">SESSION TIMELINE</span>

              <div className="timeline-viewport">
                <div className="timeline-list">
                  {renderedTimeline.map((session, index) => {
                    const isHighlighted = index === 0 && !session.placeholder
                    const tone = getTimelineTone(session.statusLabel)
                    const canToggle =
                      session.statusLabel === '참여 예정' ||
                      session.statusLabel === '미응답'
                    const isInteractive =
                      !session.placeholder &&
                      (activeStudy.isLeader ? isHighlighted : canToggle)

                    return (
                      <div className="timeline-row" key={session.sessionId}>
                        <div className="timeline-row__rail" aria-hidden="true">
                          <span
                            className={
                              isHighlighted
                                ? 'timeline-node is-highlighted'
                                : 'timeline-node'
                            }
                          />
                          {index < renderedTimeline.length - 1 ? (
                            <span className="timeline-connector" />
                          ) : null}
                        </div>

                        <button
                          className={
                            isHighlighted
                              ? 'timeline-card is-highlighted'
                              : session.placeholder
                                ? 'timeline-card is-placeholder'
                                : 'timeline-card'
                          }
                          disabled={!isInteractive}
                          onClick={() => {
                            if (session.placeholder) {
                              return
                            }

                            if (activeStudy.isLeader && isHighlighted) {
                              setShowAttendance(true)
                              return
                            }

                            if (canToggle && typeof session.sessionId === 'number') {
                              void handleParticipation(
                                session.sessionId,
                                session.statusLabel !== '참여 예정',
                              )
                            }
                          }}
                          type="button"
                        >
                      <div className="timeline-card__meta">
                        <span>{session.roundLabel}</span>
                        <span>{session.scheduledAt}</span>
                      </div>
                      <strong>{session.title}</strong>
                      {isHighlighted || session.placeholder ? (
                        <span className={`status-chip is-${tone}`}>
                          {activeStudy.isLeader && isHighlighted
                            ? '참여 예정'
                            : session.statusLabel}
                        </span>
                      ) : null}
                    </button>
                  </div>
                )
                  })}
                </div>

                <span className="scroll-indicator timeline-scroll-indicator" />
              </div>
            </section>

            <aside className="board-pane">
              {activeStudy.notice ? (
                <section className="notice-panel">
                  <div className="surface-heading">
                    <span className="section-kicker">공지</span>
                    <span className="muted-caption">{activeStudy.notice.createdAt}</span>
                  </div>
                  <strong>{activeStudy.notice.title}</strong>
                  <p>{activeStudy.notice.content}</p>
                </section>
              ) : null}

              <section className="posts-panel">
                <div className="surface-heading">
                  <span className="section-kicker">게시글</span>
                  <button
                    className="soft-button"
                    onClick={() => setShowComposer(true)}
                    type="button"
                  >
                    글쓰기
                  </button>
                </div>

                <div className="posts-viewport">
                  <div className="post-list">
                    {renderedPosts.map((post) => (
                      <article
                        className={post.placeholder ? 'post-row is-placeholder' : 'post-row'}
                        key={String(post.postId)}
                      >
                        <div>
                          <strong>{post.title}</strong>
                          <span>{post.authorNickname}</span>
                        </div>
                        <time>{post.createdAt}</time>
                      </article>
                    ))}
                  </div>

                  <span className="scroll-indicator board-scroll-indicator" />
                </div>

                <div className="posts-divider" />

                <div className="pagination-row">
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
          onClick={() => setShowAttendance(false)}
          role="presentation"
        >
          <article
            className="modal-card attendance-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="modal-card__header">
              <div>
                <span className="section-kicker">출석 체크</span>
                <h2>{activeStudy.attendanceSessionLabel ?? '이번 회차'}</h2>
              </div>
              <button
                className="soft-button"
                onClick={() => setShowAttendance(false)}
                type="button"
              >
                닫기
              </button>
            </div>

            <div className="attendance-list">
              {activeStudy.attendanceRoster.map((member) => (
                <div className="attendance-row" key={member.userId}>
                  <div>
                    <strong>{member.nickname}</strong>
                    <span>{member.planned ? '참여 예정' : '미응답'}</span>
                  </div>

                  <div className="attendance-actions">
                    <button
                      className={
                        attendanceMap[member.userId] === 'PRESENT'
                          ? 'filter-chip is-active'
                          : 'filter-chip'
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
                          ? 'filter-chip is-active'
                          : 'filter-chip'
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
