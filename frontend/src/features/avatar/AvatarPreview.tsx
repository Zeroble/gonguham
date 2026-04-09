import { getAvatarLayers, type AvatarRenderState } from './avatarCatalog'

type AvatarPreviewProps = {
  state: AvatarRenderState
  size?: 'summary' | 'stage' | 'thumb'
  className?: string
}

export function AvatarPreview({
  state,
  size = 'stage',
  className,
}: AvatarPreviewProps) {
  const layers = getAvatarLayers(state)
  const classes = ['avatar-preview', `is-${size}`, className].filter(Boolean).join(' ')

  return (
    <div className={classes}>
      <div className="avatar-preview__backdrop" aria-hidden="true" />
      <div className="avatar-preview__stack" aria-hidden="true">
        {layers.map((layer) => (
          <img
            alt=""
            className={`avatar-preview__layer avatar-preview__layer--${layer.id}`}
            key={layer.id}
            src={layer.src}
          />
        ))}
      </div>
    </div>
  )
}
