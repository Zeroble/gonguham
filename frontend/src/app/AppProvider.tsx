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

type CredentialsInput = {
  email: string
  password: string
}

type SignUpInput = CredentialsInput & {
  nickname: string
}

type AppContextValue = {
  me: SessionUser | null
  avatarSummary: AvatarSummary | null
  sessionUserId: number | null
  activeProfileUserId: number | null
  isBooting: boolean
  toast: AppToast | null
  login: (input: CredentialsInput) => Promise<SessionUser>
  signUp: (input: SignUpInput) => Promise<SessionUser>
  logout: () => void
  refreshMe: () => Promise<SessionUser | null>
  refreshAvatarSummary: () => Promise<AvatarSummary | null>
  openProfile: (userId: number) => void
  closeProfile: () => void
  replaceMe: (next: SessionUser) => void
  replaceAvatarSummary: (next: AvatarSummary | null) => void
  showToast: (message: string) => void
}

const AppContext = createContext<AppContextValue | null>(null)

export function AppProvider({ children }: PropsWithChildren) {
  const [activeProfileUserId, setActiveProfileUserId] = useState<number | null>(null)
  const [me, setMe] = useState<SessionUser | null>(null)
  const [avatarSummary, setAvatarSummary] = useState<AvatarSummary | null>(null)
  const [isBooting, setIsBooting] = useState(true)
  const [toast, setToast] = useState<AppToast | null>(null)
  const toastIdRef = useRef(0)
  const toastTimerRef = useRef<number | null>(null)
  const sessionUserId = me?.id ?? null

  useEffect(() => {
    let cancelled = false

    async function bootstrap() {
      setIsBooting(true)

      try {
        const next = await api.getMe()
        const nextAvatarSummary = await api.getAvatarSummary(next.id)

        if (!cancelled) {
          setMe(next)
          setAvatarSummary(nextAvatarSummary)
        }
      } catch {
        if (!cancelled) {
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
  }, [])

  useEffect(() => {
    return () => {
      if (toastTimerRef.current !== null) {
        window.clearTimeout(toastTimerRef.current)
      }
    }
  }, [])

  async function hydrateAfterAuth(user: SessionUser) {
    const nextAvatarSummary = await api.getAvatarSummary(user.id)
    setMe(user)
    setAvatarSummary(nextAvatarSummary)
    setIsBooting(false)
    return user
  }

  async function login(input: CredentialsInput) {
    const user = await api.login(input)
    return hydrateAfterAuth(user)
  }

  async function signUp(input: SignUpInput) {
    const user = await api.signUp(input)
    return hydrateAfterAuth(user)
  }

  function logout() {
    void api.logout().catch(() => undefined)
    setActiveProfileUserId(null)
    setMe(null)
    setAvatarSummary(null)
    setIsBooting(false)
  }

  async function refreshMe() {
    if (!sessionUserId) {
      return null
    }

    const user = await api.getMe()
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
        login,
        signUp,
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
