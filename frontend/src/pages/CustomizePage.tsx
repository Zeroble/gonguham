import { useEffect, useMemo, useState } from 'react'
import { api, type AvatarShopItem, type AvatarSummary } from '../app/api'
import { AVATAR_CATEGORY_OPTIONS, getRarityLabel } from '../app/display'
import { useApp } from '../app/useApp'
import thumbBangs from '../assets/gonguham/thumb-bangs.png'
import thumbCloud from '../assets/gonguham/thumb-cloud.png'
import thumbPony from '../assets/gonguham/thumb-pony.png'

const thumbs = [thumbCloud, thumbPony, thumbBangs]

export function CustomizePage() {
  const { sessionUserId, refreshMe, showToast } = useApp()
  const [summary, setSummary] = useState<AvatarSummary | null>(null)
  const [items, setItems] = useState<AvatarShopItem[]>([])
  const [category, setCategory] = useState('HAIR')
  const [mode, setMode] = useState<'SHOP' | 'INVENTORY'>('SHOP')

  useEffect(() => {
    if (!sessionUserId) {
      return
    }

    let cancelled = false

    async function fetchData() {
      const [nextSummary, nextItems] = await Promise.all([
        api.getAvatarSummary(sessionUserId),
        api.getAvatarShop(sessionUserId, category),
      ])

      if (!cancelled) {
        setSummary(nextSummary)
        setItems(nextItems)
      }
    }

    void fetchData()

    return () => {
      cancelled = true
    }
  }, [category, sessionUserId])

  const visibleItems = useMemo(
    () => (mode === 'SHOP' ? items : items.filter((item) => item.owned)),
    [items, mode],
  )

  async function reload(nextCategory = category) {
    if (!sessionUserId) {
      return
    }

    const [nextSummary, nextItems] = await Promise.all([
      api.getAvatarSummary(sessionUserId),
      api.getAvatarShop(sessionUserId, nextCategory),
    ])
    setSummary(nextSummary)
    setItems(nextItems)
  }

  async function handlePurchase(itemId: number) {
    if (!sessionUserId) {
      return
    }

    try {
      await api.purchaseAvatarItem(sessionUserId, itemId)
      await refreshMe()
      await reload()
      showToast('아이템을 구매했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '아이템 구매에 실패했어요.')
    }
  }

  async function handleEquip(itemId: number) {
    if (!sessionUserId) {
      return
    }

    try {
      setSummary(await api.equipAvatarItem(sessionUserId, itemId))
      setItems(await api.getAvatarShop(sessionUserId, category))
      showToast('아이템을 착용했어요.')
    } catch (error) {
      showToast(error instanceof Error ? error.message : '아이템 착용에 실패했어요.')
    }
  }

  return (
    <section className="customize-layout">
      <article className="page-surface stage-panel">
        <div className="section-card__header">
          <div>
            <h2>캐릭터 스테이지</h2>
            <p>지금은 헤어, 상의, 하의 3개 슬롯만 우선 구현해 두었어요.</p>
          </div>
        </div>

        <div className="stage-canvas">
          <span className="stage-canvas__label">현재 캐릭터</span>
          <div className="stage-placeholder" />
        </div>

        <article className="equipped-panel">
          <span className="section-kicker">현재 착용 슬롯</span>
          <div className="equipped-slot-list">
            <div className="equipped-slot">
              <span>헤어</span>
              <strong>{summary?.equipped.hair?.name ?? '기본 헤어'}</strong>
            </div>
            <div className="equipped-slot">
              <span>상의</span>
              <strong>{summary?.equipped.top?.name ?? '기본 상의'}</strong>
            </div>
            <div className="equipped-slot">
              <span>하의</span>
              <strong>{summary?.equipped.bottom?.name ?? '기본 하의'}</strong>
            </div>
          </div>
        </article>
      </article>

      <article className="page-surface shop-panel">
        <div className="shop-panel__header">
          <div>
            <h2>아이템 상점</h2>
            <p>카테고리를 바꿔 가며 상점과 인벤토리를 같은 판 안에서 보이게 했어요.</p>
          </div>

          <div className="shop-summary-pills">
            <span className="count-badge">보유 체크 {summary?.currentChecks ?? 0}</span>
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
                className={
                  category === option.value ? 'filter-chip is-active' : 'filter-chip'
                }
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
          {visibleItems.map((item, index) => (
            <article className="shop-item-card" key={item.itemId}>
              <div className="shop-item-card__top">
                <span className={`rarity-badge is-${item.rarity.toLowerCase()}`}>
                  {getRarityLabel(item.rarity)}
                </span>
                <span className="count-badge">{item.priceChecks}체크</span>
              </div>

              <div className="thumb-plate">
                <img alt={item.name} src={thumbs[index % thumbs.length]} />
              </div>

              <h3>{item.name}</h3>
              <p>{item.description}</p>

              <div className="card-action-row">
                {item.owned ? (
                  <button
                    className={item.equipped ? 'soft-button is-disabled' : 'primary-button'}
                    disabled={item.equipped}
                    onClick={() => handleEquip(item.itemId)}
                    type="button"
                  >
                    {item.equipped ? '착용중' : '착용'}
                  </button>
                ) : (
                  <button
                    className="primary-button"
                    onClick={() => handlePurchase(item.itemId)}
                    type="button"
                  >
                    구매
                  </button>
                )}
              </div>
            </article>
          ))}
        </section>

        {!visibleItems.length ? (
          <div className="empty-inline-state">이 카테고리에는 아직 보유한 아이템이 없어요.</div>
        ) : null}
      </article>
    </section>
  )
}
