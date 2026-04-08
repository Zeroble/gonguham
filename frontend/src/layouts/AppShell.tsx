import { NavLink, Navigate, Outlet } from 'react-router-dom'
import { useApp } from '../app/useApp'

const tabs = [
  { to: '/app/home', label: '내 스터디' },
  { to: '/app/studies', label: '스터디 찾기' },
  { to: '/app/create', label: '스터디 만들기' },
  { to: '/app/customize', label: '커스터마이징' },
]

export function AppShell() {
  const { isBooting, me, logout } = useApp()

  if (isBooting) {
    return <div className="fullscreen-message">공구함을 불러오는 중입니다.</div>
  }

  if (!me) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="app-shell">
      <header className="top-summary-card">
        <div className="summary-brand">
          <strong>공구함</strong>
          <span>공부 같이할 사람 구함</span>
        </div>

        <div className="summary-main">
          <span className="section-kicker">TODAY SUMMARY</span>
          <h2>오늘, 2개의 스터디가 예정되어 있어요.</h2>

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
            <div className="summary-character-slot__placeholder">
              <span>
                커스터마이징
                <br />
                캐릭터가
                <br />
                여기 표시됨
              </span>
            </div>
          </div>

          <div className="summary-profile">
            <div className="summary-profile__identity">
              <strong>{me.nickname}</strong>
              <span>Lv.{me.level}</span>
            </div>
            <button className="inline-text-button" onClick={logout} type="button">
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main className="page-body">
        <Outlet />
      </main>
    </div>
  )
}
