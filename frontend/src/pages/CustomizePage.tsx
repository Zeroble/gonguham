import { useEffect, useMemo, useState } from 'react'
import { api, type AvatarShopItem, type AvatarSummary } from '../app/api'
import { AVATAR_CATEGORY_OPTIONS, getRarityLabel } from '../app/display'
import { useApp } from '../app/useApp'
import { AvatarPreview } from '../features/avatar/AvatarPreview'
import {
  BODY_OPTIONS,
  EYEBROW_OPTIONS,
  EYELASH_OPTIONS,
  MOUTH_OPTIONS,
  PUPIL_OPTIONS,
  applyShopItemToDraft,
  buildItemPreviewState,
  draftToAppearanceRequest,
  draftToRenderState,
  isSameDraft,
  summaryToDraft,
  type AvatarDraft,
} from '../features/avatar/avatarCatalog'

type FreeAppearanceField =
  | 'bodyAssetKey'
  | 'pupilAssetKey'
  | 'eyebrowAssetKey'
  | 'eyelashAssetKey'
  | 'mouthAssetKey'

const FREE_EDITOR_SECTIONS: Array<{
  key: FreeAppearanceField
  label: string
  options: Array<{ assetKey: string; label: string }>
}> = [
  { key: 'bodyAssetKey', label: '스킨톤', options: BODY_OPTIONS },
  { key: 'pupilAssetKey', label: '눈', options: PUPIL_OPTIONS },
  { key: 'eyebrowAssetKey', label: '눈썹', options: EYEBROW_OPTIONS },
  { key: 'eyelashAssetKey', label: '속눈썹', options: EYELASH_OPTIONS },
  { key: 'mouthAssetKey', label: '입', options: MOUTH_OPTIONS },
]

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
  const [items, setItems] = useState<AvatarShopItem[]>([])
  const [category, setCategory] = useState('HAIR')
  const [mode, setMode] = useState<'SHOP' | 'INVENTORY'>('SHOP')
  const [draft, setDraft] = useState<AvatarDraft>(() => summaryToDraft(avatarSummary))
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    setSummary(avatarSummary)
    setDraft(summaryToDraft(avatarSummary))
  }, [avatarSummary])

  useEffect(() => {
    if (!sessionUserId) {
      return
    }

    let cancelled = false

    async function fetchItems() {
      const nextItems = await api.getAvatarShop(sessionUserId, category)
      if (!cancelled) {
        setItems(nextItems)
      }
    }

    void fetchItems()

    return () => {
      cancelled = true
    }
  }, [category, sessionUserId])

  const savedDraft = useMemo(() => summaryToDraft(summary), [summary])
  const visibleItems = useMemo(
    () => (mode === 'SHOP' ? items : items.filter((item) => item.owned)),
    [items, mode],
  )
  const hasChanges = !isSameDraft(draft, savedDraft)
  const currentChecks = me?.currentChecks ?? summary?.currentChecks ?? 0

  async function reloadShop(nextCategory = category) {
    if (!sessionUserId) {
      return
    }

    setItems(await api.getAvatarShop(sessionUserId, nextCategory))
  }

  async function handlePurchase(item: AvatarShopItem) {
    if (!sessionUserId) {
      return
    }

    if (currentChecks < item.priceChecks) {
      showToast('체크가 부족해서 아직 구매할 수 없어요.')
      return
    }

    try {
      await api.purchaseAvatarItem(sessionUserId, item.itemId)
      const nextUser = await refreshMe()
      if (nextUser) {
        setSummary((current) =>
          current
            ? {
                ...current,
                currentChecks: nextUser.currentChecks,
                totalEarnedChecks: nextUser.totalEarnedChecks,
                level: nextUser.level,
              }
            : current,
        )
      }
      await reloadShop()
      showToast('아이템을 구매했어요. 저장 전에 미리 골라둘 수 있어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '아이템 구매에 실패했어요.')
    }
  }

  async function handleSave() {
    if (!sessionUserId || !hasChanges) {
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
      replaceAvatarSummary(nextSummary)
      await reloadShop()
      showToast('커스터마이징을 저장했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '커스터마이징 저장에 실패했어요.')
    } finally {
      setIsSaving(false)
    }
  }

  function handleReset() {
    setDraft(savedDraft)
    showToast('저장된 모습으로 되돌렸어요.')
  }

  function handleSelectItem(item: AvatarShopItem) {
    setDraft((current) => applyShopItemToDraft(current, item))
  }

  function handleFreeAppearanceChange(field: FreeAppearanceField, assetKey: string) {
    setDraft((current) => ({ ...current, [field]: assetKey }))
  }

  return (
    <section className="customize-layout">
      <article className="page-surface stage-panel">
        <div className="section-card__header">
          <div>
            <h2>캐릭터 스테이지</h2>
            <p>얼굴 파츠와 스킨톤은 자유롭게 조합하고, 헤어와 옷은 골라둔 뒤 저장해요.</p>
          </div>
          <span className={hasChanges ? 'count-badge is-pending' : 'count-badge'}>
            {hasChanges ? '저장 전 미리보기' : '저장 완료'}
          </span>
        </div>

        <div className="stage-canvas">
          <span className="stage-canvas__label">{hasChanges ? '미리보기 캐릭터' : '현재 캐릭터'}</span>
          <div className="stage-canvas__viewport">
            <AvatarPreview className="stage-canvas__preview" size="stage" state={draftToRenderState(draft)} />
          </div>
        </div>

        <div className="editor-action-row">
          <button
            className="soft-button"
            disabled={!hasChanges || isSaving}
            onClick={handleReset}
            type="button"
          >
            취소
          </button>
          <button
            className="primary-button"
            disabled={!hasChanges || isSaving}
            onClick={handleSave}
            type="button"
          >
            {isSaving ? '저장 중...' : '저장'}
          </button>
        </div>

        <article className="equipped-panel">
          <div className="equipped-panel__header">
            <span className="section-kicker">현재 선택 슬롯</span>
            <span className="filter-label">저장 시 아래 선택이 반영돼요.</span>
          </div>

          <div className="equipped-slot-list">
            <div className="equipped-slot">
              <span>헤어</span>
              <strong>{draft.hair?.name ?? '선택 안 함'}</strong>
            </div>
            <div className="equipped-slot">
              <span>상의</span>
              <strong>{draft.top?.name ?? '선택 안 함'}</strong>
            </div>
            <div className="equipped-slot">
              <span>하의</span>
              <strong>{draft.bottom?.name ?? '선택 안 함'}</strong>
            </div>
          </div>
        </article>

        <article className="free-editor-panel">
          <div className="free-editor-panel__header">
            <div>
              <span className="section-kicker">무료 편집</span>
              <h3>얼굴과 스킨톤을 조합해 보세요.</h3>
            </div>
          </div>

          <div className="free-editor-sections">
            {FREE_EDITOR_SECTIONS.map((section) => (
              <section className="free-editor-section" key={section.key}>
                <div className="free-editor-section__header">
                  <strong>{section.label}</strong>
                  <span className="filter-label">
                    {
                      section.options.find((option) => option.assetKey === draft[section.key])?.label
                    }
                  </span>
                </div>

                <div className="free-option-grid">
                  {section.options.map((option) => (
                    <button
                      className={
                        draft[section.key] === option.assetKey
                          ? 'free-option-button is-active'
                          : 'free-option-button'
                      }
                      key={option.assetKey}
                      onClick={() => handleFreeAppearanceChange(section.key, option.assetKey)}
                      type="button"
                    >
                      {option.label}
                    </button>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </article>
      </article>

      <article className="page-surface shop-panel">
        <div className="shop-panel__header">
          <div>
            <h2>아이템 상점</h2>
            <p>보유 아이템은 먼저 골라 두고, 원하는 조합이 되면 한 번에 저장할 수 있어요.</p>
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

        <section className="shop-grid">
          {visibleItems.map((item) => {
            const currentSelection =
              item.category === 'HAIR'
                ? draft.hair?.itemId
                : item.category === 'TOP'
                  ? draft.top?.itemId
                  : draft.bottom?.itemId
            const isSelected = currentSelection === item.itemId
            const isAffordable = currentChecks >= item.priceChecks

            return (
              <article className="shop-item-card" key={item.itemId}>
                <div className="shop-item-card__top">
                  <span className={`rarity-badge is-${item.rarity.toLowerCase()}`}>
                    {getRarityLabel(item.rarity)}
                  </span>
                  <span className="count-badge">{item.priceChecks}체크</span>
                </div>

                <div className="thumb-plate">
                  <AvatarPreview
                    className="shop-item-card__preview"
                    size="thumb"
                    state={buildItemPreviewState(draft, item)}
                  />
                </div>

                <div className="shop-item-card__body">
                  <div className="shop-item-card__title-row">
                    <h3>{item.name}</h3>
                    {item.owned && isSelected && !item.equipped ? (
                      <span className="inline-state-pill">저장 대기</span>
                    ) : null}
                  </div>
                  <p>{item.description}</p>
                </div>

                <div className="card-action-row">
                  {item.owned ? (
                    <button
                      className={isSelected ? 'soft-button is-disabled' : 'primary-button'}
                      disabled={isSelected}
                      onClick={() => handleSelectItem(item)}
                      type="button"
                    >
                      {isSelected ? (item.equipped ? '착용중' : '선택됨') : '선택'}
                    </button>
                  ) : (
                    <button
                      className={isAffordable ? 'primary-button' : 'soft-button is-disabled'}
                      disabled={!isAffordable}
                      onClick={() => handlePurchase(item)}
                      type="button"
                    >
                      {isAffordable ? '구매' : '체크 부족'}
                    </button>
                  )}
                </div>
              </article>
            )
          })}
        </section>

        {!visibleItems.length ? (
          <div className="empty-inline-state">
            {mode === 'INVENTORY'
              ? '이 카테고리에는 아직 보유한 아이템이 없어요.'
              : '지금 보여줄 아이템이 없어요.'}
          </div>
        ) : null}
      </article>
    </section>
  )
}
