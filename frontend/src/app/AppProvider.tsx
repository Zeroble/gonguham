import {
  createContext,
  useEffect,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react'
import { api, type AvatarSummary, type SessionUser } from './api'

type AppToast = {
  id: number
  message: string
}

type AppContextValue = {
  me: SessionUser | null
  avatarSummary: AvatarSummary | null
  sessionUserId: number | null
  activeProfileUserId: number | null
  isBooting: boolean
  toast: AppToast | null
  loginDemo: () => Promise<SessionUser>
  logout: () => void
  refreshMe: () => Promise<SessionUser | null>
  refreshAvatarSummary: () => Promise<AvatarSummary | null>
  openProfile: (userId: number) => void
  closeProfile: () => void
  replaceMe: (next: SessionUser) => void
  replaceAvatarSummary: (next: AvatarSummary | null) => void
  showToast: (message: string) => void
}

const SESSION_KEY = 'gonguham-demo-user-id'

const AppContext = createContext<AppContextValue | null>(null)

export function AppProvider({ children }: PropsWithChildren) {
  const [sessionUserId, setSessionUserId] = useState<number | null>(() => {
    const raw = window.localStorage.getItem(SESSION_KEY)
    return raw ? Number(raw) : null
  })
  const [activeProfileUserId, setActiveProfileUserId] = useState<number | null>(null)
  const [me, setMe] = useState<SessionUser | null>(null)
  const [avatarSummary, setAvatarSummary] = useState<AvatarSummary | null>(null)
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
        const [next, nextAvatarSummary] = await Promise.all([
          api.getMe(sessionUserId),
          api.getAvatarSummary(sessionUserId),
        ])
        if (!cancelled) {
          setMe(next)
          setAvatarSummary(nextAvatarSummary)
        }
      } catch {
        if (!cancelled) {
          setSessionUserId(null)
          window.localStorage.removeItem(SESSION_KEY)
          setActiveProfileUserId(null)
          setMe(null)
          setAvatarSummary(null)
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
    const nextAvatarSummary = await api.getAvatarSummary(user.id)
    window.localStorage.setItem(SESSION_KEY, String(user.id))
    setSessionUserId(user.id)
    setMe(user)
    setAvatarSummary(nextAvatarSummary)
    setIsBooting(false)
    return user
  }

  function logout() {
    window.localStorage.removeItem(SESSION_KEY)
    setSessionUserId(null)
    setActiveProfileUserId(null)
    setMe(null)
    setAvatarSummary(null)
  }

  async function refreshMe() {
    if (!sessionUserId) {
      return null
    }
    const user = await api.getMe(sessionUserId)
    setMe(user)
    return user
  }

  async function refreshAvatarSummary() {
    if (!sessionUserId) {
      return null
    }
    const nextAvatarSummary = await api.getAvatarSummary(sessionUserId)
    setAvatarSummary(nextAvatarSummary)
    return nextAvatarSummary
  }

  function replaceMe(next: SessionUser) {
    setMe(next)
  }

  function replaceAvatarSummary(next: AvatarSummary | null) {
    setAvatarSummary(next)
  }

  function openProfile(userId: number) {
    setActiveProfileUserId(userId)
  }

  function closeProfile() {
    setActiveProfileUserId(null)
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
        avatarSummary,
        sessionUserId,
        activeProfileUserId,
        isBooting,
        toast,
        loginDemo,
        logout,
        refreshMe,
        refreshAvatarSummary,
        openProfile,
        closeProfile,
        replaceMe,
        replaceAvatarSummary,
        showToast,
      }}
    >
      {children}
    </AppContext.Provider>
  )
}

export { AppContext }
