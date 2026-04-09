import type {
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
  ...import.meta.glob('../../../assets/shoes/*.png', { eager: true, import: 'default' }),
  ...import.meta.glob('../../../assets/shoes/COLOR/*.png', { eager: true, import: 'default' }),
} as Record<string, string>

export type AvatarDraft = {
  bodyAssetKey: string
  hair: AvatarSlot | null
  top: AvatarSlot | null
  bottom: AvatarSlot | null
  shoes: AvatarSlot | null
  pupil: AvatarSlot | null
  eyebrow: AvatarSlot | null
  eyelash: AvatarSlot | null
  mouth: AvatarSlot | null
}

export type AvatarRenderState = {
  bodyAssetKey: string
  pupilAssetKey: string
  eyebrowAssetKey: string
  eyelashAssetKey: string
  mouthAssetKey: string
  hairAssetKey: string | null
  topAssetKey: string | null
  bottomAssetKey: string | null
  shoesAssetKey: string | null
}

type AssetOption = {
  assetKey: string
  label: string
}

const DEFAULT_BODY_ASSET_KEY = 'body-01'
const DEFAULT_PUPIL_ASSET_KEY = 'pupil-01'
const DEFAULT_EYEBROW_ASSET_KEY = 'eyebrow-01'
const DEFAULT_EYELASH_ASSET_KEY = 'eyelash-01'
const DEFAULT_MOUTH_ASSET_KEY = 'mouth-01'
const DEFAULT_HAIR_ITEM_ASSET_KEY = 'hair-01-f'
const DEFAULT_TOP_ITEM_ASSET_KEY = 'top-06'
const DEFAULT_BOTTOM_ITEM_ASSET_KEY = 'bottom-04'
const DEFAULT_SHOES_ITEM_ASSET_KEY: string | null = null

export const BODY_OPTIONS = createNumberedOptions('body', 29, '톤')

export function createDefaultDraft(): AvatarDraft {
  return {
    bodyAssetKey: DEFAULT_BODY_ASSET_KEY,
    hair: null,
    top: null,
    bottom: null,
    shoes: null,
    pupil: null,
    eyebrow: null,
    eyelash: null,
    mouth: null,
  }
}

export function summaryToDraft(summary: AvatarSummary | null): AvatarDraft {
  if (!summary) {
    return createDefaultDraft()
  }

  return {
    bodyAssetKey: summary.appearance.bodyAssetKey,
    hair: summary.equipped.hair,
    top: summary.equipped.top,
    bottom: summary.equipped.bottom,
    shoes: summary.equipped.shoes,
    pupil: summary.equipped.pupil,
    eyebrow: summary.equipped.eyebrow,
    eyelash: summary.equipped.eyelash,
    mouth: summary.equipped.mouth,
  }
}

export function summaryToRenderState(summary: AvatarSummary | null): AvatarRenderState {
  return draftToRenderState(summaryToDraft(summary))
}

export function draftToRenderState(draft: AvatarDraft): AvatarRenderState {
  return {
    bodyAssetKey: draft.bodyAssetKey,
    pupilAssetKey: draft.pupil?.assetKey ?? DEFAULT_PUPIL_ASSET_KEY,
    eyebrowAssetKey: draft.eyebrow?.assetKey ?? DEFAULT_EYEBROW_ASSET_KEY,
    eyelashAssetKey: draft.eyelash?.assetKey ?? DEFAULT_EYELASH_ASSET_KEY,
    mouthAssetKey: draft.mouth?.assetKey ?? DEFAULT_MOUTH_ASSET_KEY,
    hairAssetKey: draft.hair?.assetKey ?? DEFAULT_HAIR_ITEM_ASSET_KEY,
    topAssetKey: draft.top?.assetKey ?? DEFAULT_TOP_ITEM_ASSET_KEY,
    bottomAssetKey: draft.bottom?.assetKey ?? DEFAULT_BOTTOM_ITEM_ASSET_KEY,
    shoesAssetKey: draft.shoes?.assetKey ?? DEFAULT_SHOES_ITEM_ASSET_KEY,
  }
}

export function draftToAppearanceRequest(draft: AvatarDraft): SaveAvatarAppearanceRequest {
  return {
    hairItemId: draft.hair?.itemId ?? null,
    topItemId: draft.top?.itemId ?? null,
    bottomItemId: draft.bottom?.itemId ?? null,
    shoesItemId: draft.shoes?.itemId ?? null,
    pupilItemId: draft.pupil?.itemId ?? null,
    eyebrowItemId: draft.eyebrow?.itemId ?? null,
    eyelashItemId: draft.eyelash?.itemId ?? null,
    mouthItemId: draft.mouth?.itemId ?? null,
    bodyAssetKey: draft.bodyAssetKey,
  }
}

export function isSameDraft(left: AvatarDraft, right: AvatarDraft) {
  return (
    left.bodyAssetKey === right.bodyAssetKey &&
    left.hair?.itemId === right.hair?.itemId &&
    left.top?.itemId === right.top?.itemId &&
    left.bottom?.itemId === right.bottom?.itemId &&
    left.shoes?.itemId === right.shoes?.itemId &&
    left.pupil?.itemId === right.pupil?.itemId &&
    left.eyebrow?.itemId === right.eyebrow?.itemId &&
    left.eyelash?.itemId === right.eyelash?.itemId &&
    left.mouth?.itemId === right.mouth?.itemId
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
    case 'SHOES':
      return { ...draft, shoes: slot }
    case 'PUPIL':
      return { ...draft, pupil: slot }
    case 'EYEBROW':
      return { ...draft, eyebrow: slot }
    case 'EYELASH':
      return { ...draft, eyelash: slot }
    case 'MOUTH':
      return { ...draft, mouth: slot }
    default:
      return draft
  }
}

export function buildItemPreviewState(draft: AvatarDraft, item: AvatarShopItem): AvatarRenderState {
  return draftToRenderState(applyShopItemToDraft(draft, item))
}

export function buildBodyPreviewState(draft: AvatarDraft, bodyAssetKey: string): AvatarRenderState {
  return draftToRenderState({
    ...draft,
    bodyAssetKey,
  })
}

export function getSelectedItemId(draft: AvatarDraft, category: string) {
  switch (category) {
    case 'HAIR':
      return draft.hair?.itemId ?? null
    case 'TOP':
      return draft.top?.itemId ?? null
    case 'BOTTOM':
      return draft.bottom?.itemId ?? null
    case 'SHOES':
      return draft.shoes?.itemId ?? null
    case 'PUPIL':
      return draft.pupil?.itemId ?? null
    case 'EYEBROW':
      return draft.eyebrow?.itemId ?? null
    case 'EYELASH':
      return draft.eyelash?.itemId ?? null
    case 'MOUTH':
      return draft.mouth?.itemId ?? null
    default:
      return null
  }
}

export function getAvatarLayers(state: AvatarRenderState, scope: 'full' | 'face' = 'full') {
  const layers = [
    createLayer('hair-back', state.hairAssetKey ? resolveHairAsset(state.hairAssetKey, 'back') : null),
    createLayer('body', resolveBodyAsset(state.bodyAssetKey)),
    createLayer('shoes-color', state.shoesAssetKey ? resolveShoesAsset(state.shoesAssetKey, 'color') : null),
    createLayer('shoes-base', state.shoesAssetKey ? resolveShoesAsset(state.shoesAssetKey, 'base') : null),
    createLayer('bottom', state.bottomAssetKey ? resolveBottomAsset(state.bottomAssetKey) : null),
    createLayer('top', state.topAssetKey ? resolveTopAsset(state.topAssetKey) : null),
    createLayer('pupil', resolvePupilAsset(state.pupilAssetKey)),
    createLayer('eyebrow', resolveEyebrowAsset(state.eyebrowAssetKey)),
    createLayer('eyelash', resolveEyelashAsset(state.eyelashAssetKey)),
    createLayer('mouth', resolveMouthAsset(state.mouthAssetKey)),
    createLayer('hair-front', state.hairAssetKey ? resolveHairAsset(state.hairAssetKey, 'front') : null),
    createLayer('bangs', state.hairAssetKey ? resolveHairAsset(state.hairAssetKey, 'bangs') : null),
  ].filter((layer): layer is { id: string; src: string } => layer !== null)

  if (scope === 'face') {
    const hiddenLayerIds = new Set(['shoes-color', 'shoes-base', 'bottom', 'top'])
    return layers.filter((layer) => !hiddenLayerIds.has(layer.id))
  }

  return layers
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

function resolveShoesAsset(assetKey: string, part: 'base' | 'color') {
  const assetNo = parseSimpleAssetKey(assetKey, 'shoes')
  if (part === 'base') {
    return resolveAsset(`shoes/${Number(assetNo)}.png`)
  }

  return resolveAsset(`shoes/COLOR/${Number(assetNo)}.png`)
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
