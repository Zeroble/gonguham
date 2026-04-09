import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, type CreateStudySessionInput } from '../app/api'
import {
  DAY_PICKER_OPTIONS,
  formatDaysOfWeek,
  getDayLabel,
  getStudyTypeLabel,
  LOCATION_OPTIONS,
  STUDY_TYPE_PICKER_OPTIONS,
} from '../app/display'
import { useApp } from '../app/useApp'

type StudyType = 'TOPIC' | 'MOGAKGONG' | 'FLASH'
type SessionType = 'REGULAR' | 'BREAK'

type StudyFormState = {
  type: StudyType
  title: string
  description: string
  daysOfWeek: string[]
  startTime: string
  endTime: string
  startDate: string
  endDate: string
  maxMembers: number
  locationType: 'ONLINE' | 'OFFLINE'
  locationText: string
  rulesText: string
  suppliesText: string
  tags: string
}

type DraftSession = CreateStudySessionInput & {
  id: string
}

const BREAK_TITLE = '쉬어가는 회차'
const DAY_ORDER: string[] = DAY_PICKER_OPTIONS.map((option) => option.value)
const JS_DAY_TO_VALUE = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const TOPIC_TITLE_TEMPLATES = ['OT', '핵심 개념 정리', '문제 풀이', '응용 실습', '리뷰와 회고']

const initialForm: StudyFormState = {
  type: 'TOPIC',
  title: '자료구조 같이 끝내는 주제 스터디',
  description: '기초 개념부터 문제 풀이까지 차근차근 가는 주제형 스터디입니다.',
  daysOfWeek: ['TUESDAY'],
  startTime: '18:30',
  endTime: '20:00',
  startDate: '2026-04-15',
  endDate: '2026-06-24',
  maxMembers: 8,
  locationType: 'OFFLINE',
  locationText: '인천공항 3층 모임방 B',
  rulesText: '결석 예정은 하루 전까지 게시판에 올려주세요.',
  suppliesText: '노트북, 교재, 필기구를 챙겨주세요.',
  tags: 'CS, 자료구조, 오프라인',
}

function sortDaysOfWeek(daysOfWeek: string[]) {
  return [...daysOfWeek].sort(
    (left, right) => DAY_ORDER.indexOf(left) - DAY_ORDER.indexOf(right),
  )
}

function formatDateKey(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getDayValueFromDate(dateKey: string) {
  if (!dateKey) {
    return ''
  }

  const date = new Date(`${dateKey}T00:00:00`)
  return JS_DAY_TO_VALUE[date.getDay()] ?? ''
}

function listScheduledDates(startDate: string, endDate: string, daysOfWeek: string[]) {
  if (!startDate || !endDate || !daysOfWeek.length) {
    return []
  }

  const current = new Date(`${startDate}T00:00:00`)
  const last = new Date(`${endDate}T00:00:00`)
  const allowedDays = new Set(daysOfWeek)
  const dates: string[] = []

  while (current <= last) {
    const weekday = JS_DAY_TO_VALUE[current.getDay()]
    if (allowedDays.has(weekday)) {
      dates.push(formatDateKey(current))
    }
    current.setDate(current.getDate() + 1)
  }

  return dates
}

function buildDefaultSessionTitle(index: number, type: StudyType) {
  if (type !== 'TOPIC') {
    return `${index + 1}회차`
  }

  return TOPIC_TITLE_TEMPLATES[index] ?? `${index + 1}회차 주제`
}

function sortSessions(sessions: DraftSession[]) {
  return [...sessions].sort((left, right) => left.scheduledAt.localeCompare(right.scheduledAt))
}

function buildAutoSessions(form: StudyFormState): DraftSession[] {
  const dates = listScheduledDates(form.startDate, form.endDate, form.daysOfWeek)

  return dates.map((date, index) => ({
    id: `${date}-${index}`,
    title: buildDefaultSessionTitle(index, form.type),
    scheduledAt: `${date}T${form.startTime}`,
    sessionType: 'REGULAR',
    placeText: form.locationText,
  }))
}

function formatScheduledAtLabel(scheduledAt: string) {
  if (!scheduledAt) {
    return '일정을 입력하세요'
  }

  const date = scheduledAt.slice(5, 10).replace('-', '.')
  const time = scheduledAt.slice(11, 16)
  return `${date} ${time}`
}

function isScheduleReady(form: StudyFormState) {
  return (
    Boolean(form.startDate) &&
    Boolean(form.endDate) &&
    Boolean(form.startTime) &&
    Boolean(form.endTime) &&
    form.daysOfWeek.length > 0
  )
}

export function StudyCreatePage() {
  const { sessionUserId, showToast } = useApp()
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [sessions, setSessions] = useState<DraftSession[]>(() => buildAutoSessions(initialForm))
  const [sessionsDirty, setSessionsDirty] = useState(false)

  const scheduleReady = useMemo(() => isScheduleReady(form), [form])
  const previewTags = useMemo(
    () =>
      form.tags
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean),
    [form.tags],
  )
  const daySummary = useMemo(() => formatDaysOfWeek(form.daysOfWeek), [form.daysOfWeek])
  const flashDaySummary = useMemo(() => {
    const flashDay = form.daysOfWeek[0] ?? getDayValueFromDate(form.startDate)
    return flashDay ? getDayLabel(flashDay) : '미정'
  }, [form.daysOfWeek, form.startDate])
  const regularSessionCount = useMemo(
    () => sessions.filter((session) => session.sessionType === 'REGULAR').length,
    [sessions],
  )
  const breakSessionCount = sessions.length - regularSessionCount
  const periodSummary = useMemo(() => {
    if (!form.startDate || !form.endDate) {
      return '미정'
    }

    return form.startDate === form.endDate
      ? form.startDate
      : `${form.startDate} ~ ${form.endDate}`
  }, [form.endDate, form.startDate])

  useEffect(() => {
    if (form.type !== 'FLASH' || !form.startDate) {
      return
    }

    const flashDay = getDayValueFromDate(form.startDate)

    setForm((current) => {
      const nextDays = flashDay ? [flashDay] : current.daysOfWeek
      const shouldSyncDays =
        current.daysOfWeek.length !== nextDays.length ||
        current.daysOfWeek.some((day, index) => day !== nextDays[index])

      if (current.endDate === current.startDate && !shouldSyncDays) {
        return current
      }

      return {
        ...current,
        endDate: current.startDate,
        daysOfWeek: nextDays,
      }
    })
  }, [form.startDate, form.type])

  useEffect(() => {
    if (!scheduleReady) {
      if (form.type !== 'TOPIC' || !sessionsDirty) {
        setSessions([])
      }
      return
    }

    if (form.type !== 'TOPIC') {
      setSessions(buildAutoSessions(form))
      setSessionsDirty(false)
      return
    }

    if (!sessionsDirty) {
      setSessions(buildAutoSessions(form))
    }
  }, [
    form.daysOfWeek,
    form.endDate,
    form.locationText,
    form.startDate,
    form.startTime,
    form.type,
    scheduleReady,
    sessionsDirty,
  ])

  function toggleDay(dayOfWeek: string) {
    setForm((current) => {
      const nextDays = current.daysOfWeek.includes(dayOfWeek)
        ? current.daysOfWeek.filter((day) => day !== dayOfWeek)
        : sortDaysOfWeek([...current.daysOfWeek, dayOfWeek])

      return {
        ...current,
        daysOfWeek: nextDays,
      }
    })
  }

  function regenerateSessions() {
    setSessions(buildAutoSessions(form))
    setSessionsDirty(false)
  }

  function updateSession(sessionId: string, patch: Partial<DraftSession>) {
    setSessionsDirty(true)
    setSessions((current) =>
      sortSessions(
        current.map((session) =>
          session.id === sessionId ? { ...session, ...patch } : session,
        ),
      ),
    )
  }

  function handleSessionTypeChange(sessionId: string, nextType: SessionType) {
    setSessionsDirty(true)
    setSessions((current) =>
      current.map((session, index) => {
        if (session.id !== sessionId) {
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
              ? buildDefaultSessionTitle(index, form.type)
              : session.title,
          sessionType: 'REGULAR',
        }
      }),
    )
  }

  async function handleSubmit() {
    if (!sessionUserId) {
      return
    }

    if (!form.daysOfWeek.length) {
      showToast('요일을 하나 이상 선택해주세요.')
      return
    }

    if (!scheduleReady) {
      showToast('운영 일정 정보를 먼저 입력해주세요.')
      return
    }

    if (!sessions.length) {
      showToast('생성할 회차가 없습니다.')
      return
    }

    if (form.type === 'TOPIC' && regularSessionCount === 0) {
      showToast('진행 회차를 최소 1개 이상 남겨주세요.')
      return
    }

    try {
      const created = await api.createStudy(sessionUserId, {
        type: form.type,
        title: form.title,
        description: form.description,
        daysOfWeek: form.daysOfWeek,
        startTime: form.startTime,
        endTime: form.endTime,
        startDate: form.startDate,
        endDate: form.endDate,
        maxMembers: Number(form.maxMembers),
        locationType: form.locationType,
        locationText: form.locationText,
        rulesText: form.rulesText,
        suppliesText: form.suppliesText,
        cautionText: '',
        tags: previewTags,
        sessions: sortSessions(sessions).map((session) => ({
          title:
            session.sessionType === 'BREAK' ? BREAK_TITLE : session.title.trim(),
          scheduledAt: session.scheduledAt,
          sessionType: session.sessionType,
          placeText: form.locationText,
        })),
      })
      showToast(`"${created.title}" 스터디를 개설했어요.`)
      navigate('/app/home', { replace: true })
    } catch (error) {
      showToast(error instanceof Error ? error.message : '스터디 개설에 실패했어요.')
    }
  }

  return (
    <section className="create-layout">
      <div className="stack-section">
        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>기본 정보</h2>
              <p>스터디의 첫인상을 만들 핵심 정보를 정리해주세요.</p>
            </div>
          </div>

          <div className="field-stack">
            <div>
              <span className="field-label">스터디 유형</span>
              <div className="filter-chip-row">
                {STUDY_TYPE_PICKER_OPTIONS.map((option) => (
                  <button
                    className={
                      form.type === option.value
                        ? 'filter-chip is-active'
                        : 'filter-chip'
                    }
                    key={option.value}
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        type: option.value as StudyType,
                      }))
                    }
                    type="button"
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <label className="field-block">
              <span className="field-label">제목</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({ ...current, title: event.target.value }))
                }
                value={form.title}
              />
            </label>

            <label className="field-block">
              <span className="field-label">설명</span>
              <textarea
                className="field-control textarea-field"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
                value={form.description}
              />
            </label>
          </div>
        </article>

        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>운영 일정</h2>
              <p>기간과 요일을 정하면 회차를 자동으로 계산해드릴게요.</p>
            </div>
          </div>

          <div className="field-stack">
            {form.type !== 'FLASH' ? (
              <div>
                <span className="field-label">요일</span>
                <div className="filter-chip-row">
                  {DAY_PICKER_OPTIONS.map((option) => (
                    <button
                      className={
                        form.daysOfWeek.includes(option.value)
                          ? 'filter-chip is-active'
                          : 'filter-chip'
                      }
                      key={option.value}
                      onClick={() => toggleDay(option.value)}
                      type="button"
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}

            <div className="split-field-grid">
              <label className="field-block">
                <span className="field-label">시작 시간</span>
                <input
                  className="field-control"
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      startTime: event.target.value,
                    }))
                  }
                  type="time"
                  value={form.startTime}
                />
              </label>

              <label className="field-block">
                <span className="field-label">종료 시간</span>
                <input
                  className="field-control"
                  onChange={(event) =>
                    setForm((current) => ({ ...current, endTime: event.target.value }))
                  }
                  type="time"
                  value={form.endTime}
                />
              </label>

              <label className="field-block">
                <span className="field-label">{form.type === 'FLASH' ? '진행일' : '시작일'}</span>
                <input
                  className="field-control"
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      startDate: event.target.value,
                    }))
                  }
                  type="date"
                  value={form.startDate}
                />
              </label>

              {form.type !== 'FLASH' ? (
                <label className="field-block">
                  <span className="field-label">종료일</span>
                  <input
                    className="field-control"
                    onChange={(event) =>
                      setForm((current) => ({ ...current, endDate: event.target.value }))
                    }
                    type="date"
                    value={form.endDate}
                  />
                </label>
              ) : (
                <div className="field-block">
                  <span className="field-label">진행 요일</span>
                  <div className="schedule-summary-card">
                    <strong>{flashDaySummary}</strong>
                  </div>
                </div>
              )}

              <label className="field-block">
                <span className="field-label">모집 인원</span>
                <input
                  className="field-control"
                  min={2}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      maxMembers: Number(event.target.value),
                    }))
                  }
                  type="number"
                  value={form.maxMembers}
                />
              </label>

              <div className="field-block">
                <span className="field-label">자동 계산</span>
                <div className="schedule-summary-card">
                  <strong>총 {sessions.length}회차 진행</strong>
                </div>
              </div>
            </div>
          </div>
        </article>

        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>장소와 운영 방식</h2>
              <p>온라인/오프라인에 따라 멤버가 바로 이해할 수 있게 적어주세요.</p>
            </div>
          </div>

          <div className="field-stack">
            <div>
              <span className="field-label">진행 방식</span>
              <div className="filter-chip-row">
                {LOCATION_OPTIONS.map((option) => (
                  <button
                    className={
                      form.locationType === option.value
                        ? 'filter-chip is-active'
                        : 'filter-chip'
                    }
                    key={option.value}
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        locationType: option.value as 'ONLINE' | 'OFFLINE',
                      }))
                    }
                    type="button"
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <label className="field-block">
              <span className="field-label">장소</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    locationText: event.target.value,
                  }))
                }
                placeholder={
                  form.locationType === 'ONLINE'
                    ? '예: Google Meet 링크'
                    : '예: 인천공항 3층 모임방 B'
                }
                value={form.locationText}
              />
            </label>
          </div>
        </article>

        {form.type === 'TOPIC' ? (
          <article className="page-surface form-section-card">
            <div className="section-card__header">
              <div>
                <h2>회차 설계</h2>
                <p>자동 생성된 회차를 다듬고, 필요하면 휴차로 바꿔주세요.</p>
              </div>

              <div className="session-builder-actions">
                <span className="count-badge">진행 {regularSessionCount}회</span>
                {breakSessionCount ? (
                  <span className="count-badge is-warm">휴차 {breakSessionCount}회</span>
                ) : null}
                <button className="soft-button" onClick={regenerateSessions} type="button">
                  회차 다시 생성
                </button>
              </div>
            </div>

            {sessionsDirty ? (
              <p className="session-builder-note">
                회차를 직접 수정한 상태예요. 일정이 바뀌어도 자동으로 덮어쓰지 않습니다.
              </p>
            ) : null}

            <div className="session-plan-list">
              {sessions.map((session, index) => {
                const isBreak = session.sessionType === 'BREAK'

                return (
                  <article
                    className={
                      isBreak
                        ? 'session-plan-card is-break'
                        : 'session-plan-card'
                    }
                    key={session.id}
                  >
                    <div className="session-plan-card__header">
                      <div>
                        <span className="field-label">{index + 1}회차</span>
                        <strong>{formatScheduledAtLabel(session.scheduledAt)}</strong>
                      </div>

                      <div className="filter-chip-row">
                        <button
                          className={
                            !isBreak ? 'filter-chip is-active' : 'filter-chip'
                          }
                          onClick={() =>
                            handleSessionTypeChange(session.id, 'REGULAR')
                          }
                          type="button"
                        >
                          진행
                        </button>
                        <button
                          className={
                            isBreak ? 'filter-chip is-active warm' : 'filter-chip warm'
                          }
                          onClick={() =>
                            handleSessionTypeChange(session.id, 'BREAK')
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
                          onChange={(event) =>
                            updateSession(session.id, {
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
                          disabled={isBreak}
                          onChange={(event) =>
                            updateSession(session.id, {
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
          </article>
        ) : null}

        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>규칙과 안내</h2>
              <p>참여 전에 알아야 할 운영 규칙을 미리 적어주세요.</p>
            </div>
          </div>

          <div className="field-stack">
            <label className="field-block">
              <span className="field-label">준비물</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    suppliesText: event.target.value,
                  }))
                }
                value={form.suppliesText}
              />
            </label>

            <label className="field-block">
              <span className="field-label">참여 규칙</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    rulesText: event.target.value,
                  }))
                }
                value={form.rulesText}
              />
            </label>

            <label className="field-block">
              <span className="field-label">태그</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({ ...current, tags: event.target.value }))
                }
                placeholder="쉼표로 구분해주세요"
                value={form.tags}
              />
            </label>
          </div>
        </article>
      </div>

      <aside className="page-surface preview-panel create-preview-panel">
        <div className="section-card__header">
          <div>
            <h2>개설 요약</h2>
            <p>길게 내려보지 않아도 핵심 정보가 한눈에 들어오도록 압축했어요.</p>
          </div>
        </div>

        <article className="preview-study-card">
          <div className="study-result-card__top">
            <span className="study-type-badge">{getStudyTypeLabel(form.type)}</span>
          </div>

          <h3>{form.title || '스터디 제목을 입력해주세요'}</h3>
          <p>{form.description || '스터디 설명이 여기에 표시됩니다.'}</p>

          <div className="create-preview-summary">
            <div className="create-preview-summary__item">
              <span>운영 기간</span>
              <strong>{periodSummary}</strong>
            </div>
            <div className="create-preview-summary__item">
              <span>요일</span>
              <strong>{daySummary || '미정'}</strong>
            </div>
            <div className="create-preview-summary__item">
              <span>장소</span>
              <strong>
                {form.locationType === 'ONLINE' ? '온라인' : '오프라인'} / {form.locationText}
              </strong>
            </div>
            <div className="create-preview-summary__item">
              <span>모집 인원</span>
              <strong>{form.maxMembers}명</strong>
            </div>
          </div>

          <div className="tag-row">
            {previewTags.map((tag) => (
              <span className="tag-chip" key={tag}>
                {tag}
              </span>
            ))}
          </div>

          <button className="primary-button preview-submit" onClick={handleSubmit} type="button">
            개설하기
          </button>
        </article>
      </aside>
    </section>
  )
}
