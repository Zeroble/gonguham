import { useEffect, useMemo, useState } from 'react'
import { api, type AvatarShopItem, type AvatarSummary } from '../app/api'
import { AVATAR_CATEGORY_OPTIONS } from '../app/display'
import { useApp } from '../app/useApp'
import { AvatarPreview } from '../features/avatar/AvatarPreview'
import {
  BODY_OPTIONS,
  applyShopItemToDraft,
  buildBodyPreviewState,
  buildItemPreviewState,
  draftToAppearanceRequest,
  draftToRenderState,
  getSelectedItemId,
  isSameDraft,
  summaryToDraft,
  type AvatarDraft,
} from '../features/avatar/avatarCatalog'

type CustomizeCategory = (typeof AVATAR_CATEGORY_OPTIONS)[number]['value']
type ShopCategory = Exclude<CustomizeCategory, 'BODY'>
type PendingPurchaseCart = Partial<Record<ShopCategory, AvatarShopItem>>

const FACE_PREVIEW_CATEGORIES = new Set(['PUPIL', 'EYEBROW', 'EYELASH', 'MOUTH'])
const SHOP_CATEGORIES = AVATAR_CATEGORY_OPTIONS
  .map((option) => option.value)
  .filter((value): value is ShopCategory => value !== 'BODY')
const CATEGORY_LABELS = Object.fromEntries(
  AVATAR_CATEGORY_OPTIONS.map((option) => [option.value, option.label]),
) as Record<CustomizeCategory, string>

async function fetchShopItems(userId: number) {
  const entries = await Promise.all(
    SHOP_CATEGORIES.map(async (shopCategory) => {
      const nextItems = await api.getAvatarShop(userId, shopCategory)
      return [shopCategory, nextItems] as const
    }),
  )

  return entries.reduce<Partial<Record<ShopCategory, AvatarShopItem[]>>>((next, [shopCategory, nextItems]) => {
    next[shopCategory] = nextItems
    return next
  }, {})
}

function toShopCategory(category: string): ShopCategory | null {
  return SHOP_CATEGORIES.find((value) => value === category) ?? null
}

function removePendingPurchase(
  pendingPurchases: PendingPurchaseCart,
  category: ShopCategory,
): PendingPurchaseCart {
  if (!pendingPurchases[category]) {
    return pendingPurchases
  }

  const next = { ...pendingPurchases }
  delete next[category]
  return next
}

function restoreDraftCategory(draft: AvatarDraft, savedDraft: AvatarDraft, category: ShopCategory): AvatarDraft {
  switch (category) {
    case 'HAIR':
      return { ...draft, hair: savedDraft.hair }
    case 'TOP':
      return { ...draft, top: savedDraft.top }
    case 'BOTTOM':
      return { ...draft, bottom: savedDraft.bottom }
    case 'SHOES':
      return { ...draft, shoes: savedDraft.shoes }
    case 'PUPIL':
      return { ...draft, pupil: savedDraft.pupil }
    case 'EYEBROW':
      return { ...draft, eyebrow: savedDraft.eyebrow }
    case 'EYELASH':
      return { ...draft, eyelash: savedDraft.eyelash }
    case 'MOUTH':
      return { ...draft, mouth: savedDraft.mouth }
  }
}

export function CustomizePage() {
  const {
    avatarSummary,
    me,
    refreshMe,
    replaceAvatarSummary,
    sessionUserId,
    showToast,
  } = useApp()
  const [summary, setSummary] = useState<AvatarSummary | null>(avatarSummary)
  const [shopItemsByCategory, setShopItemsByCategory] = useState<Partial<Record<ShopCategory, AvatarShopItem[]>>>({})
  const [category, setCategory] = useState<CustomizeCategory>('BODY')
  const [mode, setMode] = useState<'SHOP' | 'INVENTORY'>('SHOP')
  const [draft, setDraft] = useState<AvatarDraft>(() => summaryToDraft(avatarSummary))
  const [pendingPurchases, setPendingPurchases] = useState<PendingPurchaseCart>({})
  const [isShopLoading, setIsShopLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    setSummary(avatarSummary)
    setDraft(summaryToDraft(avatarSummary))
    setPendingPurchases({})
  }, [avatarSummary])

  useEffect(() => {
    if (!sessionUserId) {
      setShopItemsByCategory({})
      setPendingPurchases({})
      setIsShopLoading(false)
      return
    }

    setShopItemsByCategory({})
    setIsShopLoading(true)
    let cancelled = false

    async function prefetchShop() {
      try {
        const nextItems = await fetchShopItems(sessionUserId)
        if (cancelled) {
          return
        }

        setShopItemsByCategory(nextItems)
      } catch {
        if (!cancelled) {
          setShopItemsByCategory({})
        }
      } finally {
        if (!cancelled) {
          setIsShopLoading(false)
        }
      }
    }

    void prefetchShop()

    return () => {
      cancelled = true
    }
  }, [sessionUserId])

  const savedDraft = useMemo(() => summaryToDraft(summary), [summary])
  const hasChanges = !isSameDraft(draft, savedDraft)
  const currentChecks = me?.currentChecks ?? summary?.currentChecks ?? 0
  const pendingItems = useMemo(
    () => SHOP_CATEGORIES.flatMap((shopCategory) => (pendingPurchases[shopCategory] ? [pendingPurchases[shopCategory]] : [])),
    [pendingPurchases],
  )
  const pendingPriceChecks = useMemo(
    () => pendingItems.reduce((total, item) => total + item.priceChecks, 0),
    [pendingItems],
  )
  const canCheckoutPendingPurchases = pendingPriceChecks <= currentChecks
  const items = useMemo(
    () => (category === 'BODY' ? [] : shopItemsByCategory[category] ?? []),
    [category, shopItemsByCategory],
  )
  const visibleItems = useMemo(
    () => (mode === 'SHOP' ? items : items.filter((item) => item.owned)),
    [items, mode],
  )

  async function reloadAllShop(userId = sessionUserId) {
    if (!userId) {
      return
    }

    setIsShopLoading(true)
    try {
      setShopItemsByCategory(await fetchShopItems(userId))
    } catch {
      showToast('상점 목록을 다시 불러오지 못했어요.')
    } finally {
      setIsShopLoading(false)
    }
  }

  function handlePreviewItem(item: AvatarShopItem) {
    const shopCategory = toShopCategory(item.category)

    if (!shopCategory) {
      return
    }

    if (item.owned) {
      setPendingPurchases((current) => removePendingPurchase(current, shopCategory))
      setDraft((current) => applyShopItemToDraft(current, item))
      return
    }

    const replacedItem = pendingPurchases[shopCategory]
    const nextPendingPrice = pendingPriceChecks - (replacedItem?.priceChecks ?? 0) + item.priceChecks

    if (nextPendingPrice > currentChecks) {
      showToast('체크가 부족해서 이 아이템은 아직 담을 수 없어요.')
      return
    }

    setPendingPurchases((current) => ({
      ...current,
      [shopCategory]: item,
    }))
    setDraft((current) => applyShopItemToDraft(current, item))
  }

  function handleRemovePendingItem(categoryToRemove: ShopCategory) {
    setPendingPurchases((current) => removePendingPurchase(current, categoryToRemove))
    setDraft((current) => restoreDraftCategory(current, savedDraft, categoryToRemove))
  }

  async function handleSave() {
    if (!sessionUserId || !hasChanges || !canCheckoutPendingPurchases) {
      return
    }

    try {
      setIsSaving(true)
      const nextSummary = await api.saveAvatarAppearance(
        sessionUserId,
        draftToAppearanceRequest(draft),
      )
      setSummary(nextSummary)
      setDraft(summaryToDraft(nextSummary))
      setPendingPurchases({})
      replaceAvatarSummary(nextSummary)
      await refreshMe().catch(() => null)
      await reloadAllShop(sessionUserId)
      showToast(
        pendingPriceChecks > 0
          ? `${pendingPriceChecks}체크로 아이템을 구매하고 저장했어요.`
          : '커스터마이징을 저장했어요.',
      )
    } catch (error) {
      showToast(error instanceof Error ? error.message : '커스터마이징 저장에 실패했어요.')
    } finally {
      setIsSaving(false)
    }
  }

  function handleSelectBody(assetKey: string) {
    setDraft((current) => ({ ...current, bodyAssetKey: assetKey }))
  }

  return (
    <section className="customize-layout">
      <article className="page-surface stage-panel">
        <div className="section-card__header">
          <div>
            <h2>미리보기</h2>
          </div>
        </div>

        <div className="stage-canvas__viewport">
          <AvatarPreview className="stage-canvas__preview" size="stage" state={draftToRenderState(draft)} />
        </div>

        <section className="pending-purchase-panel">
          <div className="pending-purchase-panel__header">
            <h3>구매 대기 목록</h3>
            {pendingItems.length ? <span className="count-badge is-pending">총 {pendingPriceChecks}체크</span> : null}
          </div>

          {pendingItems.length ? (
            <ul className="pending-purchase-list">
              {pendingItems.map((item) => {
                const itemCategory = toShopCategory(item.category)
                if (!itemCategory) {
                  return null
                }

                return (
                  <li className="pending-purchase-item" key={`${item.category}-${item.itemId}`}>
                    <div className="pending-purchase-item__meta">
                      <strong>{CATEGORY_LABELS[itemCategory]}</strong>
                      <span>{item.name}</span>
                    </div>

                    <div className="pending-purchase-item__actions">
                      <span>{item.priceChecks}체크</span>
                      <button
                        aria-label={`${item.name} 미리보기 해제`}
                        className="pending-purchase-item__remove"
                        onClick={() => handleRemovePendingItem(itemCategory)}
                        type="button"
                      >
                        ×
                      </button>
                    </div>
                  </li>
                )
              })}
            </ul>
          ) : (
            <div className="empty-inline-state pending-purchase-panel__empty">
              미리보기에 담아둔 구매 예정 아이템이 없어요.
            </div>
          )}
        </section>

        <div className="stage-panel__footer">
          <button
            className="primary-button stage-panel__save"
            disabled={!hasChanges || isSaving || !canCheckoutPendingPurchases}
            onClick={handleSave}
            type="button"
          >
            {isSaving
              ? '저장 중...'
              : pendingPriceChecks > 0
                ? canCheckoutPendingPurchases
                  ? `구매 및 저장 (${pendingPriceChecks}체크)`
                  : `체크 부족 (${pendingPriceChecks}체크)`
                : '저장'}
          </button>
        </div>
      </article>

      <article className="page-surface shop-panel">
        <div className="shop-panel__header">
          <div>
            <h2>커스터마이징 셀렉터</h2>
            <p>피부는 무료로 바꾸고, 나머지 파츠는 먼저 미리보기에 담은 뒤 저장할 때 한 번에 구매돼요.</p>
          </div>

          <div className="shop-summary-pills">
            <span className="count-badge">보유 체크 {currentChecks}</span>
            <button
              className={mode === 'SHOP' ? 'filter-chip is-active' : 'filter-chip'}
              onClick={() => setMode('SHOP')}
              type="button"
            >
              상점
            </button>
            <button
              className={mode === 'INVENTORY' ? 'filter-chip is-active' : 'filter-chip'}
              onClick={() => setMode('INVENTORY')}
              type="button"
            >
              인벤토리
            </button>
          </div>
        </div>

        <div className="shop-toolbar">
          <span className="filter-label">카테고리</span>
          <div className="filter-chip-row">
            {AVATAR_CATEGORY_OPTIONS.map((option) => (
              <button
                className={category === option.value ? 'filter-chip is-active' : 'filter-chip'}
                key={option.value}
                onClick={() => setCategory(option.value)}
                type="button"
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        {category === 'BODY' ? (
          <section className="shop-grid">
            {BODY_OPTIONS.map((option) => {
              const isSelected = draft.bodyAssetKey === option.assetKey

              return (
                <article className="shop-item-card" key={option.assetKey}>
                  <div className="thumb-plate">
                    <AvatarPreview
                      className="shop-item-card__preview"
                      size="thumb"
                      state={buildBodyPreviewState(draft, option.assetKey)}
                    />
                  </div>

                  <div className="shop-item-card__body">
                    <div className="shop-item-card__title-row">
                      <h3>{option.label}</h3>
                      {isSelected ? <span className="inline-state-pill">미리보기 적용중</span> : null}
                    </div>
                    <p>기본 피부 톤은 자유롭게 바꿀 수 있어요.</p>
                  </div>

                  <div className="card-action-row">
                    <button
                      className={isSelected ? 'soft-button is-disabled' : 'primary-button'}
                      disabled={isSelected}
                      onClick={() => handleSelectBody(option.assetKey)}
                      type="button"
                    >
                      {isSelected ? '선택됨' : '선택'}
                    </button>
                  </div>
                </article>
              )
            })}
          </section>
        ) : (
          <>
            <section className="shop-grid">
              {visibleItems.map((item) => {
                const shopCategory = toShopCategory(item.category)
                const isSelected = getSelectedItemId(draft, item.category) === item.itemId
                const pendingItem = shopCategory ? pendingPurchases[shopCategory] : null
                const isPending = pendingItem?.itemId === item.itemId
                const nextPendingPrice = pendingPriceChecks - (pendingItem?.priceChecks ?? 0) + item.priceChecks
                const canPreviewItem = item.owned || nextPendingPrice <= currentChecks

                return (
                  <article className="shop-item-card" key={item.itemId}>
                    <div className="thumb-plate">
                      <AvatarPreview
                        className="shop-item-card__preview"
                        size={FACE_PREVIEW_CATEGORIES.has(item.category) ? 'face-thumb' : 'thumb'}
                        scope={FACE_PREVIEW_CATEGORIES.has(item.category) ? 'face' : 'full'}
                        state={buildItemPreviewState(draft, item)}
                      />
                    </div>

                    <div className="shop-item-card__body">
                      <div className="shop-item-card__title-row">
                        <h3>{item.name}</h3>
                        {!item.owned && isPending ? (
                          <span className="inline-state-pill">구매 대기</span>
                        ) : null}
                        {item.owned && isSelected ? (
                          <span className="inline-state-pill">
                            {item.equipped ? '착용중' : '미리보기 적용중'}
                          </span>
                        ) : null}
                      </div>
                    </div>

                    <div className="card-action-row">
                      {item.owned ? (
                        <button
                          className={isSelected ? 'soft-button is-disabled' : 'primary-button'}
                          disabled={isSelected}
                          onClick={() => handlePreviewItem(item)}
                          type="button"
                        >
                          {isSelected ? '선택됨' : '적용'}
                        </button>
                      ) : (
                        <button
                          className={canPreviewItem && !isPending ? 'primary-button' : 'soft-button is-disabled'}
                          disabled={!canPreviewItem || isPending}
                          onClick={() => handlePreviewItem(item)}
                          type="button"
                        >
                          {isPending
                            ? '장바구니 담김'
                            : canPreviewItem
                              ? `미리보기 · ${item.priceChecks}체크`
                              : `${item.priceChecks}체크 필요`}
                        </button>
                      )}
                    </div>
                  </article>
                )
              })}
            </section>

            {!isShopLoading && !visibleItems.length ? (
              <div className="empty-inline-state">
                {mode === 'INVENTORY'
                  ? '이 카테고리에는 아직 보유한 아이템이 없어요.'
                  : '지금 보여줄 아이템이 없어요.'}
              </div>
            ) : null}

            {isShopLoading ? (
              <div className="empty-inline-state">아이템을 불러오는 중이에요.</div>
            ) : null}
          </>
        )}
      </article>
    </section>
  )
}
