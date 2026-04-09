import { useMemo, useState } from 'react'
import { useNavigate, useOutletContext } from 'react-router-dom'
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
import { type AppShellOutletContext } from '../layouts/appShellDashboard'

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

function buildDefaultDate(offsetDays: number) {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() + offsetDays)
  return formatDateKey(date)
}

const initialForm: StudyFormState = {
  type: 'MOGAKGONG',
  title: '',
  description: '',
  daysOfWeek: [],
  startTime: '05:00',
  endTime: '06:00',
  startDate: buildDefaultDate(7),
  endDate: buildDefaultDate(67),
  maxMembers: 4,
  locationType: 'OFFLINE',
  locationText: '',
  rulesText: '',
  suppliesText: '',
  tags: '',
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

  return `${index + 1}회차`
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

function isScheduleReady(form: StudyFormState) {
  return (
    Boolean(form.startDate) &&
    Boolean(form.endDate) &&
    Boolean(form.startTime) &&
    Boolean(form.endTime) &&
    form.daysOfWeek.length > 0
  )
}

function normalizeStudyForm(form: StudyFormState): StudyFormState {
  if (form.type !== 'FLASH' || !form.startDate) {
    return form
  }

  const flashDay = getDayValueFromDate(form.startDate)

  return {
    ...form,
    endDate: form.startDate,
    daysOfWeek: flashDay ? [flashDay] : form.daysOfWeek,
  }
}

export function StudyCreatePage() {
  const { sessionUserId, showToast } = useApp()
  const { refreshDashboard } = useOutletContext<AppShellOutletContext>()
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
  const [sessions, setSessions] = useState<DraftSession[]>(() => buildAutoSessions(initialForm))
  const [sessionsDirty, setSessionsDirty] = useState(false)

  const normalizedForm = useMemo(() => normalizeStudyForm(form), [form])
  const scheduleReady = useMemo(() => isScheduleReady(normalizedForm), [normalizedForm])
  const previewTags = useMemo(
    () =>
      form.tags
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean),
    [form.tags],
  )
  const daySummary = useMemo(
    () => formatDaysOfWeek(normalizedForm.daysOfWeek),
    [normalizedForm.daysOfWeek],
  )
  const flashDaySummary = useMemo(() => {
    const flashDay =
      normalizedForm.daysOfWeek[0] ?? getDayValueFromDate(normalizedForm.startDate)
    return flashDay ? getDayLabel(flashDay) : '미정'
  }, [normalizedForm.daysOfWeek, normalizedForm.startDate])
  const autoSessions = useMemo(
    () => (scheduleReady ? buildAutoSessions(normalizedForm) : []),
    [normalizedForm, scheduleReady],
  )
  const visibleSessions = useMemo(() => {
    if (!scheduleReady) {
      return normalizedForm.type !== 'TOPIC' || !sessionsDirty ? [] : sessions
    }

    if (normalizedForm.type !== 'TOPIC') {
      return autoSessions
    }

    return sessionsDirty ? sessions : autoSessions
  }, [autoSessions, normalizedForm.type, scheduleReady, sessions, sessionsDirty])
  const regularSessionCount = useMemo(
    () => visibleSessions.filter((session) => session.sessionType === 'REGULAR').length,
    [visibleSessions],
  )
  const breakSessionCount = visibleSessions.length - regularSessionCount
  const periodSummary = useMemo(() => {
    if (!normalizedForm.startDate || !normalizedForm.endDate) {
      return '미정'
    }

    return normalizedForm.startDate === normalizedForm.endDate
      ? normalizedForm.startDate
      : `${normalizedForm.startDate} ~ ${normalizedForm.endDate}`
  }, [normalizedForm.endDate, normalizedForm.startDate])

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
    setSessions(buildAutoSessions(normalizedForm))
    setSessionsDirty(false)
  }

  function updateSession(sessionId: string, patch: Partial<DraftSession>) {
    setSessionsDirty(true)
    setSessions((current) =>
      sortSessions(
        (sessionsDirty ? current : visibleSessions).map((session) =>
          session.id === sessionId ? { ...session, ...patch } : session,
        ),
      ),
    )
  }

  function handleSessionTypeChange(sessionId: string, nextType: SessionType) {
    setSessionsDirty(true)
    setSessions((current) =>
      (sessionsDirty ? current : visibleSessions).map((session, index) => {
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
              ? buildDefaultSessionTitle(index, normalizedForm.type)
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

    if (!form.title.trim()) {
      showToast('제목을 입력해주세요')
      return
    }

    if (!normalizedForm.daysOfWeek.length) {
      showToast('요일을 하나 이상 선택해주세요.')
      return
    }

    if (!scheduleReady) {
      showToast('운영 일정 정보를 먼저 입력해주세요.')
      return
    }

    if (!visibleSessions.length) {
      showToast('생성할 회차가 없습니다.')
      return
    }

    if (normalizedForm.endTime <= normalizedForm.startTime) {
      showToast('시작일을 종료일 이전으로 설정해주세요.')
      return
    }

    if (normalizedForm.maxMembers < 2) {
      showToast('최대 멤버수를 2명 이상으로 설정해주세요.')
      return
    }

    if (
      visibleSessions.some(
        (session) =>
          session.sessionType === 'REGULAR' && !session.title.trim(),
      )
    ) {
      showToast('吏꾪뻾 ?뚯감 ?쒕ぉ???낅젰?댁＜?몄슂.')
      return
    }

    if (normalizedForm.type === 'TOPIC' && regularSessionCount === 0) {
      showToast('진행 회차를 최소 1개 이상 남겨주세요.')
      return
    }

    try {
      const created = await api.createStudy(sessionUserId, {
        type: normalizedForm.type,
        title: form.title,
        description: form.description,
        daysOfWeek: normalizedForm.daysOfWeek,
        startTime: normalizedForm.startTime,
        endTime: normalizedForm.endTime,
        startDate: normalizedForm.startDate,
        endDate: normalizedForm.endDate,
        maxMembers: Number(normalizedForm.maxMembers),
        locationType: normalizedForm.locationType,
        locationText: normalizedForm.locationText,
        rulesText: normalizedForm.rulesText,
        suppliesText: normalizedForm.suppliesText,
        cautionText: '',
        tags: previewTags,
        sessions: sortSessions(visibleSessions).map((session) => ({
          title:
            session.sessionType === 'BREAK' ? BREAK_TITLE : session.title.trim(),
          scheduledAt: session.scheduledAt,
          sessionType: session.sessionType,
          placeText: normalizedForm.locationText,
        })),
      })
      await refreshDashboard()
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
                      setForm((current) => {
                        const nextType = option.value as StudyType
                        if (nextType !== 'TOPIC') {
                          setSessionsDirty(false)
                        }

                        return {
                          ...current,
                          type: nextType,
                        }
                      })
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
                placeholder="예: 자료구조 같이 끝내는 주제 스터디"
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
                placeholder="예: 기초 개념부터 문제 풀이까지 차근차근 가는 주제형 스터디입니다."
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
                  <strong>총 {visibleSessions.length}회차 진행</strong>
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
                  normalizedForm.locationType === 'ONLINE'
                    ? '예: Google Meet 링크'
                    : '예: 나눔관 301'
                }
                value={form.locationText}
              />
            </label>
          </div>
        </article>

        {form.type === 'TOPIC' || form.type === 'MOGAKGONG' ? (
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
              {visibleSessions.map((session, index) => {
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
                placeholder="예: 노트북, 교재, 필기구를 챙겨주세요."
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
                placeholder="예: 결석 예정은 하루 전까지 게시판에 올려주세요."
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
            <h2>개설하기</h2>
            {/* <p>길게 내려보지 않아도 핵심 정보가 한눈에 들어오도록 압축했어요.</p> */}
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
                {normalizedForm.locationType === 'ONLINE' ? '온라인' : '오프라인'} / {normalizedForm.locationText}
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
