import { useEffect, useEffectEvent, useState } from 'react'
import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom'
import { api, type DashboardResponse } from '../app/api'
import { useApp } from '../app/useApp'
import { AvatarPreview } from '../features/avatar/AvatarPreview'
import { summaryToRenderState } from '../features/avatar/avatarCatalog'
import { ProfileModal } from '../features/profile/ProfileModal'
import { ProfileNameButton } from '../features/profile/ProfileNameButton'
import gonguhamLogoSmall from '../assets/gonguham/logo-small.png'
import {
  buildAppShellStudySummary,
  type AppShellOutletContext,
} from './appShellDashboard'

const tabs = [
  { to: '/app/home', label: '내 스터디' },
  { to: '/app/studies', label: '스터디 찾기' },
  { to: '/app/create', label: '스터디 만들기' },
  { to: '/app/customize', label: '커스터마이징' },
]

export function AppShell() {
  const {
    activeProfileUserId,
    avatarSummary,
    closeProfile,
    isBooting,
    me,
    logout,
    sessionUserId,
    showToast,
    toast,
  } = useApp()
  const location = useLocation()
  const isHomeRoute = location.pathname === '/app/home'
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null)
  const [isDashboardLoading, setIsDashboardLoading] = useState(false)

  const handleDashboardLoadError = useEffectEvent((error: unknown) => {
    showToast(error instanceof Error ? error.message : '대시보드를 불러오지 못했어요.')
  })

  useEffect(() => {
    let cancelled = false

    async function loadDashboard() {
      if (!sessionUserId) {
        if (!cancelled) {
          setDashboard(null)
          setIsDashboardLoading(false)
        }
        return
      }

      if (!cancelled) {
        setIsDashboardLoading(true)
      }

      try {
        const nextDashboard = await api.getDashboard(sessionUserId)

        if (!cancelled) {
          setDashboard(nextDashboard)
        }
      } catch (error) {
        if (!cancelled) {
          setDashboard(null)
          handleDashboardLoadError(error)
        }
      } finally {
        if (!cancelled) {
          setIsDashboardLoading(false)
        }
      }
    }

    void loadDashboard()

    return () => {
      cancelled = true
    }
  }, [sessionUserId])

  async function refreshDashboard() {
    if (!sessionUserId) {
      setDashboard(null)
      setIsDashboardLoading(false)
      return
    }

    setIsDashboardLoading(true)

    try {
      const nextDashboard = await api.getDashboard(sessionUserId)
      setDashboard(nextDashboard)
    } catch (error) {
      showToast(error instanceof Error ? error.message : '대시보드를 불러오지 못했어요.')
    } finally {
      setIsDashboardLoading(false)
    }
  }

  const studySummary = dashboard
    ? buildAppShellStudySummary(dashboard)
    : '오늘 스터디를 확인하고 있어요.'

  const outletContext: AppShellOutletContext = {
    dashboard,
    isDashboardLoading,
    refreshDashboard,
  }

  if (isBooting) {
    return <div className="fullscreen-message">공구함을 불러오는 중입니다.</div>
  }

  if (!me) {
    return <Navigate to="/" replace />
  }

  return (
    <div className={isHomeRoute ? 'app-shell is-home-route' : 'app-shell'}>
      {toast ? (
        <div className="app-toast-region" aria-atomic="true" aria-live="polite">
          <div className="app-toast" key={toast.id} role="status">
            {toast.message}
          </div>
        </div>
      ) : null}

      <header className="top-summary-card">
        <div className="summary-brand">
          <img
            className="summary-brand__logo"
            src={gonguhamLogoSmall}
            alt="공구함"
          />
          <span style={{ textAlign: "center", color: "black" }}><strong>공</strong>부 같이할 사람 <strong>구함</strong></span>
        </div>

        <div className="summary-main">
          <span className="section-kicker">TODAY SUMMARY</span>
          <h2>{studySummary}</h2>

          <nav className="summary-tabs" aria-label="메인 탭">
            {tabs.map((tab) => (
              <NavLink
                key={tab.to}
                to={tab.to}
                className={({ isActive }) =>
                  isActive ? 'summary-tab is-active' : 'summary-tab'
                }
              >
                {tab.label}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="summary-side">
          <div className="summary-character-slot" aria-hidden="true">
            <AvatarPreview
              className="summary-character-slot__preview"
              size="profile"
              state={summaryToRenderState(avatarSummary)}
            />
          </div>

          <div className="summary-profile">
            <div className="summary-profile__identity">
              <ProfileNameButton
                className="profile-name-button is-summary"
                nickname={me.nickname}
                userId={me.id}
              />
              <span>Lv.{me.level}</span>
            </div>
            <button className="inline-text-button" onClick={logout} type="button">
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main className={isHomeRoute ? 'page-body is-home-route' : 'page-body'}>
        <Outlet context={outletContext} />
      </main>

      {activeProfileUserId ? (
        <ProfileModal onClose={closeProfile} userId={activeProfileUserId} />
      ) : null}
    </div>
  )
}
