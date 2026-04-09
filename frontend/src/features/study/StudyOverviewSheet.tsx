import { type StudyDetail } from '../../app/api'
import { ProfileNameButton } from '../profile/ProfileNameButton'

type StudyOverviewSheetProps = {
  study: Pick<
    StudyDetail,
    | 'type'
    | 'title'
    | 'description'
    | 'leaderUserId'
    | 'leaderNickname'
    | 'dayLabel'
    | 'timeLabel'
    | 'locationLabel'
    | 'rulesText'
    | 'suppliesText'
    | 'cautionText'
    | 'tags'
    | 'slotsLabel'
    | 'sessions'
  >
}

export function StudyOverviewSheet({ study }: StudyOverviewSheetProps) {
  return (
    <div className="study-overview-sheet">
      <section className="study-overview-sheet__hero">
        <div className="study-overview-sheet__copy">
          <div className="study-overview-sheet__badges">
            <span className="study-type-badge">{study.type}</span>
            <span className="count-badge">{study.slotsLabel}</span>
          </div>

          <h2>{study.title}</h2>
          <p>{study.description}</p>

          <div className="study-overview-sheet__schedule">
            <span>{study.dayLabel}</span>
            <span>{study.timeLabel}</span>
            <span>{study.locationLabel}</span>
          </div>
        </div>

        <div className="study-overview-sheet__summary">
          <article className="study-overview-sheet__summary-card">
            <span>스터디장</span>
            <ProfileNameButton
              className="profile-name-button is-inline is-strong"
              nickname={study.leaderNickname}
              userId={study.leaderUserId}
            />
          </article>
          <article className="study-overview-sheet__summary-card">
            <span>운영 요일</span>
            <strong>{study.dayLabel}</strong>
          </article>
          <article className="study-overview-sheet__summary-card">
            <span>운영 시간</span>
            <strong>{study.timeLabel}</strong>
          </article>
          <article className="study-overview-sheet__summary-card">
            <span>진행 장소</span>
            <strong>{study.locationLabel}</strong>
          </article>
        </div>
      </section>

      <div className="study-overview-sheet__body">
        <section className="study-overview-sheet__info-grid">
          <article className="study-overview-sheet__panel study-overview-sheet__panel--info">
            <div className="study-overview-sheet__panel-header">
              <span className="section-kicker">준비물</span>
            </div>
            <p>{study.suppliesText}</p>
          </article>

          <article className="study-overview-sheet__panel study-overview-sheet__panel--info">
            <div className="study-overview-sheet__panel-header">
              <span className="section-kicker">참여 규칙</span>
            </div>
            <p>{study.rulesText}</p>
          </article>

          <article className="study-overview-sheet__panel study-overview-sheet__panel--info">
            <div className="study-overview-sheet__panel-header">
              <span className="section-kicker">주의사항</span>
            </div>
            <p>{study.cautionText}</p>
          </article>

          <article className="study-overview-sheet__panel study-overview-sheet__panel--info">
            <div className="study-overview-sheet__panel-header">
              <span className="section-kicker">태그</span>
            </div>

            {study.tags.length ? (
              <div className="tag-row study-overview-sheet__tag-grid">
                {study.tags.map((tag) => (
                  <span className="tag-chip" key={tag}>
                    {tag}
                  </span>
                ))}
              </div>
            ) : (
              <div className="study-overview-sheet__empty">등록된 태그가 아직 없어요.</div>
            )}
          </article>
        </section>

        <section className="study-overview-sheet__sessions">
          <article className="study-overview-sheet__panel study-overview-sheet__panel--sessions">
            <div className="study-overview-sheet__panel-header">
              <span className="section-kicker">회차 미리보기</span>
              <span className="home-card__meta">{study.sessions.length}개</span>
            </div>

            {study.sessions.length ? (
              <div className="study-overview-sheet__session-list">
                {study.sessions.map((session) => (
                  <article
                    className={
                      session.sessionType === 'BREAK'
                        ? 'study-overview-sheet__session-card is-break'
                        : 'study-overview-sheet__session-card'
                    }
                    key={session.sessionId}
                  >
                    <div className="study-overview-sheet__session-meta">
                      <span>{session.sessionNo}회차</span>
                      <span>{session.scheduledAt}</span>
                    </div>
                    <strong>{session.title}</strong>
                  </article>
                ))}
              </div>
            ) : (
              <div className="study-overview-sheet__empty">예정된 회차가 아직 없어요.</div>
            )}
          </article>
        </section>
      </div>
    </div>
  )
}
