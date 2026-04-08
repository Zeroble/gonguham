import {
  createContext,
  useEffect,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react'
import { api, type SessionUser } from './api'

type AppToast = {
  id: number
  message: string
}

type AppContextValue = {
  me: SessionUser | null
  sessionUserId: number | null
  isBooting: boolean
  toast: AppToast | null
  loginDemo: () => Promise<SessionUser>
  logout: () => void
  refreshMe: () => Promise<SessionUser | null>
  replaceMe: (next: SessionUser) => void
  showToast: (message: string) => void
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
  const [toast, setToast] = useState<AppToast | null>(null)
  const toastIdRef = useRef(0)
  const toastTimerRef = useRef<number | null>(null)

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

  useEffect(() => {
    return () => {
      if (toastTimerRef.current !== null) {
        window.clearTimeout(toastTimerRef.current)
      }
    }
  }, [])

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

  function showToast(message: string) {
    if (toastTimerRef.current !== null) {
      window.clearTimeout(toastTimerRef.current)
    }

    toastIdRef.current += 1
    setToast({ id: toastIdRef.current, message })
    toastTimerRef.current = window.setTimeout(() => {
      setToast(null)
      toastTimerRef.current = null
    }, 3000)
  }

  return (
    <AppContext.Provider
      value={{
        me,
        sessionUserId,
        isBooting,
        toast,
        loginDemo,
        logout,
        refreshMe,
        replaceMe,
        showToast,
      }}
    >
      {children}
    </AppContext.Provider>
  )
}

export { AppContext }
