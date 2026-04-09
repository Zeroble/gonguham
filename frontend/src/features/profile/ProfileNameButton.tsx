import { useApp } from '../../app/useApp'

type ProfileNameButtonProps = {
  userId: number
  nickname: string
  className?: string
}

export function ProfileNameButton({
  userId,
  nickname,
  className,
}: ProfileNameButtonProps) {
  const { me, openProfile } = useApp()
  const displayName = me?.id === userId ? me.nickname : nickname
  const classes = ['profile-name-button', className].filter(Boolean).join(' ')

  return (
    <button
      className={classes}
      onClick={(event) => {
        event.stopPropagation()
        openProfile(userId)
      }}
      onKeyDown={(event) => {
        event.stopPropagation()
      }}
      type="button"
    >
      {displayName}
    </button>
  )
}
