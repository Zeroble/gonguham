import {
  createContext,
  useEffect,
  useState,
  type PropsWithChildren,
} from 'react'
import { api, type SessionUser } from './api'

type AppContextValue = {
  me: SessionUser | null
  sessionUserId: number | null
  isBooting: boolean
  loginDemo: () => Promise<SessionUser>
  logout: () => void
  refreshMe: () => Promise<SessionUser | null>
  replaceMe: (next: SessionUser) => void
}

const SESSION_KEY = 'gonguham-demo-user-id'

const AppContext = createContext<AppContextValue | null>(null)

export function AppProvider({ children }: PropsWithChildren) {
  const [sessionUserId, setSessionUserId] = useState<number | null>(() => {
    const raw = window.localStorage.getItem(SESSION_KEY)
    return raw ? Number(raw) : null
  })
  const [me, setMe] = useState<SessionUser | null>(null)
  const [isBooting, setIsBooting] = useState(true)

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      if (!sessionUserId) {
        setMe(null)
        setIsBooting(false)
        return
      }

      setIsBooting(true)
      try {
        const next = await api.getMe(sessionUserId)
        if (!cancelled) {
          setMe(next)
        }
      } catch {
        if (!cancelled) {
          setSessionUserId(null)
          window.localStorage.removeItem(SESSION_KEY)
          setMe(null)
        }
      } finally {
        if (!cancelled) {
          setIsBooting(false)
        }
      }
    }

    void bootstrap()

    return () => {
      cancelled = true
    }
  }, [sessionUserId])

  async function loginDemo() {
    const user = await api.demoLogin()
    window.localStorage.setItem(SESSION_KEY, String(user.id))
    setSessionUserId(user.id)
    setMe(user)
    setIsBooting(false)
    return user
  }

  function logout() {
    window.localStorage.removeItem(SESSION_KEY)
    setSessionUserId(null)
    setMe(null)
  }

  async function refreshMe() {
    if (!sessionUserId) {
      return null
    }
    const user = await api.getMe(sessionUserId)
    setMe(user)
    return user
  }

  function replaceMe(next: SessionUser) {
    setMe(next)
  }

  return (
    <AppContext.Provider
      value={{
        me,
        sessionUserId,
        isBooting,
        loginDemo,
        logout,
        refreshMe,
        replaceMe,
      }}
    >
      {children}
    </AppContext.Provider>
  )
}

export { AppContext }
