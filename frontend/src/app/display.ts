export const STUDY_TYPE_OPTIONS = [
  { label: '전체', value: '' },
  { label: '모각공', value: 'MOGAKGONG' },
  { label: '주제', value: 'TOPIC' },
  { label: '반짝', value: 'FLASH' },
] as const

export const STUDY_TYPE_PICKER_OPTIONS = STUDY_TYPE_OPTIONS.filter(
  (option) => option.value,
)

export const DAY_FILTER_OPTIONS = [
  { label: '월', value: 'MONDAY' },
  { label: '화', value: 'TUESDAY' },
  { label: '수', value: 'WEDNESDAY' },
  { label: '목', value: 'THURSDAY' },
  { label: '금', value: 'FRIDAY' },
  { label: '토', value: 'SATURDAY' },
  { label: '일', value: 'SUNDAY' },
] as const

export const DAY_PICKER_OPTIONS = DAY_FILTER_OPTIONS

export const TIME_FILTER_OPTIONS = [
  { label: '오전', value: 'MORNING' },
  { label: '오후', value: 'AFTERNOON' },
  { label: '저녁', value: 'EVENING' },
] as const

export const PLACE_FILTER_OPTIONS = [
  { label: '온라인', value: 'ONLINE' },
  { label: '오프라인', value: 'OFFLINE' },
] as const

export const LOCATION_OPTIONS = [
  { label: '오프라인', value: 'OFFLINE' },
  { label: '온라인', value: 'ONLINE' },
] as const

export const AVATAR_CATEGORY_OPTIONS = [
  { label: '피부', value: 'BODY' },
  { label: '눈', value: 'PUPIL' },
  { label: '눈썹', value: 'EYEBROW' },
  { label: '속눈썹', value: 'EYELASH' },
  { label: '입', value: 'MOUTH' },
  { label: '헤어', value: 'HAIR' },
  { label: '상의', value: 'TOP' },
  { label: '하의', value: 'BOTTOM' },
  { label: '신발', value: 'SHOES' },
] as const

const studyTypeLabels: Record<string, string> = {
  MOGAKGONG: '모각공',
  TOPIC: '주제',
  FLASH: '반짝',
}

const dayShortLabels: Record<string, string> = {
  MONDAY: '월',
  TUESDAY: '화',
  WEDNESDAY: '수',
  THURSDAY: '목',
  FRIDAY: '금',
  SATURDAY: '토',
  SUNDAY: '일',
}

export function getStudyTypeLabel(value: string) {
  return studyTypeLabels[value] ?? value
}

export function getDayLabel(value: string) {
  return dayShortLabels[value] ?? value
}

export function formatDaysOfWeek(daysOfWeek: string[]) {
  return daysOfWeek
    .map((day) => getDayLabel(day))
    .join('·')
}

export function matchesDayFilter(daysOfWeek: string[], filters: string[]) {
  if (!filters.length) {
    return true
  }

  return filters.some((filter) => daysOfWeek.includes(filter))
}

export function matchesPlaceFilter(locationLabel: string, filter: string) {
  if (!filter) {
    return true
  }

  return locationLabel.includes(filter === 'ONLINE' ? '온라인' : '오프라인')
}

export function matchesTimeFilter(timeLabel: string, filter: string) {
  if (!filter) {
    return true
  }

  const hour = Number.parseInt(timeLabel.slice(0, 2), 10)

  if (Number.isNaN(hour)) {
    return true
  }

  if (filter === 'MORNING') {
    return hour < 12
  }

  if (filter === 'AFTERNOON') {
    return hour >= 12 && hour < 18
  }

  return hour >= 18
}
