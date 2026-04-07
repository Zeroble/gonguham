import { useEffect, useMemo, useState } from 'react'
import { api, type StudyCard, type StudyDetail } from '../app/api'
import {
  DAY_FILTER_OPTIONS,
  PLACE_FILTER_OPTIONS,
  STUDY_TYPE_OPTIONS,
  TIME_FILTER_OPTIONS,
  matchesDayFilter,
  matchesPlaceFilter,
  matchesTimeFilter,
} from '../app/display'
import { useApp } from '../app/useApp'

export function StudySearchPage() {
  const { sessionUserId } = useApp()
  const [studies, setStudies] = useState<StudyCard[]>([])
  const [keyword, setKeyword] = useState('')
  const [selectedType, setSelectedType] = useState('')
  const [selectedDay, setSelectedDay] = useState('')
  const [selectedTime, setSelectedTime] = useState('')
  const [selectedPlace, setSelectedPlace] = useState('')
  const [selectedStudy, setSelectedStudy] = useState<StudyDetail | null>(null)
  const [message, setMessage] = useState('')

  const trimmedKeyword = useMemo(() => keyword.trim(), [keyword])

  useEffect(() => {
    if (!sessionUserId) {
      return
    }

    let cancelled = false

    async function fetchStudies() {
      const next = await api.getStudies(sessionUserId, trimmedKeyword, selectedType)

      if (!cancelled) {
        setStudies(next)
      }
    }

    void fetchStudies()

    return () => {
      cancelled = true
    }
  }, [selectedType, sessionUserId, trimmedKeyword])

  const visibleStudies = useMemo(
    () =>
      studies.filter(
        (study) =>
          matchesDayFilter(study.dayLabel, selectedDay) &&
          matchesTimeFilter(study.timeLabel, selectedTime) &&
          matchesPlaceFilter(study.locationLabel, selectedPlace),
      ),
    [selectedDay, selectedPlace, selectedTime, studies],
  )

  async function openDetail(studyId: number) {
    if (!sessionUserId) {
      return
    }

    try {
      const detail = await api.getStudyDetail(sessionUserId, studyId)
      setSelectedStudy(detail)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '스터디 상세를 불러오지 못했어요.')
    }
  }

  async function handleJoin(studyId: number) {
    if (!sessionUserId) {
      return
    }

    try {
      const detail = await api.joinStudy(sessionUserId, studyId)
      setSelectedStudy(detail)
      setStudies(await api.getStudies(sessionUserId, trimmedKeyword, selectedType))
      setMessage('스터디에 바로 참여했어요.')
    } catch (error) {
      setMessage(error instanceof Error ? error.message : '스터디 참여에 실패했어요.')
    }
  }

  function resetFilters() {
    setKeyword('')
    setSelectedType('')
    setSelectedDay('')
    setSelectedTime('')
    setSelectedPlace('')
  }

  return (
    <section className="stack-section">
      {message ? <div className="message-banner">{message}</div> : null}

      <article className="page-surface search-toolbar-card">
        <div className="filter-panel-grid">
          <div className="filter-group">
            <span className="filter-label">형태</span>
            <div className="filter-chip-row">
              {STUDY_TYPE_OPTIONS.filter((option) => option.value).map((option) => (
                <button
                  className={
                    selectedType === option.value ? 'filter-chip is-active' : 'filter-chip'
                  }
                  key={option.value}
                  onClick={() => setSelectedType(option.value)}
                  type="button"
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="filter-group">
            <span className="filter-label">요일</span>
            <div className="filter-chip-row">
              {DAY_FILTER_OPTIONS.map((option) => (
                <button
                  className={
                    selectedDay === option.value ? 'filter-chip is-active' : 'filter-chip'
                  }
                  key={option.value}
                  onClick={() => setSelectedDay(option.value)}
                  type="button"
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="filter-group">
            <span className="filter-label">시간</span>
            <div className="filter-chip-row">
              {TIME_FILTER_OPTIONS.map((option) => (
                <button
                  className={
                    selectedTime === option.value ? 'filter-chip is-active' : 'filter-chip'
                  }
                  key={option.value}
                  onClick={() => setSelectedTime(option.value)}
                  type="button"
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="filter-group">
            <span className="filter-label">위치</span>
            <div className="filter-chip-row">
              {PLACE_FILTER_OPTIONS.map((option) => (
                <button
                  className={
                    selectedPlace === option.value ? 'filter-chip is-active' : 'filter-chip'
                  }
                  key={option.value}
                  onClick={() => setSelectedPlace(option.value)}
                  type="button"
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <button className="soft-button reset-button" onClick={resetFilters} type="button">
            필터 초기화
          </button>

          <input
            className="field-control search-field"
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="검색: 자료구조 / 모각공 / 토요일"
            value={keyword}
          />
        </div>
      </article>

      <div className="page-title-row">
        <h2>스터디 찾기 결과 {visibleStudies.length}개</h2>
      </div>

      <section className="study-result-grid">
        {visibleStudies.map((study) => (
          <article className="study-result-card" key={study.studyId}>
            <div className="study-result-card__top">
              <span className="study-type-badge">{study.type}</span>
              <span className="count-badge">{study.slotsLabel}</span>
            </div>

            <h3>{study.title}</h3>
            <p>{study.description}</p>

            <div className="study-result-card__meta">
              <span>{study.dayLabel}</span>
              <span>{study.timeLabel}</span>
            </div>
            <span className="muted-caption">{study.locationLabel}</span>

            <div className="tag-row">
              {study.tags.map((tag) => (
                <span className="tag-chip" key={tag}>
                  {tag}
                </span>
              ))}
            </div>

            <div className="card-action-row">
              <button
                className={study.joined ? 'soft-button is-disabled' : 'primary-button'}
                disabled={study.joined}
                onClick={() => handleJoin(study.studyId)}
                type="button"
              >
                {study.joined ? '가입됨' : '가입하기'}
              </button>
              <button
                className="soft-button"
                onClick={() => openDetail(study.studyId)}
                type="button"
              >
                상세 보기
              </button>
            </div>
          </article>
        ))}
      </section>

      {selectedStudy ? (
        <div
          className="modal-backdrop"
          onClick={() => setSelectedStudy(null)}
          role="presentation"
        >
          <article
            className="modal-card study-detail-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="modal-card__header">
              <div>
                <span className="study-type-badge">{selectedStudy.type}</span>
                <h2>{selectedStudy.title}</h2>
                <p className="modal-description">{selectedStudy.description}</p>
              </div>
              <button
                className="soft-button"
                onClick={() => setSelectedStudy(null)}
                type="button"
              >
                닫기
              </button>
            </div>

            <div className="info-grid">
              <div className="info-tile">
                <span>요일</span>
                <strong>{selectedStudy.dayLabel}</strong>
              </div>
              <div className="info-tile">
                <span>시간</span>
                <strong>{selectedStudy.timeLabel}</strong>
              </div>
              <div className="info-tile">
                <span>위치</span>
                <strong>{selectedStudy.locationLabel}</strong>
              </div>
              <div className="info-tile">
                <span>리더</span>
                <strong>{selectedStudy.leaderNickname}</strong>
              </div>
            </div>

            <div className="detail-modal-grid">
              <section className="detail-modal-column">
                {selectedStudy.notice ? (
                  <article className="notice-panel">
                    <span className="section-kicker">공지</span>
                    <strong>{selectedStudy.notice.title}</strong>
                    <p>{selectedStudy.notice.content}</p>
                  </article>
                ) : null}

                <article className="detail-panel">
                  <span className="section-kicker">회차</span>
                  <div className="session-preview-list">
                    {selectedStudy.sessions.map((session) => (
                      <div className="session-preview-item" key={session.sessionId}>
                        <span>{session.sessionNo}회차</span>
                        <strong>{session.title}</strong>
                        <span>{session.scheduledAt}</span>
                      </div>
                    ))}
                  </div>
                </article>

                <article className="detail-panel">
                  <span className="section-kicker">게시글</span>
                  <div className="post-list">
                    {selectedStudy.posts.map((post) => (
                      <article className="post-row" key={post.postId}>
                        <div>
                          <strong>{post.title}</strong>
                          <span>{post.authorNickname}</span>
                        </div>
                        <time>{post.createdAt}</time>
                      </article>
                    ))}
                  </div>
                </article>
              </section>

              <aside className="detail-modal-column">
                <article className="detail-panel">
                  <span className="section-kicker">준비물</span>
                  <p>{selectedStudy.suppliesText}</p>
                </article>
                <article className="detail-panel">
                  <span className="section-kicker">참여 규칙</span>
                  <p>{selectedStudy.rulesText}</p>
                </article>
                <article className="detail-panel">
                  <span className="section-kicker">유의사항</span>
                  <p>{selectedStudy.cautionText}</p>
                </article>
                <article className="detail-panel">
                  <span className="section-kicker">태그</span>
                  <div className="tag-row">
                    {selectedStudy.tags.map((tag) => (
                      <span className="tag-chip" key={tag}>
                        {tag}
                      </span>
                    ))}
                  </div>
                </article>
              </aside>
            </div>

            <button
              className={selectedStudy.joined ? 'soft-button is-disabled' : 'primary-button'}
              disabled={selectedStudy.joined}
              onClick={() => handleJoin(selectedStudy.studyId)}
              type="button"
            >
              {selectedStudy.joined ? '이미 참여중' : '즉시 참여'}
            </button>
          </article>
        </div>
      ) : null}
    </section>
  )
}
