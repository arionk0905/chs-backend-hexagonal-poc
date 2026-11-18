# 채널 백엔드 헥사고날 PoC

Java 17 이상, Spring Boot 4.1.2, Gradle Wrapper를 사용하는 단일 프로젝트입니다. [CHSBackPOCV1](https://github.com/zx10185252/CHSBackPOCV1)의 샘플 API와 전문 형식을 참고해 독립적으로 구현했습니다.

## 구조

```text
채널 API → 입력 어댑터 → 입력 포트 → Application Service → 출력 포트
                                                            ↑
                                                 HTTP / 데모 / SQLite 어댑터
```

| 기능 | 입력 포트 | 서비스 | 출력 포트 | 구현 어댑터 |
|---|---|---|---|---|
| 상품 조회 | `ProductInquiry` | `ProductInquiryService` | `CoreProductLookup` | 데모 데이터 또는 계정계 HTTP |
| 요청 접수·조회 | `ChannelRequests` | `ChannelRequestService` | `ChannelRequestStore` | SQLite / JdbcClient |

`product`, `request` 패키지 아래에 각각 `domain`, `application`, `adapter`가 있습니다. Domain은 상품 코드 형식과 접수 제목의 규칙을 정의합니다. Application은 도메인 모델을 사용하고 외부 작업을 자신이 정의한 출력 포트에 요청합니다. Spring은 `config/ChannelConfiguration`에서 포트와 구현체를 연결합니다.

## MCI·EAI와 EIMS

채널 애플리케이션에서 연계 업무마다 필요한 포트를 정의하고, 해당 시스템의 어댑터를 같은 Spring Boot 프로젝트에 구현합니다.

| 연계 상황 | PoC에서의 위치 |
|---|---|
| 채널이 MCI를 통해 계정계 상품을 조회 | `product/adapter/out/core/mci/`에 `MciProductInquiryAdapter`를 두고 `CoreProductLookup` 구현 |
| 채널이 다른 업무 시스템을 EAI로 직접 호출 | 해당 업무 패키지의 `adapter/out/eai/`에 어댑터를 두고 해당 업무의 출력 포트 구현 |
| 채널이 EAI에서 이벤트를 수신 | 해당 업무 패키지의 `adapter/in/eai/`에서 전문을 해석해 입력 포트 호출 |

계정계 상품 조회의 연계 경로 예시는 `채널 API → 상품 조회 서비스 → CoreProductLookup → MCI 어댑터 → MCI → 계정계`입니다. 채널이 MCI만 호출하고 MCI 뒤에서 EAI가 연계되는 구성이라면, 채널 코드에 필요한 구현은 MCI 어댑터 하나입니다. MCI 호출 설정, 전문 변환, 결과 코드와 타임아웃 변환은 이 어댑터에 모읍니다.

**EIMS는 이 설계에서 전문 계약의 개발 입력으로 사용합니다.** PRD의 채널 API 계약과 함께 EIMS의 전문 ID·버전, 요청·응답 필드, 인코딩, 결과 코드, 상관관계 ID를 확인하고 전문 타입·변환기·계약 테스트에 반영합니다. 현재 [EIMS 샘플 계약](docs/eims/CHS-CORE-001-v1.0.json)은 데모용 HTTP 전문입니다. MCI·EAI 구현에는 실제 연계 경로와 승인된 전문 계약, 전송 방식이 필요합니다.

**프로젝트 분리 기준은 배포와 재사용 범위입니다.** 채널만 사용하는 어댑터는 현재 프로젝트에 둡니다. 여러 앱이 같은 전문 코덱이나 연계 클라이언트를 공유하면 공통 라이브러리 모듈을 검토합니다. 연계 게이트웨이 또는 여러 팀이 공유하는 스텁 서버를 별도로 배포·운영하는 경우에는 해당 실행 앱을 별도 프로젝트로 관리할 수 있습니다.

## 실행

```sh
./gradlew test
./gradlew bootRun
```

최초 빌드는 Gradle과 Maven 의존성을 내려받습니다. 서버 기본 주소는 `127.0.0.1:8080`이며 SQLite는 실행 위치에 `channel-poc.sqlite3`를 생성합니다. `SERVER_PORT`와 `SQLITE_DB_PATH`로 변경할 수 있습니다. 설정한 DB 경로의 상위 디렉터리는 미리 만듭니다.

### 요청 접수·조회

```sh
curl -i http://127.0.0.1:8080/api/v1/channel-requests \
  -H 'Content-Type: application/json' \
  --data '{"subject":"채널 POC 요청"}'
```

응답은 `201 Created`이며 본문에 UUID, 제목, 상태, 생성 시간이 포함됩니다. `Location` 헤더의 경로를 GET으로 조회하면 저장된 요청을 확인할 수 있습니다. 공백 또는 120자를 초과하는 제목과 잘못된 UUID는 `400`, 존재하지 않는 UUID는 `404`입니다. `RECEIVED`는 SQLite에 접수 기록이 저장됐다는 뜻입니다.

### 상품 조회

기본 프로필은 `SAMPLE-DEPOSIT` 샘플 상품을 반환합니다.

```sh
curl -i http://127.0.0.1:8080/api/v1/core/product-inquiries \
  -H 'Content-Type: application/json' \
  --data '{"productCode":"SAMPLE-DEPOSIT"}'
```

샘플 계약을 제공하는 계정계 HTTP 스텁이 있다면 같은 채널 API를 다음 설정으로 실행합니다.

```sh
SPRING_PROFILES_ACTIVE=core-http CORE_BASE_URL=http://127.0.0.1:8091 ./gradlew bootRun
```

HTTP 어댑터의 접속 및 응답 타임아웃은 `CORE_CONNECT_TIMEOUT_MS` 및 `CORE_READ_TIMEOUT_MS`로 설정합니다. 채널 API는 정상 조회 `200`, 상품 없음 `404`, 계정계 연결 실패·응답 계약 불일치 `502`, 타임아웃 `504`를 반환합니다. `CoreHttpProfileTest`는 로컬 스텁을 띄워 HTTP 어댑터 선택, 전문 전송 및 채널 응답 변환을 확인합니다.
