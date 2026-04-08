import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../app/api'
import {
  DAY_PICKER_OPTIONS,
  LOCATION_OPTIONS,
  STUDY_TYPE_PICKER_OPTIONS,
  getStudyTypeLabel,
} from '../app/display'
import { useApp } from '../app/useApp'

const initialSessions = [
  '배열과 연결 리스트',
  '스택과 큐',
  '트리와 우선순위 큐',
  '그래프 탐색',
  '최단 경로와 MST',
  '해시와 집합',
]

export function StudyCreatePage() {
  const { sessionUserId, showToast } = useApp()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    type: 'TOPIC',
    title: '자료구조 같이 끝내는 주제 스터디',
    description:
      '연결 리스트부터 그래프까지 차근차근 공부하는 개념 중심 스터디입니다.',
    dayOfWeek: 'TUESDAY',
    startTime: '18:30',
    endTime: '20:00',
    startDate: '2026-04-15',
    endDate: '2026-06-24',
    maxMembers: 8,
    locationType: 'OFFLINE',
    locationText: '새천년관 3층 세미나실 B',
    rulesText: '노트북, 교재, 필기구를 챙겨 와 주세요.',
    suppliesText: '복습할 문제가 있으면 전날까지 공유해 주세요.',
    cautionText: '3회 이상 결석 시 운영진과 대화합니다.',
    tags: 'CS, 자료구조, 세미나, 오프라인',
  })
  const [sessions, setSessions] = useState(initialSessions)

  const previewTags = useMemo(
    () =>
      form.tags
        .split(',')
        .map((tag) => tag.trim())
        .filter(Boolean),
    [form.tags],
  )

  async function handleSubmit() {
    if (!sessionUserId) {
      return
    }

    try {
      const created = await api.createStudy(sessionUserId, {
        ...form,
        maxMembers: Number(form.maxMembers),
        tags: previewTags,
        sessions,
      })
      showToast(`"${created.title}" 스터디를 개설했어요.`)
      navigate('/app/home', { replace: true })
    } catch (error) {
      showToast(error instanceof Error ? error.message : '스터디 개설에 실패했어요.')
    }
  }

  function updateSession(index: number, value: string) {
    setSessions((current) =>
      current.map((session, sessionIndex) =>
        sessionIndex === index ? value : session,
      ),
    )
  }

  function addSession() {
    setSessions((current) => [...current, `새 회차 ${current.length + 1}`])
  }

  function removeSession() {
    setSessions((current) => (current.length > 1 ? current.slice(0, -1) : current))
  }

  return (
    <section className="create-layout">
      <div className="stack-section">
        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>기본 정보</h2>
              <p>스터디의 첫인상을 피그마 화면처럼 정리해 둘게요.</p>
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
                      setForm((current) => ({ ...current, type: option.value }))
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
              <h2>일정 / 운영 정보</h2>
              <p>요일, 시간, 모집 인원까지 이 카드 안에서 정리합니다.</p>
            </div>
          </div>

          <div className="field-stack">
            <div>
              <span className="field-label">요일</span>
              <div className="filter-chip-row">
                {DAY_PICKER_OPTIONS.map((option) => (
                  <button
                    className={
                      form.dayOfWeek === option.value
                        ? 'filter-chip is-active'
                        : 'filter-chip'
                    }
                    key={option.value}
                    onClick={() =>
                      setForm((current) => ({
                        ...current,
                        dayOfWeek: option.value,
                      }))
                    }
                    type="button"
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="split-field-grid">
              <label className="field-block">
                <span className="field-label">시작</span>
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
                <span className="field-label">종료</span>
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
                <span className="field-label">시작일</span>
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

              <label className="field-block">
                <span className="field-label">정원</span>
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
            </div>
          </div>
        </article>

        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>위치</h2>
              <p>온라인 / 오프라인 여부에 따라 필드를 다르게 보이게 할 수 있어요.</p>
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
                        locationType: option.value,
                      }))
                    }
                    type="button"
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="split-field-grid">
              <label className="field-block field-block--wide">
                <span className="field-label">장소</span>
                <input
                  className="field-control"
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      locationText: event.target.value,
                    }))
                  }
                  value={form.locationText}
                />
              </label>
            </div>
          </div>
        </article>

        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>회차 설계</h2>
              <p>개설 전 회차 흐름을 간단히 설계해 둘 수 있어요.</p>
            </div>

            <div className="session-builder-actions">
              <span className="count-badge">총 회차 {sessions.length}</span>
              <button className="soft-button" onClick={removeSession} type="button">
                -
              </button>
              <button className="soft-button" onClick={addSession} type="button">
                +
              </button>
              <button
                className="primary-button"
                onClick={() =>
                  showToast('AI 커리큘럼 추천은 1차 데모에서 버튼만 제공해요.')
                }
                type="button"
              >
                AI 커리큘럼 추천
              </button>
            </div>
          </div>

          <div className="session-preview-list">
            {sessions.map((session, index) => (
              <label className="field-block" key={`${session}-${index}`}>
                <span className="field-label">{index + 1}회차</span>
                <input
                  className="field-control"
                  onChange={(event) => updateSession(index, event.target.value)}
                  value={session}
                />
              </label>
            ))}
          </div>
        </article>

        <article className="page-surface form-section-card">
          <div className="section-card__header">
            <div>
              <h2>규칙 / 안내</h2>
              <p>준비물과 운영 규칙은 개설 후에도 수정할 수 있습니다.</p>
            </div>
          </div>

          <div className="split-field-grid">
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
              <span className="field-label">유의사항</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    cautionText: event.target.value,
                  }))
                }
                value={form.cautionText}
              />
            </label>

            <label className="field-block">
              <span className="field-label">태그</span>
              <input
                className="field-control"
                onChange={(event) =>
                  setForm((current) => ({ ...current, tags: event.target.value }))
                }
                value={form.tags}
              />
            </label>
          </div>
        </article>
      </div>

      <aside className="page-surface preview-panel">
        <div className="section-card__header">
          <div>
            <h2>개설 미리보기</h2>
            <p>유저 스크롤을 따라가며 지금 입력한 정보가 바로 보이게 구성했습니다.</p>
          </div>
        </div>

        <article className="preview-study-card">
          <div className="study-result-card__top">
            <span className="study-type-badge">{getStudyTypeLabel(form.type)}</span>
            <span className="count-badge">{sessions.length}회차</span>
          </div>

          <h3>{form.title}</h3>
          <p>{form.description}</p>

          <div className="study-result-card__meta">
            <span>
              {DAY_PICKER_OPTIONS.find((option) => option.value === form.dayOfWeek)?.label}
              요일
            </span>
            <span>
              {form.startTime} - {form.endTime}
            </span>
          </div>
          <span className="muted-caption">
            {form.locationType === 'ONLINE' ? '온라인' : '오프라인'} · {form.locationText}
          </span>

          <div className="tag-row">
            {previewTags.map((tag) => (
              <span className="tag-chip" key={tag}>
                {tag}
              </span>
            ))}
          </div>

          <div className="session-preview-list">
            {sessions.map((session, index) => (
              <div className="session-preview-item" key={`${session}-${index}`}>
                <span>{index + 1}회차</span>
                <strong>{session}</strong>
              </div>
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
