# 채널 백엔드 헥사고날 PoC

Java 17 이상, Spring Boot 4.1.2, Gradle Wrapper를 사용하는 단일 프로젝트입니다. [CHSBackPOCV1](https://github.com/zx10185252/CHSBackPOCV1)의 샘플 API와 전문 형식을 참고해 독립적으로 구현했습니다.

## 구조

```mermaid
flowchart TD
    CLIENT["채널 프론트 또는 API 클라이언트"]
    subgraph WAS["Spring Boot WAS"]
        WEB["REST Controller · 입력 어댑터"]
        REQ["ChannelRequests → ChannelRequestService"]
        PROD["ProductInquiry → ProductInquiryService"]
        DOMAIN["Domain · 접수 제목과 상품 코드 규칙"]
        STORE["ChannelRequestStore · 저장 포트"]
        CORE["CoreProductLookup · 상품 조회 포트"]
        JDBC["MariaDbChannelRequestAdapter · JdbcClient"]
        DEMO["데모 어댑터"]
        MCI["MCI 어댑터 · 전문 생성과 결과 변환"]
        TRANSPORT["HTTP 스텁 전송부"]
        WEB -->|접수·조회| REQ
        WEB -->|상품 조회| PROD
        REQ -.-> DOMAIN
        PROD -.-> DOMAIN
        REQ --> STORE --> JDBC
        PROD --> CORE
        CORE -->|기본 설정| DEMO
        CORE -->|mci-stub 설정| MCI
        MCI --> TRANSPORT
    end
    CLIENT --> WEB
    JDBC --> DB["MariaDB"]
    TRANSPORT --> STUB["PoC 계정계 스텁"]
    EIMS["EIMS 샘플 전문 계약"] -.->|개발 시 참조| MCI
```

| 기능 | 입력 포트 | 서비스 | 출력 포트 | 구현 어댑터 |
|---|---|---|---|---|
| 상품 조회 | `ProductInquiry` | `ProductInquiryService` | `CoreProductLookup` | 데모 또는 MCI 상품 조회 어댑터 |
| 요청 접수·조회 | `ChannelRequests` | `ChannelRequestService` | `ChannelRequestStore` | MariaDB / JdbcClient |

`product`, `request` 패키지 아래에 각각 `domain`, `application`, `adapter`가 있습니다. Domain은 상품 코드 형식과 접수 제목의 규칙을 정의합니다. Application은 도메인 모델을 사용하고 외부 작업을 자신이 정의한 출력 포트에 요청합니다. Spring은 `config/ChannelConfiguration`에서 포트와 구현체를 연결합니다.

`MciProductInquiryAdapter`가 `CoreProductLookup`을 구현하고 [EIMS 샘플 계약](docs/eims/CHS-CORE-001-v1.0.json)에 따라 전문 생성과 결과 변환을 맡습니다. PoC에서는 `HttpMciStubTransport`가 샘플 전문을 HTTP로 전달합니다. 실제 MCI 연계는 승인된 전문과 전송 방식이 확정되면 해당 계약을 적용합니다.

## 실행

로컬 Docker Compose에서 MariaDB 11.8을 실행한 뒤 Spring Boot를 시작합니다.

```sh
export DB_PASSWORD='replace-with-a-local-password'
docker compose up -d --wait
./gradlew bootRun
```

기본 API 주소는 `127.0.0.1:8080`이고 DB는 `127.0.0.1:3306/chs_poc`, 사용자는 `chs`입니다. 각각 `SERVER_PORT`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`으로 변경합니다. 사용할 포트와 DB 계정 설정은 Compose를 실행하기 전에 지정합니다. Compose는 볼륨을 처음 초기화할 때 계정과 [테이블](src/main/resources/db/mariadb/schema.sql)을 생성하며 이후에도 데이터를 보존합니다.

이미 운영 중인 MariaDB는 `DB_URL`에 `jdbc:mariadb://호스트:3306/DB이름`을 지정하고 `DB_USERNAME`과 `DB_PASSWORD`를 설정해 연결합니다. [DDL](src/main/resources/db/mariadb/schema.sql)은 대상 DB에 사전 적용합니다. 이전 PoC의 SQLite 파일을 사용했다면 필요한 데이터는 별도로 이관합니다.

`./gradlew test`는 Docker를 사용해 독립된 테스트용 MariaDB 컨테이너에 스키마를 적용합니다. 최초 빌드는 Gradle과 Maven 의존성을 내려받습니다.

### 요청 접수·조회

```sh
curl -i http://127.0.0.1:8080/api/v1/channel-requests \
  -H 'Content-Type: application/json' \
  --data '{"subject":"채널 POC 요청"}'
```

응답은 `201 Created`이며 본문에 UUID, 제목, 상태, 생성 시간이 포함됩니다. `Location` 헤더의 경로를 GET으로 조회하면 저장된 요청을 확인할 수 있습니다. 공백 또는 120자를 초과하는 제목과 잘못된 UUID는 `400`, 존재하지 않는 UUID는 `404`입니다. `RECEIVED`는 MariaDB에 접수 기록이 저장됐다는 뜻입니다. 생성 시간은 UTC 기준으로 마이크로초까지 저장합니다.

### 상품 조회

기본 프로필은 `SAMPLE-DEPOSIT` 샘플 상품을 반환합니다.

```sh
curl -i http://127.0.0.1:8080/api/v1/core/product-inquiries \
  -H 'Content-Type: application/json' \
  --data '{"productCode":"SAMPLE-DEPOSIT"}'
```

샘플 계약을 제공하는 계정계 스텁을 띄운 상태에서 MCI 어댑터를 사용하려면 다음 설정으로 실행합니다.

```sh
SPRING_PROFILES_ACTIVE=mci-stub CORE_BASE_URL=http://127.0.0.1:8091 ./gradlew bootRun
```

스텁 접속 및 응답 타임아웃은 `CORE_CONNECT_TIMEOUT_MS`와 `CORE_READ_TIMEOUT_MS`로 설정합니다. 기존 `core-http` 프로필도 같은 구성을 선택합니다.

### 고객이 상품 조회 결과를 받는 흐름

그림은 `mci-stub` 프로필에서 채널 API 응답이 고객 화면에 표시되는 예입니다. Spring Boot WAS에는 Controller와 업무 서비스가 있고, MCI 어댑터도 같은 WAS 안에서 실행됩니다. 고객 안내 문구와 화면은 연동 예시입니다.

```mermaid
sequenceDiagram
    actor C as 고객
    participant FE as 채널 프론트
    participant BE as Spring Boot WAS
    participant MCI as MCI 어댑터
    participant CORE as 계정계 스텁
    C->>FE: 상품 상세 선택
    FE->>BE: POST /api/v1/core/product-inquiries
    BE->>MCI: CoreProductLookup으로 상품코드 조회
    MCI->>CORE: CHS-CORE-001 v1.0 샘플 전문 요청
    alt 상품 조회 성공
        CORE-->>MCI: 0000과 상품명
        MCI-->>BE: 채널 상품 모델
        BE-->>FE: 200과 productCode, productName
        FE-->>C: POC 예금 상품 상세 표시
    else 등록된 상품 없음
        CORE-->>MCI: 1001
        MCI-->>BE: 상품 없음
        BE-->>FE: 404
        FE-->>C: 상품을 찾을 수 없습니다
    else 응답 계약 불일치 또는 타임아웃
        MCI-->>BE: 연계 오류
        BE-->>FE: 502 또는 504
        FE-->>C: 조회 실패 안내
    end
```

조회 성공 시 채널 프론트에는 `{"productCode":"SAMPLE-DEPOSIT","productName":"POC 예금 상품"}`이 반환됩니다. 입력 형식 오류는 `400`, 상품 없음은 `404`, 계정계 연결 실패·응답 계약 불일치는 `502`, 타임아웃은 `504`입니다.

`MciStubProfileTest`는 계정계 역할의 로컬 HTTP 스텁을 띄워 전문 요청, 채널 성공 응답, 상품 없음, 전문 불일치, 타임아웃을 검증합니다.
