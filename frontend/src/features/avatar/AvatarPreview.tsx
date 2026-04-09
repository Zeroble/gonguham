import { getAvatarLayers, type AvatarRenderState } from './avatarCatalog'

type AvatarPreviewProps = {
  state: AvatarRenderState
  size?: 'summary' | 'profile' | 'stage' | 'thumb' | 'face-thumb'
  scope?: 'full' | 'face'
  className?: string
}

export function AvatarPreview({
  state,
  size = 'stage',
  scope = 'full',
  className,
}: AvatarPreviewProps) {
  const layers = getAvatarLayers(state, scope)
  const classes = ['avatar-preview', `is-${size}`, className].filter(Boolean).join(' ')
  const isCropped = size === 'profile' || size === 'face-thumb'

  return (
    <div className={classes}>
      {isCropped ? (
        <div className="avatar-preview__crop">
          <div className="avatar-preview__scene">
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
        </div>
      ) : (
        <>
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
        </>
      )}
    </div>
  )
}
