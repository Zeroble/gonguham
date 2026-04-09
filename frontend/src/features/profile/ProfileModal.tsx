import { useEffect, useMemo, useState } from 'react'
import { api, type UserProfile } from '../../app/api'
import { useApp } from '../../app/useApp'
import { AvatarPreview } from '../avatar/AvatarPreview'
import { draftToRenderState, type AvatarDraft } from '../avatar/avatarCatalog'

type ProfileModalProps = {
  userId: number
  onClose: () => void
}

function toAvatarDraft(profile: UserProfile): AvatarDraft {
  return {
    bodyAssetKey: profile.avatar.appearance.bodyAssetKey,
    hair: profile.avatar.equipped.hair,
    top: profile.avatar.equipped.top,
    bottom: profile.avatar.equipped.bottom,
    shoes: profile.avatar.equipped.shoes,
    pupil: profile.avatar.equipped.pupil,
    eyebrow: profile.avatar.equipped.eyebrow,
    eyelash: profile.avatar.equipped.eyelash,
    mouth: profile.avatar.equipped.mouth,
  }
}

export function ProfileModal({ userId, onClose }: ProfileModalProps) {
  const { me, replaceMe, sessionUserId, showToast } = useApp()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [isEditingNickname, setIsEditingNickname] = useState(false)
  const [draftNickname, setDraftNickname] = useState('')
  const [isSavingNickname, setIsSavingNickname] = useState(false)
  const isOwnProfile = me?.id === userId

  useEffect(() => {
    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    window.addEventListener('keydown', handleEscape)
    return () => window.removeEventListener('keydown', handleEscape)
  }, [onClose])

  useEffect(() => {
    if (!sessionUserId) {
      return
    }

    let cancelled = false
    setProfile(null)
    setLoadError('')
    setIsLoading(true)
    setIsEditingNickname(false)

    async function loadProfile() {
      try {
        const nextProfile = await api.getUserProfile(sessionUserId, userId)

        if (!cancelled) {
          setProfile(nextProfile)
          setDraftNickname(isOwnProfile && me ? me.nickname : nextProfile.nickname)
        }
      } catch (error) {
        if (!cancelled) {
          setLoadError(error instanceof Error ? error.message : '프로필을 불러오지 못했어요.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadProfile()

    return () => {
      cancelled = true
    }
  }, [isOwnProfile, me, sessionUserId, userId])

  useEffect(() => {
    if (isOwnProfile && me) {
      setDraftNickname(me.nickname)
    }
  }, [isOwnProfile, me])

  const displayName = isOwnProfile && me ? me.nickname : profile?.nickname ?? ''
  const renderState = useMemo(
    () => (profile ? draftToRenderState(toAvatarDraft(profile)) : null),
    [profile],
  )
  const statCards = useMemo(() => {
    if (!profile) {
      return []
    }

    return [
      {
        label: '현재 참여중인 스터디',
        value: `${profile.stats.activeStudyCount}개`,
        helper: '현재 활성 멤버십 기준',
      },
      {
        label: '현재 보유 체크',
        value: `${profile.stats.currentChecks}개`,
        helper: '상점에서 바로 쓸 수 있어요',
      },
      {
        label: '누적 획득 체크',
        value: `${profile.stats.totalEarnedChecks}개`,
        helper: '레벨 계산 기준',
      },
      {
        label: '연속 출석 횟수',
        value: `${profile.stats.consecutiveAttendanceCount}회`,
        helper: '최신 종료 회차 기준',
      },
      {
        label: '총 출석 횟수',
        value: `${profile.stats.totalAttendanceCount}회`,
        helper: '실제 출석 확정 기준',
      },
      {
        label: '최근 2주 출석률',
        value: `${profile.stats.recentTwoWeekAttendanceRatePercent}%`,
        helper: `${profile.stats.recentTwoWeekAttendedCount}/${profile.stats.recentTwoWeekSessionCount}회`,
      },
      {
        label: '작성한 게시글',
        value: `${profile.stats.totalPostCount}개`,
        helper: '스터디 게시판 전체 기준',
      },
      {
        label: '작성한 댓글',
        value: `${profile.stats.totalCommentCount}개`,
        helper: '스터디 댓글 전체 기준',
      },
    ]
  }, [profile])

  function startNicknameEdit() {
    setDraftNickname(me?.nickname ?? profile?.nickname ?? '')
    setIsEditingNickname(true)
  }

  function cancelNicknameEdit() {
    setDraftNickname(me?.nickname ?? profile?.nickname ?? '')
    setIsEditingNickname(false)
  }

  async function handleSaveNickname() {
    if (!sessionUserId || !isOwnProfile) {
      return
    }

    try {
      setIsSavingNickname(true)
      const nextMe = await api.updateNickname(sessionUserId, draftNickname)
      replaceMe(nextMe)
      setProfile((currentProfile) =>
        currentProfile
          ? {
            ...currentProfile,
            nickname: nextMe.nickname,
            level: nextMe.level,
          }
          : currentProfile,
      )
      setIsEditingNickname(false)
      showToast('닉네임을 수정했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '닉네임을 수정하지 못했어요.')
    } finally {
      setIsSavingNickname(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <article
        className="modal-card profile-modal"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-card__header">
          <div>
            <span className="section-kicker">PROFILE</span>
            <h2>개인 프로필</h2>
          </div>
          <button className="soft-button" onClick={onClose} type="button">
            닫기
          </button>
        </div>

        {isLoading ? (
          <div className="profile-modal__state">프로필을 불러오는 중이에요.</div>
        ) : loadError ? (
          <div className="profile-modal__state is-error">{loadError}</div>
        ) : profile && renderState ? (
          <>
            <section className="profile-modal__hero">
              <div className="profile-modal__avatar-panel">
                <AvatarPreview
                  className="profile-modal__avatar"
                  size="summary"
                  state={renderState}
                />
                <p className="profile-modal__avatar-note">
                  아바타 변경은 커스터마이즈 화면에서 계속할 수 있어요.
                </p>
              </div>

              <div className="profile-modal__summary">
                <div className="profile-modal__identity">
                  <div className="profile-modal__identity-copy">
                    <span className="section-kicker">
                      {isOwnProfile ? 'MY PROFILE' : 'USER PROFILE'}
                    </span>

                    {isOwnProfile && isEditingNickname ? (
                      <>
                        <div className="profile-modal__nickname-row">
                          <input
                            autoFocus
                            className="field-control profile-modal__nickname-input"
                            maxLength={20}
                            onChange={(event) => setDraftNickname(event.target.value)}
                            placeholder="닉네임을 입력해주세요"
                            value={draftNickname}
                          />

                          <div className="profile-modal__nickname-actions">
                            <button
                              className="soft-button"
                              disabled={isSavingNickname}
                              onClick={cancelNicknameEdit}
                              type="button"
                            >
                              취소
                            </button>
                            <button
                              className="primary-button"
                              disabled={isSavingNickname || !draftNickname.trim()}
                              onClick={handleSaveNickname}
                              type="button"
                            >
                              {isSavingNickname ? '저장 중..' : '저장'}
                            </button>
                          </div>
                        </div>

                        <p className="profile-modal__editor-note">
                          공백 제외 20자까지 설정할 수 있어요.
                        </p>
                      </>
                    ) : (
                      <div className="profile-modal__nickname-row">
                        <strong>{displayName}</strong>
                        {isOwnProfile ? (
                          <button
                            className="soft-button profile-modal__nickname-action"
                            onClick={startNicknameEdit}
                            type="button"
                          >
                            편집
                          </button>
                        ) : null}
                      </div>
                    )}
                  </div>

                  <span className="profile-modal__level">Lv.{profile.level}</span>
                </div>

                <div className="profile-modal__progress-copy">
                  <strong>
                    다음 레벨까지 누적 체크 {profile.levelProgress.remainingChecksToNextLevel}개 남음
                  </strong>
                </div>

                <div className="profile-modal__progress-meta">
                  <span>
                    {profile.levelProgress.currentLevelStartTotalChecks} / {profile.levelProgress.nextLevelTargetTotalChecks} 체크
                  </span>
                  <strong>{profile.levelProgress.progressPercent}%</strong>
                </div>

                <div
                  aria-hidden="true"
                  className="profile-modal__progress-track"
                >
                  <span
                    className="profile-modal__progress-fill"
                    style={{ width: `${profile.levelProgress.progressPercent}%` }}
                  />
                </div>
              </div>
            </section>

            <section className="profile-stats-grid">
              {statCards.map((card) => (
                <article className="profile-stat-card" key={card.label}>
                  <span>{card.label}</span>
                  <strong>{card.value}</strong>
                  <p>{card.helper}</p>
                </article>
              ))}
            </section>
          </>
        ) : null}
      </article>
    </div>
  )
}
