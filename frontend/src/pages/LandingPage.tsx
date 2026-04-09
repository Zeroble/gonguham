import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApp } from '../app/useApp'
import landingBackground from '../assets/gonguham/landing-bg.png'

type AuthMode = 'login' | 'signup'

const INITIAL_FORM = {
  email: '',
  password: '',
  nickname: '',
  passwordConfirm: '',
}

export function LandingPage() {
  const navigate = useNavigate()
  const { login, signUp, isBooting, me } = useApp()
  const [mode, setMode] = useState<AuthMode>('login')
  const [email, setEmail] = useState(INITIAL_FORM.email)
  const [password, setPassword] = useState(INITIAL_FORM.password)
  const [nickname, setNickname] = useState(INITIAL_FORM.nickname)
  const [passwordConfirm, setPasswordConfirm] = useState(INITIAL_FORM.passwordConfirm)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isBooting && me) {
      navigate('/app/home', { replace: true })
    }
  }, [isBooting, me, navigate])

  function resetError() {
    if (error) {
      setError('')
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setError('')

    try {
      if (mode === 'signup') {
        if (password !== passwordConfirm) {
          setError('비밀번호가 서로 일치하지 않아요.')
          return
        }

        await signUp({
          email,
          password,
          nickname,
        })
      } else {
        await login({
          email,
          password,
        })
      }

      navigate('/app/home', { replace: true })
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : '로그인 처리 중 문제가 발생했어요.',
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

      <section className="landing-auth-shell" aria-label="공부함 로그인 및 회원가입">
        <article className="landing-card landing-card--auth-hero">
          <div className="landing-hero">
            <div className="landing-hero__copy">
              <div className="landing-card__copy">
                <span className="section-kicker">Study Together</span>
                <h1 style={{ fontSize: "2em" }}>스터디를 찾고, 참여하고, 꾸준함을 기록하세요.</h1>
                <p>
                  혼자서는 흐트러지기 쉬운 공부도 함께라면 더 오래 이어갈 수 있어요.
                  <br />
                  관심 있는 스터디를 발견하고, 참여하고, 매일의 꾸준함을 차곡차곡
                  남겨보세요.
                </p>
              </div>
            </div>
          </div>

          <div className="landing-auth-panel">
            <div className="landing-auth__tabs" role="tablist" aria-label="인증 방식">
              <button
                aria-selected={mode === 'login'}
                className={mode === 'login' ? 'summary-tab is-active' : 'summary-tab'}
                onClick={() => {
                  setMode('login')
                  resetError()
                }}
                role="tab"
                type="button"
              >
                로그인
              </button>
              <button
                aria-selected={mode === 'signup'}
                className={mode === 'signup' ? 'summary-tab is-active' : 'summary-tab'}
                onClick={() => {
                  setMode('signup')
                  resetError()
                }}
                role="tab"
                type="button"
              >
                회원가입
              </button>
            </div>

            <div className="landing-auth__header">
              <h2>{mode === 'login' ? '이메일로 로그인' : '새 계정 만들기'}</h2>
              <p>
                {mode === 'login'
                  ? '기존 계정으로 바로 들어가서 내 스터디와 출석 흐름을 이어보세요.'
                  : '닉네임과 이메일만 입력하면 바로 기본 프로필과 함께 시작할 수 있어요.'}
              </p>
            </div>

            <form className="landing-auth__form" onSubmit={handleSubmit}>
              <label className="landing-auth__field">
                <span className="field-label">이메일</span>
                <input
                  autoComplete={mode === 'login' ? 'username' : 'email'}
                  className="field-control"
                  onChange={(event) => {
                    resetError()
                    setEmail(event.target.value)
                  }}
                  placeholder="name@example.com"
                  required
                  type="email"
                  value={email}
                />
              </label>

              {mode === 'signup' ? (
                <label className="landing-auth__field">
                  <span className="field-label">닉네임</span>
                  <input
                    autoComplete="nickname"
                    className="field-control"
                    maxLength={20}
                    onChange={(event) => {
                      resetError()
                      setNickname(event.target.value)
                    }}
                    placeholder="스터디에서 보여질 이름"
                    required
                    type="text"
                    value={nickname}
                  />
                </label>
              ) : null}

              <label className="landing-auth__field">
                <span className="field-label">비밀번호</span>
                <input
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  className="field-control"
                  minLength={8}
                  onChange={(event) => {
                    resetError()
                    setPassword(event.target.value)
                  }}
                  placeholder="8자 이상 입력"
                  required
                  type="password"
                  value={password}
                />
              </label>

              {mode === 'signup' ? (
                <label className="landing-auth__field">
                  <span className="field-label">비밀번호 확인</span>
                  <input
                    autoComplete="new-password"
                    className="field-control"
                    minLength={8}
                    onChange={(event) => {
                      resetError()
                      setPasswordConfirm(event.target.value)
                    }}
                    placeholder="비밀번호를 다시 입력"
                    required
                    type="password"
                    value={passwordConfirm}
                  />
                </label>
              ) : null}

              {error ? <p className="error-floating landing-auth__error">{error}</p> : null}

              <button
                className="primary-button landing-auth__submit"
                disabled={isSubmitting || isBooting}
                type="submit"
              >
                {isSubmitting
                  ? mode === 'login'
                    ? '로그인 중...'
                    : '가입 중...'
                  : mode === 'login'
                    ? '로그인'
                    : '회원가입하고 시작하기'}
              </button>
            </form>
          </div>
        </article>
      </section>
    </main >
  )
}
