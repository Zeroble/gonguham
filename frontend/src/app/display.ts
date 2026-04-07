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
  { label: '월', value: '월요일' },
  { label: '화', value: '화요일' },
  { label: '수', value: '수요일' },
  { label: '목', value: '목요일' },
  { label: '금', value: '금요일' },
  { label: '토', value: '토요일' },
] as const

export const DAY_PICKER_OPTIONS = [
  { label: '월', value: 'MONDAY' },
  { label: '화', value: 'TUESDAY' },
  { label: '수', value: 'WEDNESDAY' },
  { label: '목', value: 'THURSDAY' },
  { label: '금', value: 'FRIDAY' },
  { label: '토', value: 'SATURDAY' },
] as const

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
  { label: '헤어', value: 'HAIR' },
  { label: '상의', value: 'TOP' },
  { label: '하의', value: 'BOTTOM' },
] as const

const studyTypeLabels: Record<string, string> = {
  MOGAKGONG: '모각공',
  TOPIC: '주제',
  FLASH: '반짝',
}

const rarityLabels: Record<string, string> = {
  BASIC: '기본',
  POINT: '포인트',
  SIGNATURE: '시그니처',
}

export function getStudyTypeLabel(value: string) {
  return studyTypeLabels[value] ?? value
}

export function getRarityLabel(value: string) {
  return rarityLabels[value] ?? value
}

export function matchesDayFilter(dayLabel: string, filter: string) {
  return filter ? dayLabel === filter : true
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
