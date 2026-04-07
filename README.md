# 공구함

성공회대 스터디 모집/참여 서비스 `공구함`의 웹 프로젝트입니다.  
현재 저장소는 공모전 데모용 MVP를 기준으로 구성되어 있으며, `Spring Boot (Kotlin)` 백엔드와 `React + TypeScript + SCSS` 프론트엔드로 나뉘어 있습니다.

디자인 기준은 연결된 Figma 시안을 바탕으로 하고 있고, 현재 프론트는 `PC only` 화면을 우선 구현 중입니다.

## 프로젝트 목표

- 빠르게 스터디를 찾고 바로 참여할 수 있는 흐름
- 내 스터디 회차 타임라인에서 참여 여부를 표시하는 흐름
- 스터디장이 회차별 출석을 체크하고 체크를 지급하는 흐름
- 출석으로 모은 체크로 커스터마이징 아이템을 구매/착용하는 흐름

## 현재 구현 범위

### 프론트엔드

- 랜딩 페이지
- 내 스터디 메인 화면
- 스터디 찾기
- 스터디 만들기
- 커스터마이징
- React Router 기반 페이지 전환
- 데모 로그인 세션 관리

### 백엔드

- 데모 로그인
- 내 정보 조회
- 대시보드 조회
- 스터디 목록 / 상세 / 생성 / 즉시 참여
- 회차 참여 여부 업데이트
- 회차 출석 체크
- 게시글 / 공지 작성
- 아바타 상점 / 구매 / 장착
- 체크 누적 및 레벨 계산

## 기술 스택

### Backend

- Kotlin
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring OAuth2 Client
- H2 Database

### Frontend

- React 19
- TypeScript
- Vite 5
- React Router
- SCSS

## 디렉토리 구조

```text
GONGUHAM/
├─ backend/   # Spring Boot API 서버
└─ frontend/  # React 웹 클라이언트
```

백엔드 주요 패키지:

- `auth` : 데모 로그인, 세션 응답
- `user` : 사용자/프로필
- `dashboard` : 메인 화면용 집계 데이터
- `study` : 스터디, 회차, 참여, 출석, 게시글
- `avatar` : 상점, 구매, 장착
- `common` : 시드 데이터, 공통 설정, 현재 사용자 처리

프론트 주요 영역:

- `src/layouts` : 공통 셸
- `src/pages` : 화면 단위 페이지
- `src/app` : API, 라우터, 앱 상태
- `src/styles` : 전역 SCSS
- `src/assets/gonguham` : 디자인 반영용 이미지 자산

## 실행 환경

권장 환경:

- Java 21
- Node.js 20+
- npm

## 로컬 실행

### 1. 백엔드 실행

```powershell
cd C:\Users\dlals\Desktop\GONGUHAM\backend
.\gradlew.bat bootRun
```

기본 포트:

- `http://localhost:8080`

헬스체크:

- `GET /api/v1/health`

H2 콘솔:

- `http://localhost:8080/h2-console`

### 2. 프론트엔드 실행

```powershell
cd C:\Users\dlals\Desktop\GONGUHAM\frontend
npm install
npm run dev
```

기본 포트:

- Vite 기본 개발 서버 포트 사용

프론트는 기본적으로 아래 API 주소를 바라봅니다.

- `http://localhost:8080`

필요하면 `frontend/.env`에 아래 값을 지정할 수 있습니다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 검증 명령어

### Backend

```powershell
cd C:\Users\dlals\Desktop\GONGUHAM\backend
.\gradlew.bat test
```

### Frontend

```powershell
cd C:\Users\dlals\Desktop\GONGUHAM\frontend
npm run build
npm run lint
```

## 인증/데모 동작 방식

현재는 실제 카카오 OAuth 연동 전 단계입니다.

- 랜딩 페이지의 로그인 버튼은 `데모 로그인`으로 동작합니다.
- 백엔드는 `POST /api/v1/auth/demo-login`으로 세션 유저를 만듭니다.
- 프론트는 로그인 후 사용자 ID를 `localStorage`에 저장합니다.
- API는 `X-Demo-User-Id` 헤더를 기준으로 현재 사용자를 식별합니다.
- 헤더가 없으면 기본 사용자 ID `1`을 사용합니다.

즉, 현재 상태는 공모전 시연과 기능 개발을 위한 데모 인증 구조입니다.

## 데이터베이스 및 시드 데이터

현재 DB는 H2 메모리 DB입니다.

- 앱 재시작 시 데이터가 초기화됩니다.
- 시작 시 `Seeder`가 데모용 유저, 스터디, 회차, 게시글, 아바타 아이템을 주입합니다.

데모 흐름 예시:

1. 로그인
2. 스터디 찾기
3. 즉시 참여
4. 내 스터디에서 참여 예정 표시
5. 스터디장이 출석 체크
6. 체크 획득
7. 커스터마이징 아이템 구매/장착

## 현재 제약사항

- 실제 카카오 로그인 미연동
- 실제 운영 DB 미연동
- 반응형보다 PC 화면 우선
- 피그마 기반 화면 정합성은 계속 미세 조정 중

## 다음 작업 후보

- 카카오 OAuth 실연동
- MySQL 또는 PostgreSQL 전환
- 프론트 디자인 정합성 추가 보정
- 관리자 기능 / 알림 기능 확장
- 커스터마이징 실제 캐릭터 렌더링 고도화

## 참고

- 백엔드 진입점: `backend/src/main/kotlin/com/gonguham/backend/BackendApplication.kt`
- 프론트 라우터: `frontend/src/app/router.tsx`
- 메인 화면: `frontend/src/pages/HomePage.tsx`
