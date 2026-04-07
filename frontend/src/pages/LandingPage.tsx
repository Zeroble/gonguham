import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApp } from '../app/useApp'
import landingBackground from '../assets/gonguham/landing-bg.png'
import landingContent from '../assets/gonguham/landing-content.png'

export function LandingPage() {
  const navigate = useNavigate()
  const { loginDemo, isBooting, me } = useApp()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isBooting && me) {
      navigate('/app/home', { replace: true })
    }
  }, [isBooting, me, navigate])

  async function handleLogin() {
    setIsSubmitting(true)
    setError('')

    try {
      await loginDemo()
      navigate('/app/home', { replace: true })
    } catch (loginError) {
      setError(
        loginError instanceof Error
          ? loginError.message
          : '로그인 중 문제가 발생했습니다.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="landing-page">
      <div
        className="landing-page__blur"
        style={{ backgroundImage: `url(${landingBackground})` }}
      />

      <section className="landing-card" aria-label="공구함 로그인">
        <img alt="공구함 랜딩" className="landing-card__art" src={landingContent} />
        <button
          aria-label={me ? '바로 시작하기' : '카카오 로그인'}
          className="landing-card__button"
          disabled={isSubmitting || isBooting}
          onClick={handleLogin}
          type="button"
        >
          <span className="visually-hidden">
            {me ? '바로 시작하기' : '카카오 로그인'}
          </span>
        </button>
      </section>

      {error ? <p className="error-floating">{error}</p> : null}
    </main>
  )
}
