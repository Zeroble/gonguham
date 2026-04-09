const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export type SessionUser = {
  id: number
  nickname: string
  currentChecks: number
  totalEarnedChecks: number
  level: number
  profileImageUrl: string | null
}

export type DashboardResponse = {
  todayScheduledCount: number
  defaultStudyId: number | null
  joinedStudies: Array<{
    studyId: number
    typeLabel: string
    title: string
    timeLabel: string
    locationLabel: string
  }>
  studyPanels: StudyHomePanel[]
}

export type StudyHomePanel = {
  studyId: number
  title: string
  description: string
  locationText: string
  isLeader: boolean
  currentSessionId: number | null
  attendanceSessionId: number | null
  attendanceSessionLabel: string | null
  sessions: Array<{
    sessionId: number
    roundLabel: string
    title: string
    nodeState: string
    scheduledAt: string
    planned: boolean
  }>
  notice: FeedItem | null
  posts: FeedItem[]
  attendanceRoster: AttendanceRosterEntry[]
}

export type AttendanceRosterEntry = {
  userId: number
  nickname: string
  planned: boolean
  attendanceStatus: string | null
}

export type SessionAttendancePanel = {
  sessionId: number
  sessionLabel: string
  roster: AttendanceRosterEntry[]
}

export type FeedItem = {
  postId: number
  type: string
  title: string
  content: string
  authorNickname: string
  createdAt: string
}

export type StudyCard = {
  studyId: number
  type: string
  title: string
  description: string
  dayLabel: string
  timeLabel: string
  locationLabel: string
  tags: string[]
  slotsLabel: string
  joined: boolean
}

export type StudyDetail = {
  studyId: number
  type: string
  title: string
  description: string
  leaderNickname: string
  dayLabel: string
  timeLabel: string
  locationLabel: string
  rulesText: string
  suppliesText: string
  cautionText: string
  tags: string[]
  slotsLabel: string
  joined: boolean
  sessions: Array<{
    sessionId: number
    sessionNo: number
    title: string
    scheduledAt: string
  }>
  notice: FeedItem | null
  posts: FeedItem[]
}

export type AvatarSummary = {
  currentChecks: number
  totalEarnedChecks: number
  level: number
  appearance: AvatarAppearance
  equipped: {
    hair: AvatarSlot | null
    top: AvatarSlot | null
    bottom: AvatarSlot | null
  }
}

export type AvatarAppearance = {
  bodyAssetKey: string
  pupilAssetKey: string
  eyebrowAssetKey: string
  eyelashAssetKey: string
  mouthAssetKey: string
}

export type AvatarSlot = {
  itemId: number
  name: string
  assetKey: string
}

export type AvatarShopItem = {
  itemId: number
  category: string
  rarity: string
  name: string
  description: string
  priceChecks: number
  assetKey: string
  owned: boolean
  equipped: boolean
}

export type SaveAvatarAppearanceRequest = {
  hairItemId: number | null
  topItemId: number | null
  bottomItemId: number | null
  bodyAssetKey: string
  pupilAssetKey: string
  eyebrowAssetKey: string
  eyelashAssetKey: string
  mouthAssetKey: string
}

type FetchOptions = Omit<RequestInit, 'headers' | 'body'> & {
  body?: unknown
}

async function request<T>(path: string, userId?: number | null, options?: FetchOptions): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(userId ? { 'X-Demo-User-Id': String(userId) } : {}),
    },
    body: options?.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  return response.json() as Promise<T>
}

async function readErrorMessage(response: Response) {
  const fallback = '요청에 실패했습니다.'
  const raw = await response.text()

  if (!raw) {
    return fallback
  }

  const contentType = response.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    try {
      const payload = JSON.parse(raw) as Record<string, unknown>
      const candidates = [payload.message, payload.detail, payload.error]
      const message = candidates.find(
        (value): value is string => typeof value === 'string' && value.trim().length > 0,
      )

      if (message) {
        return message
      }
    } catch {
      return raw
    }
  }

  return raw
}

export const api = {
  demoLogin() {
    return request<SessionUser>('/api/v1/auth/demo-login', null, {
      method: 'POST',
      body: {},
    })
  },
  getMe(userId: number) {
    return request<SessionUser>('/api/v1/me', userId)
  },
  updateNickname(userId: number, nickname: string) {
    return request<SessionUser>('/api/v1/me', userId, {
      method: 'PATCH',
      body: { nickname },
    })
  },
  getDashboard(userId: number, studyId?: number | null) {
    const suffix = studyId ? `?studyId=${studyId}` : ''
    return request<DashboardResponse>(`/api/v1/dashboard${suffix}`, userId)
  },
  getStudyHomePanel(userId: number, studyId: number) {
    return request<StudyHomePanel>(`/api/v1/dashboard/studies/${studyId}/panel`, userId)
  },
  getSessionAttendancePanel(userId: number, sessionId: number) {
    return request<SessionAttendancePanel>(`/api/v1/sessions/${sessionId}/attendance-roster`, userId)
  },
  getStudies(userId: number, keyword = '', type = '') {
    const query = new URLSearchParams()
    if (keyword) query.set('keyword', keyword)
    if (type) query.set('type', type)
    const suffix = query.toString() ? `?${query}` : ''
    return request<StudyCard[]>(`/api/v1/studies${suffix}`, userId)
  },
  getStudyDetail(userId: number, studyId: number) {
    return request<StudyDetail>(`/api/v1/studies/${studyId}`, userId)
  },
  joinStudy(userId: number, studyId: number) {
    return request<StudyDetail>(`/api/v1/studies/${studyId}/join`, userId, {
      method: 'POST',
      body: {},
    })
  },
  createStudy(userId: number, body: unknown) {
    return request<StudyDetail>('/api/v1/studies', userId, {
      method: 'POST',
      body,
    })
  },
  updateParticipation(userId: number, sessionId: number, planned: boolean) {
    return request<{ sessionId: number; planned: boolean }>(`/api/v1/sessions/${sessionId}/participation`, userId, {
      method: 'PATCH',
      body: { planned },
    })
  },
  updateAttendance(
    userId: number,
    sessionId: number,
    entries: Array<{ userId: number; status: string }>,
  ) {
    return request<{ sessionId: number; awardedUserIds: number[] }>(
      `/api/v1/sessions/${sessionId}/attendance`,
      userId,
      {
        method: 'POST',
        body: { entries },
      },
    )
  },
  createPost(userId: number, studyId: number, body: { type: 'POST' | 'NOTICE'; title: string; content: string }) {
    return request<FeedItem>(`/api/v1/studies/${studyId}/posts`, userId, {
      method: 'POST',
      body,
    })
  },
  getAvatarSummary(userId: number) {
    return request<AvatarSummary>('/api/v1/avatar', userId)
  },
  getAvatarShop(userId: number, category: string) {
    const suffix = category ? `?category=${category}` : ''
    return request<AvatarShopItem[]>(`/api/v1/avatar/shop${suffix}`, userId)
  },
  purchaseAvatarItem(userId: number, itemId: number) {
    return request<SessionUser>(`/api/v1/avatar/items/${itemId}/purchase`, userId, {
      method: 'POST',
      body: {},
    })
  },
  equipAvatarItem(userId: number, itemId: number) {
    return request<AvatarSummary>('/api/v1/avatar/equip', userId, {
      method: 'POST',
      body: { itemId },
    })
  },
  saveAvatarAppearance(userId: number, body: SaveAvatarAppearanceRequest) {
    return request<AvatarSummary>('/api/v1/avatar/appearance', userId, {
      method: 'PUT',
      body,
    })
  },
}
