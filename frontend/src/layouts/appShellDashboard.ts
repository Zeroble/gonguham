import type { DashboardResponse } from '../app/api'

const DAY_IN_MS = 24 * 60 * 60 * 1000
const WEEKDAY_LABELS = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일']

type DashboardSession = DashboardResponse['studyPanels'][number]['sessions'][number]

export type AppShellOutletContext = {
  dashboard: DashboardResponse | null
  isDashboardLoading: boolean
  refreshDashboard: () => Promise<void>
}

function parseScheduledAtValue(value: string) {
  const [datePart = '', timePart = '00:00'] = value.split('T')
  const [year = 0, month = 1, day = 1] = datePart.split('-').map(Number)
  const [hour = 0, minute = 0] = timePart.split(':').map(Number)

  return new Date(year, month - 1, day, hour, minute)
}

function startOfDay(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function isSameDay(left: Date, right: Date) {
  return (
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate()
  )
}

function getWeekStart(date: Date) {
  const dayOfWeek = date.getDay()
  const diffFromMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + diffFromMonday)
}

function formatRelativeStudyDate(target: Date, now: Date) {
  const targetDay = startOfDay(target)
  const today = startOfDay(now)
  const dayDiff = Math.round((targetDay.getTime() - today.getTime()) / DAY_IN_MS)
  const weekdayLabel = WEEKDAY_LABELS[targetDay.getDay()]

  if (dayDiff === 1) {
    return '내일'
  }

  if (dayDiff === 2) {
    return '모레'
  }

  const thisWeekStart = getWeekStart(today)
  const targetWeekStart = getWeekStart(targetDay)
  const nextWeekStart = new Date(thisWeekStart)
  nextWeekStart.setDate(thisWeekStart.getDate() + 7)

  if (targetWeekStart.getTime() === thisWeekStart.getTime()) {
    return `이번 ${weekdayLabel}`
  }

  if (targetWeekStart.getTime() === nextWeekStart.getTime()) {
    return `다음주 ${weekdayLabel}`
  }

  return `${targetDay.getMonth() + 1}월 ${targetDay.getDate()}일 ${weekdayLabel}`
}

function toSortedSessions(dashboard: DashboardResponse) {
  return dashboard.studyPanels
    .flatMap((panel) => panel.sessions)
    .map((session) => ({
      ...session,
      scheduledDate: parseScheduledAtValue(session.scheduledAtValue),
    }))
    .sort((left, right) => left.scheduledDate.getTime() - right.scheduledDate.getTime())
}

function isRegularSession(session: DashboardSession) {
  return session.sessionType === 'REGULAR'
}

function isBreakSession(session: DashboardSession) {
  return session.sessionType === 'BREAK'
}

export function buildAppShellStudySummary(
  dashboard: DashboardResponse,
  now = new Date(),
) {
  const sessions = toSortedSessions(dashboard)
  const todayRegularSessions = sessions.filter(
    (session) => isRegularSession(session) && isSameDay(session.scheduledDate, now),
  )

  if (todayRegularSessions.length > 0) {
    return `오늘, ${todayRegularSessions.length}개의 스터디가 예정되어 있어요.`
  }

  const todayBreakSessions = sessions.filter(
    (session) => isBreakSession(session) && isSameDay(session.scheduledDate, now),
  )

  if (todayBreakSessions.length > 0) {
    return '오늘, 예정된 스터디가 휴회해요.'
  }

  const today = startOfDay(now)
  const nextSession = sessions.find((session) => session.scheduledDate.getTime() > today.getTime())

  if (!nextSession) {
    return '예정된 스터디가 없어요.'
  }

  if (isBreakSession(nextSession)) {
    return '다음 스터디가 휴회입니다.'
  }

  return `다음 스터디는 ${formatRelativeStudyDate(nextSession.scheduledDate, now)} 예정되어 있어요.`
}
