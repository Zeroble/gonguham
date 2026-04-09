# 공구함

성공회대 스터디 모집/참여 서비스 `공구함` 저장소입니다.

현재 이 저장소는 공모전 제출용 MVP를 기준으로 계속 개발 중인 모노레포이며, 프론트엔드와 백엔드가 한 저장소 안에 함께 들어 있습니다. 예전 문서에 있던 데모 로그인 중심 설명과 달리, 지금은 이메일/비밀번호 기반 회원가입·로그인, PostgreSQL + Flyway 기반 로컬 DB, 자동 시드 데이터까지 연결된 상태입니다.

## 현재 상태 요약

- 이메일/비밀번호 회원가입, 로그인, 로그아웃
- 서버 세션 + 쿠키 기반 인증
- 스터디 목록 조회, 필터 검색, 상세 조회, 즉시 참여
- 스터디 생성, 회차 자동 생성, 리더의 회차 수정
- 참여 예정 표시, 출석 체크, 체크 보상 지급
- 공지/게시글/댓글 작성 및 조회
- 프로필 모달, 닉네임 수정, 레벨/출석 통계 조회
- 아바타 상점, 인벤토리, 커스터마이징, 체크 소모형 구매
- PostgreSQL + Flyway + Seeder + Testcontainers 기반 로컬/테스트 환경

## 기술 스택

### Backend

- Kotlin 2.2.21
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Bean Validation
- PostgreSQL
- Flyway
- Testcontainers

### Frontend

- React 19
- TypeScript 5
- Vite 5
- React Router 7
- SCSS
- ESLint

## 디렉터리 구조

```text
GONGUHAM/
├─ backend/                  # Spring Boot API 서버
├─ frontend/                 # React 웹 클라이언트
├─ scripts/reset-local-db.ps1
├─ docker-compose.local.yml  # 로컬 PostgreSQL 실행용 compose
└─ LOCAL_DB.md               # 로컬 DB 상세 가이드
```

### 백엔드 주요 패키지

- `auth`: 회원가입, 로그인, 로그아웃, 세션 사용자 조회
- `dashboard`: 내 스터디 대시보드와 홈 패널 데이터
- `study`: 스터디, 회차, 참여 예정, 출석, 게시글/댓글
- `avatar`: 상점, 구매, 장착, 외형 저장
- `user`: 내 정보, 프로필, 닉네임 수정
- `common`: 시드 데이터, 공통 설정, 현재 사용자 처리

### 프론트엔드 주요 영역

- `src/pages`: 랜딩, 홈, 스터디 검색, 스터디 생성, 커스터마이징
- `src/layouts`: 앱 공통 셸과 대시보드 컨텍스트
- `src/features`: 아바타, 프로필, 스터디 상세 UI
- `src/app`: API 클라이언트, 라우터, 앱 전역 상태
- `src/styles`: 전역 SCSS

## 화면/기능 기준 구현 범위

### 랜딩

- 로그인/회원가입 탭 전환
- 인증 성공 시 `/app/home`으로 이동

### 내 스터디

- 오늘 요약과 가입한 스터디 목록
- 스터디별 세션 타임라인
- 회차별 참여 예정 표시
- 공지/게시글/댓글 흐름
- 리더 전용 출석 체크
- 리더 전용 회차 수정
- 스터디 탈퇴 / 스터디 종료

### 스터디 찾기

- 유형, 요일, 시간, 장소 필터
- 키워드 검색
- 스터디 상세 모달
- 즉시 참여 후 대시보드 갱신

### 스터디 만들기

- `TOPIC`, `MOGAKGONG`, `FLASH` 타입 지원
- 일정 범위 기반 회차 자동 생성
- 회차별 제목/타입 수정
- 태그, 규칙, 준비물, 장소 입력

### 커스터마이징 / 프로필

- 아바타 미리보기
- 카테고리별 상점/인벤토리 조회
- 체크로 아이템 구매 후 저장
- 프로필 통계 조회
- 본인 프로필 닉네임 수정

## 실행 환경

- Java 21
- Node.js 20+
- npm
- Docker Desktop 또는 Docker Engine

## 빠른 시작

### 1. 로컬 PostgreSQL 실행

저장소 루트에서:

```powershell
docker compose -f docker-compose.local.yml up -d
```

기본 로컬 DB 설정:

- host: `localhost`
- port: `5432`
- database: `gonguham`
- username: `gonguham`
- password: `gonguham`

다른 DB를 쓰고 싶다면 아래 환경 변수를 덮어쓸 수 있습니다.

```powershell
$env:GONGUHAM_DB_URL="jdbc:postgresql://localhost:5432/gonguham"
$env:GONGUHAM_DB_USERNAME="gonguham"
$env:GONGUHAM_DB_PASSWORD="gonguham"
```

### 2. 백엔드 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

- 기본 Spring profile: `local`
- 기본 주소: `http://localhost:8080`
- 헬스 체크: `GET http://localhost:8080/api/v1/health`

빈 데이터베이스에서 처음 실행하면 Flyway가 스키마를 적용한 뒤 Seeder가 데모 데이터를 자동으로 적재합니다.

### 3. 프론트엔드 실행

```powershell
cd frontend
npm install
npm run dev
```

- 기본 개발 서버: `http://localhost:5173`
- 기본 API 주소: `http://localhost:8080`

필요하면 `frontend/.env`에 아래 값을 둘 수 있습니다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

현재 백엔드 CORS 허용 origin은 `http://localhost:5173`으로 설정되어 있습니다.

## 인증 방식

현재 인증은 서버 세션 기반입니다.

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me`

프론트엔드는 모든 API 요청에 `credentials: 'include'`를 사용해 쿠키를 함께 보내며, 백엔드는 세션의 `AUTH_USER_ID` 값을 기준으로 현재 사용자를 식별합니다.

즉, 예전 README에 적혀 있던 `demo-login`, `localStorage` 사용자 ID 저장, `X-Demo-User-Id` 헤더 방식은 더 이상 현재 코드와 맞지 않습니다.

## 시드 데이터 / 데모 계정

빈 로컬 DB에서 백엔드를 실행하면 아래 데이터가 자동으로 생성됩니다.

- 데모 사용자
- 스터디/회차
- 참여 예정/출석 데이터
- 게시글/댓글
- 아바타 기본 아이템 및 상점 데이터

공통 비밀번호:

- `gonguham123!`

주요 계정:

- `leader@gonguham.app`
- `member@gonguham.app`
- `guest@gonguham.app`
- `hana@gonguham.app`
- `minho@gonguham.app`
- `sora@gonguham.app`
- `daniel@gonguham.app`
- `yuna@gonguham.app`
- `jihun@gonguham.app`
- `eunchae@gonguham.app`

로컬 DB를 초기화하고 시드 데이터를 다시 만들고 싶다면:

```powershell
.\scripts\reset-local-db.ps1
```

백엔드 재시작 없이 DB만 초기화하려면:

```powershell
.\scripts\reset-local-db.ps1 -NoBackendRestart
```

더 자세한 DB 실행/초기화 흐름은 [`LOCAL_DB.md`](./LOCAL_DB.md)를 참고하세요.

## 검증 명령어

### Backend

```powershell
cd backend
.\gradlew.bat test
```

백엔드 테스트는 Testcontainers 기반 PostgreSQL 환경에서 동작하며, 인증, 프로필, 아바타, 스터디 일정/게시글 관련 시나리오를 포함합니다.

### Frontend

```powershell
cd frontend
npm run build
npm run lint
```

## 현재 제약사항

- 소셜 로그인(OAuth)은 아직 연결되어 있지 않습니다.
- 프론트엔드는 현재 데스크톱 화면을 우선 기준으로 구현 중입니다.
- 백엔드 CORS origin이 로컬 개발 주소(`http://localhost:5173`)에 맞춰 고정되어 있습니다.

## 참고 파일

- 백엔드 진입점: `backend/src/main/kotlin/com/gonguham/backend/BackendApplication.kt`
- 프론트 라우터: `frontend/src/app/router.tsx`
- 메인 홈 화면: `frontend/src/pages/HomePage.tsx`
- 로컬 DB 가이드: `LOCAL_DB.md`
