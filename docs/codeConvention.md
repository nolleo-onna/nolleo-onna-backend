# Code Convention

## **네이밍 컨벤션**

1. 컴포넌트 / class  `PascalCase`
2. 폴더명  `camelCase`
3. 파일 명 *(컴포넌트 제외)*   `camelCase`
4. 변수, 함수  `camelCase`
5. 파라미터  `camelCase`
6. 상수  `BIG_SNAKE_CASE`

## **표현 계층 & 응용 계측의 DTO 네이밍 규칙**

### **응용 계층(Application Layer)**

#### 🔹 Command DTO (쓰기 용도)

비즈니스 로직을 실행하는 요청을 위한 DTO입니다.

유즈 케이스(Use Case) 단위로 사용되며, 도메인 모델을 직접 노출하지 않고 필요한 데이터만 전달합니다.

| DTO 명칭 | 용도 |
| --- | --- |
| `Create<Entity>Command` | 새 엔티티 생성 |
| `Update<Entity>Command` | 기존 엔티티 수정 |
| `Delete<Entity>Command` | 엔티티 삭제 |
| `Register<Entity>Command` | 특정 등록 관련 작업 수행 |
| `Process<Entity>Command` | 특정 비즈니스 로직 실행 |

📌 **예제**

- `CreateUserCommand`
- `UpdateProductCommand`
- `DeleteOrderCommand`
- `RegisterMemberCommand`
- `ProcessPaymentCommand`

---

#### 🔹 Query DTO (조회 용도)

데이터 조회를 최적화하기 위한 DTO입니다.

도메인 객체를 직접 노출하지 않고 필요한 필드만 포함하여 응답을 최적화합니다.

| DTO 명칭 | 용도 |
| --- | --- |
| `Find<Entity>Query` | 특정 엔티티 조회 |
| `List<Entity>Query` | 엔티티 목록 조회 |
| `Get<Entity>Query` | 특정 속성을 포함한 조회 |
| `Search<Entity>Query` | 검색 조건이 있는 조회 |

📌 **예제**

- `FindUserQuery` (`FindUserQuery(1L)` → ID가 `1L`인 `User` 엔티티를 조회)
- `ListOrdersQuery`
- `GetUserEmailQuery` (`GetUserEmailQuery(1L)` → ID가 `1L`인 사용자의 `email`만 조회)
- `SearchCustomersQuery`

---

### 표현 계층(Presentation Layer)

#### 🔹Request DTO (클라이언트 → 서버 요청)

클라이언트가 API에 데이터를 보낼 때 사용하는 DTO입니다.

주로 `@RequestBody`, `@RequestParam`, `@PathVariable` 등의 입력으로 사용됩니다.

| DTO 명칭 | 용도 |
| --- | --- |
| `Create<Entity>Request` | 새 엔티티 생성 요청 |
| `Update<Entity>Request` | 기존 엔티티 수정 요청 |
| `Delete<Entity>Request` | 엔티티 삭제 요청 |
| `Register<Entity>Request` | 특정 등록 관련 요청 |
| `Process<Entity>Request` | 특정 비즈니스 로직 실행 요청 |

📌 **예제**

- `CreateUserRequest`
- `UpdateProductRequest`
- `DeleteOrderRequest`
- `RegisterMemberRequest`
- `ProcessPaymentRequest`

---

#### Response DTO (서버 → 클라이언트 응답)

API 응답을 최적화하기 위한 DTO입니다.

도메인 객체의 모든 필드를 노출하지 않고, 필요한 필드만 포함하여 응답을 최적화합니다.

| DTO 명칭 | 용도 |
| --- | --- |
| `Find<Entity>Response` | 특정 엔티티 조회 응답 |
| `List<Entity>Response` | 엔티티 목록 조회 응답 |
| `Get<Entity>Response` | 특정 속성을 포함한 조회 응답 |
| `Search<Entity>Response` | 검색 결과 응답 |

📌 **예제**

- `FindUserResponse`
- `ListOrdersResponse`
- `GetProductDetailsResponse`
- `SearchCustomersResponse`