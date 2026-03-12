# BulSee (불씨)

**기상청·산림청 실시간 공공데이터를 수집하여 AI 모델로 산불 발생 위험도와 피해 범위를 예측하는 웹 서비스입니다.**

지역별 산불 위험도를 예측하고, AI 기반 시뮬레이션과 대시보드·챗봇으로 상황을 한눈에 파악할 수 있는 풀스택 웹 애플리케이션입니다.

---

## 프로젝트 개요

- **데이터 소스**: 기상청·산림청 실시간 공공데이터 수집
- **핵심 가치**: 수집 데이터를 활용한 AI 기반 **산불 발생 위험도** 및 **피해 범위** 예측
- **구성**: 안정적인 DB 서버, Spring Boot + FastAPI 백엔드 파이프라인, 데이터 전처리·스케일링, 다양한 ML/DL 모델 학습·성능 비교

---

## 주요 기능 (시스템)

- **지도 기반 지역 조회** — 전국 시·군·구 단위 지역 선택 및 위험도 시각화
- **산불 위험 예측** — 기상(기온, 풍속, 습도, 강수 등) 및 AI 모델 기반 위험도·위험 점수 산출
- **시뮬레이션** — 선택 지역에 대한 확산 시뮬레이션 및 시간대별 예상 피해 면적 타임라인
- **대시보드** — 지역별 위험 등급, 요약 통계, 타임라인 뷰
- **챗봇** — 산불·예측 관련 질의응답 (FAQ + OpenAI)

---

## 백엔드·데이터 파이프라인 요약

1. **안정적인 데이터베이스 서버 구축**  
   Ubuntu Docker 환경에 Oracle DB를 구축하여 공공데이터 및 예측 결과를 저장·관리합니다.

2. **백엔드 파이프라인 설계**  
   Spring Boot와 FastAPI를 연동해 기상청 API 데이터를 수신하고, AI 모델에 전달하는 파이프라인을 구성했습니다.

3. **데이터 클렌징·전처리·스케일링**  
   산불 정보, 날씨 데이터, 임상도(산림 지형), 지표피복(토지 피복) 등 **비정형·미정제 공공데이터**의 클렌징, 전처리, 스케일링을 수행합니다.

4. **다양한 ML/DL 모델 학습 및 성능 비교**  
   **머신러닝**: Random Forest, XGBoost  
   **딥러닝**: GRU(Gated Recurrent Unit), Transformer Encoder  
   위 모델들을 학습·비교하여 산불 위험도 및 피해 예측에 활용합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **프론트엔드** | React 19, TypeScript, Vite, Leaflet, Tailwind CSS |
| **백엔드** | Spring Boot 3.5, Java 17, JPA, WebFlux, Oracle DB |
| **AI 서버** | FastAPI, PyTorch, scikit-learn, XGBoost, GRU/Transformer 계열 모델, 기상 API 연동 |
| **인프라** | Docker, Docker Compose (Ubuntu 등) |

**키워드**: `Spring Boot` · `FastAPI` · `Oracle` · `Docker` · `GRU/Transformer` · `RF/XGBoost`

---

## 프로젝트 구조

```
bulsee-final-main/
├── bulsee_frontend/     # React + Vite 프론트엔드
├── bulsee_backend/      # Spring Boot 백엔드
│   └── ai-server/       # FastAPI 산불 예측·기상 API 서버
├── requirements.txt    # 프로젝트 전체 Python 의존성 (한 번에 설치)
├── docker-compose.yml   # DB, AI, Backend, Frontend 통합 실행
└── README.md
```

---

## 사전 요구 사항

- **Docker**, **Docker Compose** (권장: 전체를 Docker로 실행할 경우)
- 또는 로컬 개발용:
  - **Node.js** 18+ (프론트엔드)
  - **JDK 17** (백엔드)
  - **Python 3.10+** (AI 서버)
  - **Oracle DB** (Express 등)

---

## Docker로 한 번에 실행 (권장)

1. 저장소 클론 후 프로젝트 루트로 이동:

   ```bash
   cd bulsee-final-main
   ```

2. Docker Compose로 모든 서비스 기동:

   ```bash
   docker-compose up -d
   ```

3. 접속 주소:
   - **프론트엔드**: http://localhost:5173
   - **백엔드 API**: http://localhost:8080
   - **AI 서버**: http://localhost:8000
   - **Oracle DB**: `localhost:1522` (SID: xe)

4. Oracle 초기 비밀번호는 `docker-compose.yml`의 `ORACLE_PWD`(기본 `1234`)이며, 백엔드 DB 사용자(`c##fire` / `1234`)는 DB 생성 후 스키마/데이터 설정이 필요할 수 있습니다.

---

## 로컬에서 개별 실행

### 1. Oracle DB

- Docker만 사용할 경우: `docker-compose up -d db`
- 또는 로컬에 설치한 Oracle에서 `c##fire` 사용자 및 스키마를 준비합니다.

### 2. AI 서버 (FastAPI)

**Python 의존성**: 루트의 `requirements.txt`로 전체 한 번에 설치하거나, AI 서버만 쓸 때는 `bulsee_backend/ai-server/requirements.txt` 사용 (FastAPI, PyTorch, scikit-learn, XGBoost 등).

```bash
# 방법 1: 루트에서 전체 의존성 설치 후 AI 서버만 실행
pip install -r requirements.txt
cd bulsee_backend/ai-server
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 방법 2: AI 서버 폴더에서만 설치·실행
cd bulsee_backend/ai-server
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 3. 백엔드 (Spring Boot)

- `application.properties`(또는 `.yml`)에 DB URL/계정을 로컬/Oracle에 맞게 설정합니다.
- AI 서버 URL이 로컬이면 `PYTHON_API_URL=http://localhost:8000` 등으로 설정합니다.

```bash
cd bulsee_backend
./gradlew bootRun
```

### 4. 프론트엔드 (Vite)

```bash
cd bulsee_frontend
npm install
npm run dev
```

브라우저에서 http://localhost:5173 으로 접속합니다.

---

## 환경 변수 (참고)

| 변수 | 설명 | 예시 |
|------|------|------|
| `SPRING_DATASOURCE_URL` | Oracle JDBC URL | `jdbc:oracle:thin:@localhost:1522:xe` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 | `c##fire` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `1234` |
| `PYTHON_API_URL` | AI 서버 주소 | `http://localhost:8000` |
| `VITE_API_URL` | 백엔드 API 주소 (프론트 빌드 시) | `http://localhost:8080` |

---

## 주요 API 엔드포인트 (백엔드)

- `GET /api/regions` — 지역 목록
- `POST /api/analysis/simulate` — 산불 시뮬레이션 실행 (위도/경도, 지역명 등)
- 챗봇·FAQ 등 추가 API는 `bulsee_backend` 컨트롤러를 참고하세요.

---

## 라이선스

이 프로젝트의 라이선스는 저장소 설정을 확인해 주세요.
