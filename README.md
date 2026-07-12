# saju

사주 계산 엔진 — 생년월일시로 사주 팔자(四柱八字)를 계산하고 파생 분석을 제공하는 Kotlin/Spring Boot 백엔드.

## 주요 기능

- **원국 계산**: 절기 기준 연·월·일·시주 (1900~2100년)
- **파생 분석**: 십성, 격국·용신, 오행 강약, 신살, 12운성·12신살, 합충형파해
- **운세 조회**: 대운 타임라인, 세운·월운, 원국과의 관계 분석, 삼재
- **LLM 해석**: 원국 풀이·대운 풀이·연도별 운세 해석문 3종 (Claude API + MariaDB 영구 캐싱)
- **입력 옵션**: 양력/음력(윤달), 해외 출생(IANA 시간대), 지방평균시/진태양시 보정, 자시 처리 방식
- **정밀도**: 절기 시각 평균 ±5분 (KASI 공식값 268건 대조 검증, ΔT 보정 적용)

## 아키텍처

```
com.saju
├── domain/core    불변 상수·순수 규칙 (의존성 없음)
│   ├── CheonGan, JiJi, SixtyGapja      천간·지지·60갑자
│   ├── Jeolgi                          24절기 (태양 황경·월지 매핑)
│   ├── ElementRelation, Gan/JiRelation 오행 상생상극, 합충형파해 테이블
│   └── SipSeong, UnSeong, TwelveSinSal 십성·12운성·12신살 판정 공식
│
├── engine         시각 계산 (domain/core에만 의존)
│   ├── BirthInputNormalizer            검증 → 음력 변환 → 시간 보정
│   ├── LunarConverter                  음↔양력 (비트마스크 테이블, <1KB)
│   ├── TimeCorrector                   시간대·경도 보정 (IANA tzdata)
│   ├── JeolgiCalculator + DeltaT       절기 시각 (Meeus 알고리즘 + ΔT)
│   ├── Year/Month/Day/HourPillarCalculator  4주 계산기
│   ├── DaeunStartCalculator, DaeunCalculator 대운 기산점·배열
│   └── SajuCalculator                  통합 파이프라인 (진입점)
│
├── analysis       파생 해석 (SajuResult 입력)
│   ├── SipSeongAnalyzer                십성 맵 (장간 포함)
│   ├── GyeokGukAnalyzer                격국 18종 + 억부/조후 용신
│   ├── ElementStrengthAnalyzer         오행 점수화 → 신강/신약·과다/결핍
│   ├── SinSalAnalyzer                  신살 10종 (천을귀인·역마·공망 등)
│   ├── UnSeong/TwelveSinSalAnalyzer    12운성·12신살 위치별 맵
│   ├── RelationAnalyzer                합충형파해 매트릭스 (원국/운 교차)
│   ├── Seun/WolunCalculator            세운·월운 + 삼재·길흉
│   └── FortuneService                  운세 통합 조회 (진입점)
│
├── reading        LLM 해석 (Claude API + 영구 캐싱)
│   ├── ReadingPromptBuilder            엔진 출력 → 결정적 압축 프롬프트
│   ├── AnthropicReadingGenerator       anthropic-java SDK, 프롬프트 캐싱
│   ├── SajuReadingService              캐시 조회 → 미스 시 1회 생성 → 저장
│   └── SajuReadingEntity/Repository    MariaDB saju_reading 테이블
│
└── api            REST 노출
    ├── SajuController                  계산 API 3개
    ├── ReadingController               해석 API 3개
    ├── SajuDtos                        요청/응답 DTO (한자·한글 병기)
    └── ApiExceptionHandler             400/503 + 한국어 상세 메시지
```

의존 방향은 아래→위 단방향입니다. 계산 경로(domain~analysis)는 DB 없이 동작하며
— 모든 역법 데이터는 공식 또는 1KB 미만 상수 테이블로 메모리에 상주 —
MariaDB는 해석문 캐시(reading)에만 사용됩니다.

### 계산 파이프라인

```
BirthInput (현지 날짜·시각, 시간대, 성별, 옵션)
  → 검증 → 음력이면 양력 변환 → 시간 보정
  → 이중 트랙:
     ├─ instantKst (절대 시점)  → 연주·월주 (절기 순간과 비교)
     └─ corrected (현지 프레임) → 일주·시주 (출생지 태양 기준)
  → SajuResult (팔자 + 일간 + 사주연도)
  → 분석기들 (십성·격국·강약·신살·관계) / FortuneService (대운·세운·월운)
```

절기는 태양 황경 도달이라는 전 지구적 단일 순간이므로 연·월주는 절대 시점으로,
시주·일주는 출생지 하늘의 태양 위치이므로 현지 시각으로 판정합니다.
한국 출생(기본값)은 두 트랙이 동일합니다.

## 빌드 및 실행

```bash
./gradlew test        # 전체 테스트 (~700건, H2 인메모리 사용 — 외부 의존 없음)
./gradlew check       # 테스트 + 커버리지 80% 게이트

# 서버 실행에는 MariaDB 필요 (해석문 캐시용)
docker run -d -p 3306:3306 -e MARIADB_DATABASE=saju \
  -e MARIADB_USER=saju -e MARIADB_PASSWORD=saju -e MARIADB_ROOT_PASSWORD=root mariadb

ANTHROPIC_API_KEY=sk-ant-... ./gradlew bootRun   # 기본 8080 포트
```

- 요구사항: JDK 21+, MariaDB (해석 API 사용 시)
- 환경변수: `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (기본 localhost/saju/saju),
  `ANTHROPIC_API_KEY` (없으면 계산 API는 정상, 해석 API는 캐시 미스 시 503)
- API 문서: 서버 실행 후 http://localhost:8080/swagger-ui.html

## API 사용법

### 1. 원국 계산 + 전체 분석

```bash
curl -X POST localhost:8080/api/v1/saju \
  -H 'Content-Type: application/json' \
  -d '{"year":2024, "month":6, "day":15, "hour":12, "gender":"MALE"}'
```

```jsonc
{
  "sajuYear": 2024,
  "paljaHanja": "甲辰 庚午 庚戌 壬午",
  "paljaHangul": "갑진 경오 경술 임오",
  "dayMaster": "庚",
  "yearPillar":  { "hanja": "甲辰", "hangul": "갑진" },
  // monthPillar, dayPillar, hourPillar ...
  "sipSeong": {
    "year": { "gan": "편재", "jiPrincipal": "편인", "jiJanggan": ["편인", "편재", "상관"] }
    // month, day(gan=null), hour ...
  },
  "gyeokGuk": {
    "name": "정관격", "category": "INNER",
    "yongsin": "토", "johuYongsin": "수", "isSinGang": false
  },
  "elementStrength": {
    "scores": { "목": 1.25, "화": 2.25, "토": 2.1, "금": 2.25, "수": 1.15 },
    "supportScore": 3.35, "opposeScore": 4.65, "isSinGang": false,
    "excessive": [], "deficient": []
  },
  "sinSal": [ { "name": "백호대살", "position": "연주", "isGilsin": false } ],
  "relations": [
    { "type": "천간충", "positions": ["연주", "월주"], "resultElement": null },
    { "type": "반합", "positions": ["월주", "일주"], "resultElement": "화" }
    // ...
  ]
}
```

### 2. 연도별 통합 운세

```bash
curl -X POST localhost:8080/api/v1/saju/fortune/2031 \
  -H 'Content-Type: application/json' \
  -d '{"year":2024, "month":6, "day":15, "hour":12, "gender":"MALE"}'
```

```jsonc
{
  "year": 2031, "age": 7,
  "currentDaeun": { "order": 1, "ganJi": { "hanja": "辛未" }, "startAge": 7, "endAge": 16 },
  "daeunRelations": [ /* 대운-원국 합충형파해 */ ],
  "seun": {
    "ganJi": { "hanja": "辛亥" }, "ganSipSeong": "겁재", "jiSipSeong": "식신",
    "relationsWithWonguk": [ /* ... */ ], "relationsWithDaeun": [ /* ... */ ]
  },
  "wolunList": [
    { "month": 1, "ganJi": { "hanja": "己丑" }, "gilHyung": "보통", "isSamjae": false /* ... */ }
    // 1~12월
  ]
}
```

- `age`는 `조회연도 - 출생 사주연도` (입춘 기준). 대운 기산 전이면 `currentDaeun`은 `null`
- 나이 기준 조회는 `사주연도 + 나이`로 연도를 환산해 호출

### 3. 대운 타임라인

```bash
curl -X POST localhost:8080/api/v1/saju/daeun \
  -H 'Content-Type: application/json' \
  -d '{"year":2024, "month":6, "day":15, "hour":12, "gender":"MALE"}'
```

10개 대운의 간지·나이 구간과 각 대운-원국 관계를 반환합니다.

### 4. LLM 해석 API 3종

계산 API가 반환하는 데이터(JSON)는 FE의 만세력 화면 렌더링용이고, 사용자가 읽을
해석문은 아래 3종이 담당합니다. Claude(Haiku)가 엔진 계산값만 근거로 생성하며,
결과는 MariaDB에 영구 캐싱됩니다.

| 엔드포인트 | 해석 | 내용 |
|---|---|---|
| `POST /api/v1/saju/reading` | 원국 풀이 (평생사주) | 일간·기질, 격국·용신, 오행 균형, 신살, 적성 |
| `POST /api/v1/saju/reading/daeun` | 대운 풀이 | 10대운의 십성·12운성·원국 관계 기반 인생 흐름 |
| `POST /api/v1/saju/reading/marriage` | 결혼운 | 배우자성·배우자궁 상태 + 향후 10년 세운 스캔으로 시기 해석 |
| `POST /api/v1/saju/reading/{year}` | 연도별 운세 | 세운 주제, 대운·원국 관계, 삼재, 월별 흐름 |
| `POST /api/v1/saju/reading/{year}?topic=` | 주제별 운세 | `money`(금전) · `career`(직장) · `health`(건강) |

요청 본문은 계산 API와 동일한 `BirthRequest`입니다.

```bash
curl -X POST localhost:8080/api/v1/saju/reading/2026 \
  -H 'Content-Type: application/json' \
  -d '{"year":1994, "month":10, "day":24, "hour":12, "minute":14, "gender":"FEMALE"}'
```

```jsonc
{
  "reading": "## 원국 개관\n일간 계수(癸水)는 ...",  // 마크다운 해석문
  "model": "claude-haiku-4-5",
  "cached": false,      // 첫 생성 — 재요청 시 true (LLM 호출 없음)
  "cacheKey": "3f2a..." // SHA-256(모델 + 엔진 출력)
}
```

**캐싱 동작** — 캐시 키가 입력이 아닌 **엔진 계산 결과의 해시**라서,
사주가 같으면 자동으로 캐시를 공유합니다:

- 12:14 출생과 12:16 출생(같은 시진) → 팔자 동일 → **캐시 공유**
- 양력 입력과 같은 날짜의 음력 입력 → 변환 후 동일 → **캐시 공유**
- 같은 팔자라도 성별이 다르면(대운 순행/역행) → **분리**
- 같은 팔자라도 대운수(기산 나이)가 다르면 대운·연도 해석은 → **분리** (실제로 다른 운세)
- 엔진 출력이 결정적이므로 캐시는 **만료 없이 영구 유효**

`ANTHROPIC_API_KEY` 미설정 시: 캐시에 있는 해석은 정상 반환되고,
캐시 미스만 `503` + 안내 메시지를 반환합니다.

### 요청 필드 (BirthRequest)

| 필드 | 기본값 | 설명 |
|---|---|---|
| `year` `month` `day` `hour` `minute` | (필수 / minute=0) | 출생 일시 — **현지 벽시계 기준** |
| `gender` | (필수) | `MALE` / `FEMALE` — 대운 순행·역행 판정 |
| `calendarType` | `SOLAR` | `LUNAR`면 음력으로 해석 |
| `isLeapMonth` | `false` | 음력 윤달 여부 |
| `timeZone` | `Asia/Seoul` | 출생지 IANA 시간대 (예: `America/New_York`) |
| `longitude` | `126.978` (서울) | 지방평균시·진태양시 보정용 경도 |
| `timeCorrectionMode` | `STANDARD` | `LOCAL_MEAN_TIME`(경도×4분), `APPARENT_SOLAR_TIME`(+균시차) |
| `zasiMode` | `YAJASI_JEONGJASI` | 자시 처리: 야자시/정자시 구분 vs `SIMPLE`(23시부터 다음날) |

#### 해외 출생 예시

```bash
# 뉴욕 1998-02-05 06:00 현지 시각 출생 — FE는 변환 없이 그대로 전송
curl -X POST localhost:8080/api/v1/saju \
  -H 'Content-Type: application/json' \
  -d '{"year":1998, "month":2, "day":5, "hour":6,
       "gender":"MALE", "timeZone":"America/New_York"}'
# → "戊寅 甲寅 癸未 乙卯" (시주는 현지 아침 卯時)
```

서머타임과 역사적 표준시 변경(한국 UTC+8:30 시기, 1948~88 서머타임)은 tzdata로 자동 처리됩니다.

### 오류 응답

검증 실패는 `400` + 입력값이 포함된 한국어 메시지로 반환됩니다.

```json
{ "message": "출생 연도는 1900~2100 범위여야 합니다: 1899" }
```

## 정확도와 검증

| 항목 | 방식 | 정확도 |
|---|---|---|
| 절기 시각 | Meeus 태양 황경 + Newton-Raphson + ΔT 보정 | 평균 4분, 최대 13분 (KASI 264건 대조) |
| 일진(일주) | epoch day 산술 (60일 주기) | 오차 0 |
| 음력 변환 | 비트마스크 테이블 (1900~2100) | 오차 0 (설날·추석 대조) |
| 표준시 이력 | IANA tzdata | 공식 기록 그대로 |

- 레퍼런스: `src/test/resources/reference/kasi_reference.csv` (287건, 출처 명기)
  — 값을 추가하면 코드 수정 없이 검증 테스트에 자동 반영
- KASI 절기 재수집: `.kasi-api-key`에 공공데이터포털 인증키(Decoding) 저장 후
  `TOL=20 ./scripts/fetch_kasi_jeolgi.sh 2024 2025 ...` (제공 범위 2000~2028)
- CI: push/PR마다 전체 테스트 + JaCoCo 커버리지 80% 게이트 (현재 라인 ~99%)

## 판정 규칙 요약 (유파별 이견이 있는 부분)

- **연·월 경계**: 절입 시각 정각부터 새 달/새 해
- **자시**: 기본 야자시/정자시 구분 (23시대 일주는 당일 유지, 時干은 익일 기준) — `SIMPLE` 옵션 제공
- **격국**: 월지 본기 십성 기준 내격 10종 + 종격·전왕격 8종 (위치 7곳 세력 계산)
- **신강**: 득령·득지·득세 중 2개 이상 (점수식은 비겁+인성 ≥ 4.0/9.0)
- **용신**: 억부 원칙 (신강: 비겁 과다→관성, 인성 과다→재성 / 신약→인성), 외격은 순세
- **대운수**: 절입까지 일수 ÷ 3 반올림 (1~10)

세부 규칙은 각 클래스 상단 주석에 명문화되어 있습니다.
