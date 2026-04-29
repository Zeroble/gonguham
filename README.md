# 공구함 (Gonguham)

공구함은 **공부 같이할 사람을 구하고, 스터디 참여와 출석을 꾸준히 기록하는 서비스**입니다.

사용자는 이메일 기반 계정으로 로그인한 뒤 스터디를 만들거나 참여할 수 있고, 회차별 참여 예정 여부와 출석을 관리할 수 있습니다. 출석으로 얻는 `체크`를 사용해 아바타 아이템을 구매하고 프로필을 꾸미는 가벼운 보상 흐름도 함께 제공합니다.

## 주요 기능

- 이메일/비밀번호 기반 회원가입, 로그인, 로그아웃
- 서버 세션과 쿠키 기반 사용자 인증
- 내 스터디 대시보드 및 오늘 예정된 스터디 요약
- 스터디 검색, 필터링, 상세 조회, 즉시 참여
- `TOPIC`, `MOGAKGONG`, `FLASH` 유형의 스터디 생성
- 스터디 회차 자동 생성 및 리더용 회차 수정
- 회차별 참여 예정 등록, 출석 체크, 체크 보상 지급
- 공지/게시글/댓글 작성 및 조회
- 사용자 프로필, 닉네임 수정, 출석/활동 통계 조회
- 아바타 미리보기, 아이템 상점, 구매, 장착, 외형 저장
- PostgreSQL, Flyway, Seeder 기반 로컬 개발 데이터 구성

## 기술 스택

### Backend

| 영역 | 기술 |
| --- | --- |
| Language | Kotlin 2.2.21, Java 21 |
| Framework | Spring Boot 4.0.5 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA, PostgreSQL |
| Migration | Flyway |
| Auth | Spring Security, BCrypt, HTTP Session |
| Test | JUnit 5, Spring Boot Test, Testcontainers |

### Frontend

| 영역 | 기술 |
| --- | --- |
| Language | TypeScript 5 |
| UI | React 19 |
| Build | Vite 5 |
| Routing | React Router 7 |
| Style | SCSS |
| Quality | ESLint |

## 프로젝트 구조

```text
gonguham/
├─ backend/                  # Spring Boot API 서버
│  ├─ src/main/kotlin/com/gonguham/backend/
│  │  ├─ auth/               # 회원가입, 로그인, 세션 사용자
│  │  ├─ avatar/             # 아바타 상점, 구매, 장착, 외형 저장
│  │  ├─ check/              # 체크 적립/사용 기록
│  │  ├─ common/             # 설정, 헬스체크, 시드 데이터, 공통 지원
│  │  ├─ dashboard/          # 내 스터디 대시보드
│  │  ├─ study/              # 스터디, 회차, 출석, 게시글, 댓글
│  │  └─ user/               # 사용자/프로필
│  └─ src/main/resources/db/migration/
├─ frontend/                 # React 클라이언트
│  ├─ src/app/               # API 클라이언트, 라우터, 전역 상태
│  ├─ src/features/          # 아바타, 프로필, 스터디 UI 단위
│  ├─ src/layouts/           # 앱 공통 레이아웃
│  ├─ src/pages/             # 랜딩, 홈, 검색, 생성, 커스터마이징
│  └─ assets/                # 아바타 파츠 및 이미지 리소스
├─ scripts/reset-local-db.ps1
├─ docker-compose.local.yml
└─ LOCAL_DB.md
```

## 실행 환경

- Java 21
- Node.js 20 이상
- npm
- Docker Desktop 또는 Docker Engine

## 빠른 시작

### 1. 로컬 PostgreSQL 실행

저장소 루트에서 PostgreSQL 컨테이너를 실행합니다.

```powershell
docker compose -f docker-compose.local.yml up -d
```

기본 DB 설정은 다음과 같습니다.

| 항목 | 값 |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `gonguham` |
| Username | `gonguham` |
| Password | `gonguham` |

### 2. 백엔드 실행

```powershell
cd backend
.\gradlew.bat bootRun
```

백엔드는 기본적으로 `local` Spring profile로 실행됩니다.

- API 서버: `http://localhost:8080`
- 헬스체크: `GET http://localhost:8080/api/v1/health`

빈 데이터베이스에서 처음 실행하면 Flyway가 스키마를 적용하고 Seeder가 데모 사용자, 스터디, 회차, 게시글, 댓글, 아바타 상점 데이터를 자동으로 생성합니다.

### 3. 프론트엔드 실행

```powershell
cd frontend
npm install
npm run dev
```

- 개발 서버: `http://localhost:5173`
- 기본 API 주소: `http://localhost:8080`

API 주소를 바꾸려면 `frontend/.env`에 다음 값을 추가합니다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 환경 변수

백엔드 DB 접속 정보는 환경 변수로 덮어쓸 수 있습니다.

```powershell
$env:GONGUHAM_DB_URL="jdbc:postgresql://localhost:5432/gonguham"
$env:GONGUHAM_DB_USERNAME="gonguham"
$env:GONGUHAM_DB_PASSWORD="gonguham"
```

예시는 [backend/.env.example](./backend/.env.example)에 있습니다.

## 데모 계정

로컬 DB 시드 데이터의 공통 비밀번호는 `gonguham123!`입니다.

| 이메일 | 설명 |
| --- | --- |
| `leader@gonguham.app` | 리더 역할 데모 계정 |
| `member@gonguham.app` | 멤버 역할 데모 계정 |
| `guest@gonguham.app` | 게스트/신규 사용자 흐름 확인용 |
| `hana@gonguham.app` | 추가 시드 사용자 |
| `minho@gonguham.app` | 추가 시드 사용자 |
| `sora@gonguham.app` | 추가 시드 사용자 |
| `daniel@gonguham.app` | 추가 시드 사용자 |
| `yuna@gonguham.app` | 추가 시드 사용자 |
| `jihun@gonguham.app` | 추가 시드 사용자 |
| `eunchae@gonguham.app` | 추가 시드 사용자 |

## 주요 API

모든 API의 기본 prefix는 `/api/v1`입니다.

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/health` | 서버 상태 확인 |
| `POST` | `/auth/signup` | 회원가입 |
| `POST` | `/auth/login` | 로그인 |
| `POST` | `/auth/logout` | 로그아웃 |
| `GET` | `/me` | 현재 로그인 사용자 조회 |
| `PATCH` | `/me` | 현재 사용자 닉네임 수정 |
| `GET` | `/dashboard` | 내 스터디 대시보드 조회 |
| `GET` | `/dashboard/studies/{studyId}/panel` | 스터디 홈 패널 조회 |
| `GET` | `/studies` | 스터디 목록/검색 |
| `POST` | `/studies` | 스터디 생성 |
| `GET` | `/studies/{studyId}` | 스터디 상세 조회 |
| `POST` | `/studies/{studyId}/join` | 스터디 참여 |
| `POST` | `/studies/{studyId}/leave` | 스터디 나가기 |
| `POST` | `/studies/{studyId}/close` | 스터디 종료 |
| `PATCH` | `/studies/{studyId}/sessions` | 스터디 회차 수정 |
| `PATCH` | `/sessions/{sessionId}/participation` | 회차 참여 예정 여부 변경 |
| `POST` | `/sessions/{sessionId}/attendance` | 출석 체크 |
| `GET` | `/sessions/{sessionId}/attendance-roster` | 출석 대상자 조회 |
| `GET` | `/studies/{studyId}/posts` | 스터디 게시글/공지 조회 |
| `POST` | `/studies/{studyId}/posts` | 스터디 게시글/공지 작성 |
| `GET` | `/posts/{postId}` | 게시글 상세 조회 |
| `POST` | `/posts/{postId}/comments` | 댓글 작성 |
| `GET` | `/avatar` | 아바타 요약 조회 |
| `GET` | `/avatar/shop` | 아바타 상점 조회 |
| `POST` | `/avatar/items/{itemId}/purchase` | 아바타 아이템 구매 |
| `POST` | `/avatar/equip` | 아바타 아이템 장착 |
| `PUT` | `/avatar/appearance` | 아바타 외형 저장 |
| `GET` | `/users/{userId}/profile` | 사용자 프로필 조회 |

프론트엔드 API 클라이언트는 `credentials: 'include'`로 쿠키를 함께 전송합니다. 백엔드는 세션의 `AUTH_USER_ID` 값을 기준으로 현재 사용자를 판단합니다.

## 로컬 DB 초기화

로컬 PostgreSQL 볼륨을 삭제하고 시드 데이터를 다시 만들려면 저장소 루트에서 실행합니다.

```powershell
.\scripts\reset-local-db.ps1
```

이 스크립트는 백엔드 `bootRun` 프로세스를 종료하고, PostgreSQL 컨테이너/볼륨을 재생성한 뒤 백엔드를 다시 실행하고 시드 데이터 요약을 출력합니다.

백엔드 재시작 없이 DB만 초기화하려면 다음 옵션을 사용합니다.

```powershell
.\scripts\reset-local-db.ps1 -NoBackendRestart
```

자세한 내용은 [LOCAL_DB.md](./LOCAL_DB.md)를 참고하세요.

## 검증 명령

### Backend

```powershell
cd backend
.\gradlew.bat test
```

백엔드 테스트는 Testcontainers 기반 PostgreSQL 환경에서 실행됩니다.

### Frontend

```powershell
cd frontend
npm run lint
npm run build
```
