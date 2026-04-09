import type {
  AvatarAppearance,
  AvatarShopItem,
  AvatarSlot,
  AvatarSummary,
  SaveAvatarAppearanceRequest,
} from '../../app/api'

const assetModules = {
  ...import.meta.glob('../../../assets/body/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/PUPILS/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/EYEBROWS/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/EYELASHES/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/MOUTH/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/hair_back/COLOR/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/hair/COLOR/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/bangs/COLOR/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/top/colors/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/bottom/color/*.png', { eager: true, import: 'default' }),
} as Record<string, string>

export type AvatarDraft = AvatarAppearance & {
  hair: AvatarSlot | null
  top: AvatarSlot | null
  bottom: AvatarSlot | null
}

export type AvatarRenderState = AvatarAppearance & {
  hairAssetKey: string | null
  topAssetKey: string | null
  bottomAssetKey: string | null
}

type AssetOption = {
  assetKey: string
  label: string
}

export const DEFAULT_AVATAR_APPEARANCE: AvatarAppearance = {
  bodyAssetKey: 'body-01',
  pupilAssetKey: 'pupil-01',
  eyebrowAssetKey: 'eyebrow-01',
  eyelashAssetKey: 'eyelash-01',
  mouthAssetKey: 'mouth-01',
}

export const BODY_OPTIONS = createNumberedOptions('body', 29, '톤')
export const PUPIL_OPTIONS = createNumberedOptions('pupil', 16, '눈')
export const EYEBROW_OPTIONS = createNumberedOptions('eyebrow', 5, '눈썹')
export const EYELASH_OPTIONS = createNumberedOptions('eyelash', 5, '속눈썹')
export const MOUTH_OPTIONS = createNumberedOptions('mouth', 20, '입')

export function createDefaultDraft(): AvatarDraft {
  return {
    ...DEFAULT_AVATAR_APPEARANCE,
    hair: null,
    top: null,
    bottom: null,
  }
}

export function summaryToDraft(summary: AvatarSummary | null): AvatarDraft {
  if (!summary) {
    return createDefaultDraft()
  }

  return {
    ...summary.appearance,
    hair: summary.equipped.hair,
    top: summary.equipped.top,
    bottom: summary.equipped.bottom,
  }
}

export function summaryToRenderState(summary: AvatarSummary | null): AvatarRenderState {
  return draftToRenderState(summaryToDraft(summary))
}

export function draftToRenderState(draft: AvatarDraft): AvatarRenderState {
  return {
    bodyAssetKey: draft.bodyAssetKey,
    pupilAssetKey: draft.pupilAssetKey,
    eyebrowAssetKey: draft.eyebrowAssetKey,
    eyelashAssetKey: draft.eyelashAssetKey,
    mouthAssetKey: draft.mouthAssetKey,
    hairAssetKey: draft.hair?.assetKey ?? null,
    topAssetKey: draft.top?.assetKey ?? null,
    bottomAssetKey: draft.bottom?.assetKey ?? null,
  }
}

export function draftToAppearanceRequest(draft: AvatarDraft): SaveAvatarAppearanceRequest {
  return {
    hairItemId: draft.hair?.itemId ?? null,
    topItemId: draft.top?.itemId ?? null,
    bottomItemId: draft.bottom?.itemId ?? null,
    bodyAssetKey: draft.bodyAssetKey,
    pupilAssetKey: draft.pupilAssetKey,
    eyebrowAssetKey: draft.eyebrowAssetKey,
    eyelashAssetKey: draft.eyelashAssetKey,
    mouthAssetKey: draft.mouthAssetKey,
  }
}

export function isSameDraft(left: AvatarDraft, right: AvatarDraft) {
  return (
    left.bodyAssetKey === right.bodyAssetKey &&
    left.pupilAssetKey === right.pupilAssetKey &&
    left.eyebrowAssetKey === right.eyebrowAssetKey &&
    left.eyelashAssetKey === right.eyelashAssetKey &&
    left.mouthAssetKey === right.mouthAssetKey &&
    left.hair?.itemId === right.hair?.itemId &&
    left.top?.itemId === right.top?.itemId &&
    left.bottom?.itemId === right.bottom?.itemId
  )
}

export function applyShopItemToDraft(draft: AvatarDraft, item: AvatarShopItem): AvatarDraft {
  const slot = {
    itemId: item.itemId,
    name: item.name,
    assetKey: item.assetKey,
  }

  switch (item.category) {
    case 'HAIR':
      return { ...draft, hair: slot }
    case 'TOP':
      return { ...draft, top: slot }
    case 'BOTTOM':
      return { ...draft, bottom: slot }
    default:
      return draft
  }
}

export function buildItemPreviewState(draft: AvatarDraft, item: AvatarShopItem): AvatarRenderState {
  const current = draftToRenderState(draft)

  switch (item.category) {
    case 'HAIR':
      return { ...current, hairAssetKey: item.assetKey }
    case 'TOP':
      return { ...current, topAssetKey: item.assetKey }
    case 'BOTTOM':
      return { ...current, bottomAssetKey: item.assetKey }
    default:
      return current
  }
}

export function getAvatarLayers(state: AvatarRenderState) {
  return [
    createLayer('hair-back', state.hairAssetKey ? resolveHairAsset(state.hairAssetKey, 'back') : null),
    createLayer('body', resolveBodyAsset(state.bodyAssetKey)),
    createLayer('bottom', state.bottomAssetKey ? resolveBottomAsset(state.bottomAssetKey) : null),
    createLayer('top', state.topAssetKey ? resolveTopAsset(state.topAssetKey) : null),
    createLayer('pupil', resolvePupilAsset(state.pupilAssetKey)),
    createLayer('eyebrow', resolveEyebrowAsset(state.eyebrowAssetKey)),
    createLayer('eyelash', resolveEyelashAsset(state.eyelashAssetKey)),
    createLayer('mouth', resolveMouthAsset(state.mouthAssetKey)),
    createLayer('hair-front', state.hairAssetKey ? resolveHairAsset(state.hairAssetKey, 'front') : null),
    createLayer('bangs', state.hairAssetKey ? resolveHairAsset(state.hairAssetKey, 'bangs') : null),
  ].filter((layer): layer is { id: string; src: string } => layer !== null)
}

function createLayer(id: string, src: string | null) {
  return src ? { id, src } : null
}

function resolveBodyAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'body')
  return resolveAsset(`body/${Number(assetNo)}.png`)
}

function resolvePupilAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'pupil')
  return resolveAsset(`PUPILS/${Number(assetNo)}.png`)
}

function resolveEyebrowAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'eyebrow')
  return resolveAsset(`EYEBROWS/${Number(assetNo)}.png`)
}

function resolveEyelashAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'eyelash')
  return resolveAsset(`EYELASHES/${Number(assetNo)}.png`)
}

function resolveMouthAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'mouth')
  return resolveAsset(`MOUTH/${Number(assetNo)}.png`)
}

function resolveTopAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'top')
  return resolveAsset(`top/colors/${Number(assetNo)}.png`)
}

function resolveBottomAsset(assetKey: string) {
  const assetNo = parseSimpleAssetKey(assetKey, 'bottom')
  return resolveAsset(`bottom/color/${Number(assetNo)}.png`)
}

function resolveHairAsset(assetKey: string, part: 'back' | 'front' | 'bangs') {
  const match = assetKey.match(/^hair-(\d{2})-([a-h])$/)
  if (!match) {
    throw new Error(`알 수 없는 헤어 에셋 키입니다: ${assetKey}`)
  }

  const assetNo = Number(match[1])
  const suffix = match[2] === 'a' ? '' : match[2]
  const fileName = `${assetNo}${suffix}.png`

  if (part === 'back') {
    return resolveAsset(`hair_back/COLOR/${fileName}`)
  }

  if (part === 'front') {
    return resolveAsset(`hair/COLOR/${fileName}`)
  }

  return resolveAsset(`bangs/COLOR/${fileName}`)
}

function parseSimpleAssetKey(assetKey: string, prefix: string) {
  const match = assetKey.match(new RegExp(`^${prefix}-(\\d{2})$`))
  if (!match) {
    throw new Error(`알 수 없는 에셋 키입니다: ${assetKey}`)
  }

  return match[1]
}

function resolveAsset(relativePath: string) {
  const key = `../../../assets/${relativePath}`
  const src = assetModules[key]

  if (!src) {
    throw new Error(`에셋을 찾을 수 없습니다: ${relativePath}`)
  }

  return src
}

function createNumberedOptions(prefix: string, count: number, labelPrefix: string): AssetOption[] {
  return Array.from({ length: count }, (_, index) => {
    const assetNo = String(index + 1).padStart(2, '0')
    return {
      assetKey: `${prefix}-${assetNo}`,
      label: `${labelPrefix} ${assetNo}`,
    }
  })
}
