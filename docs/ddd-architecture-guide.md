# 놀러온나 DDD 아키텍처 가이드

## 핵심 원칙 한 줄 요약

> **도메인 모델은 순수 Java다. DB는 모른다. 비즈니스 규칙은 도메인 안에만 있다.**

---

## 전체 레이어 구조

```
presentation/     ← HTTP 요청/응답 처리 (Controller, DTO)
application/      ← 유스케이스 흐름 조율 (Service, ApplicationDto)
domain/           ← 비즈니스 규칙과 상태 (Model, Repository 인터페이스)
infrastructure/   ← 기술 구현 (JPA Entity, Repository 구현체, 외부 API)
```

레이어 간 의존 방향은 항상 **안쪽(도메인)** 으로만 향한다.
infrastructure → domain ← application ← presentation

---

## 도메인 모델 vs JPA 엔티티

이 프로젝트에서 가장 중요한 설계 결정이다.  
**도메인 모델과 JPA 엔티티는 분리된 별개의 클래스다.**

### 왜 분리하는가?

| 구분 | 도메인 모델 | JPA 엔티티 |
|---|---|---|
| 위치 | `domain/model/` | `infrastructure/persistence/entity/` |
| 역할 | 비즈니스 규칙과 상태 보유 | DB 테이블 매핑과 영속성 |
| 의존성 | 순수 Java (Lombok만 허용) | JPA, Hibernate |
| 어노테이션 | `@Getter`, `@ToString`, `@Builder` | `@Entity`, `@Column`, `@OneToMany` ... |
| 생성 방식 | `create()` / `restore()` 팩토리 메서드 | `from(domain)` / JPA가 직접 생성 |

---

## 팩토리 메서드 패턴: create() vs restore()

도메인 모델은 생성자가 private이다. 외부에서는 반드시 팩토리 메서드를 통해 만든다.

### create() — 새로운 도메인 객체 생성

비즈니스적으로 "새로 만드는" 상황에서 사용한다.
- `id`는 null (DB에 저장되기 전)
- 도메인 기본값을 강제 적용 (예: `role = UserRole.USER`)
- 신규 가입, 코스 생성 등에서 호출

```java
// 사용 예
User user = User.create(externalId, provider, email, nickname, profileImageUrl);
// → id=null, role=USER 고정, 가입 시 항상 이 팩토리 사용
```

```java
// 도메인 모델 내부
public static User create(String externalId, OAuthProvider provider, ...) {
    return builder()
            .id(null)           // DB 저장 전이므로 null
            .role(UserRole.USER) // 신규 가입은 항상 USER 역할 고정
            .build();
}
```

### restore() — DB에서 꺼낸 데이터를 도메인 객체로 복원

JPA 엔티티의 `toDomain()`에서만 호출한다.  
DB에 이미 존재하는 데이터를 도메인 객체로 "재구성"하는 용도다.

```java
// JPA 엔티티 내부 (infrastructure 레이어)
public User toDomain() {
    return User.restore(id, externalId, provider, email, nickname, profileImageUrl, role);
    // → DB에서 읽은 값을 그대로 넘겨 도메인 객체를 재구성
}
```

**왜 new 생성자가 아닌 팩토리 메서드인가?**  
팩토리 메서드는 "어떤 의도로 만드는지"를 코드로 표현한다.  
`User.create()`를 보면 "신규 가입"임을 알고, `User.restore()`를 보면 "DB 복원"임을 안다.  
`new User(...)`는 의도가 드러나지 않는다.

---

## 도메인 로직은 도메인 안에만 있다

비즈니스 규칙은 도메인 모델 메서드로 표현한다.  
Service나 Controller에 if문으로 흩어지면 안 된다.

### 예시 1: 스팟 활성화 여부 검증

```java
// domain/model/Spot.java
public void validateActive() {
    if (!isActive) {
        throw new BusinessException(SpotErrorCode.SPOT_NOT_ACTIVE);
    }
}
```

```java
// application/service/SpotQueryService.java
public SpotResult getSpot(Long contentId) {
    Spot spot = spotRepository.findById(contentId)
            .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

    spot.validateActive(); // 비즈니스 규칙은 도메인에 위임

    return SpotResult.from(spot);
}
```

Application Service는 "무엇을 할지"만 결정하고,  
"어떤 규칙을 적용할지"는 도메인이 결정한다.

### 예시 2: 코스 삭제 여부, 조회수 증가, 공개 전환

```java
// domain/model/GeneratedCourse.java

// 삭제된 코스인지 확인 — 비즈니스 규칙
public void validateNotDeleted() {
    if (deletedAt != null) {
        throw new BusinessException(CourseErrorCode.COURSE_ALREADY_DELETED);
    }
}

// 조회수 증가 — 상태 변경 로직
public void incrementViewCount() {
    this.viewCount++;
}

// 공개/비공개 전환 — 상태 변경 로직
public void togglePublic() {
    this.isPublic = !this.isPublic;
}
```

이 메서드들은 도메인의 상태를 알고 규칙을 강제한다.  
Service가 직접 `if (deletedAt != null)`를 체크하는 구조가 아니다.

### 예시 3: VO의 도메인 로직

VO(Value Object)도 단순한 데이터 덩어리가 아니다. 자체 판단 로직을 가질 수 있다.

```java
// domain/model/vo/BusinessHoursInfo.java
public boolean isStale() {
    return staleAfter != null && OffsetDateTime.now().isAfter(staleAfter);
    // "운영시간 정보가 만료됐는지" — 외부에서 직접 날짜 비교하지 않음
}
```

---

## 데이터 흐름 전체 그림

### 조회 흐름 (Read)

```
HTTP GET /spots/123
    ↓
SpotController
    ↓ contentId 전달
SpotQueryService          ← Application Layer
    ↓ SpotRepository.findById() 호출
SpotPersistenceAdapter    ← Infrastructure Layer (Repository 구현체)
    ↓ JPA 쿼리
SpotJpaRepository         ← Spring Data JPA
    ↓ DB 조회 결과
SpotEntity                ← JPA 엔티티 (DB 매핑)
    ↓ .toDomain() 호출
Spot                      ← 순수 도메인 모델 (Spot.restore()로 재구성)
    ↑
SpotQueryService
    ↓ spot.validateActive() — 비즈니스 규칙 검증
    ↓ SpotResult.from(spot) — Application DTO로 변환
SpotController
    ↓ SpotResponse.from(result) — HTTP 응답 DTO로 변환
HTTP 200 OK
```

### 저장 흐름 (Write)

```
HTTP POST (신규 사용자 저장)
    ↓
UserService
    ↓
User.create(...)          ← 도메인 모델 생성 (id=null, role=USER)
    ↓
UserRepository.save(user) ← 도메인 Repository 인터페이스 호출
    ↓
UserPersistenceAdapter    ← 구현체
    ↓ UserEntity.from(user) — 도메인 → JPA 엔티티 변환
UserJpaRepository.save()  ← Spring Data JPA
    ↓ DB INSERT
```

---

## Repository: 인터페이스는 도메인에, 구현체는 infrastructure에

```
domain/repository/UserRepository.java       ← 인터페이스 (도메인이 정의)
infrastructure/repository/
    UserJpaRepository.java                  ← Spring Data JPA 인터페이스
    UserPersistenceAdapter.java             ← 실제 구현체 (도메인 인터페이스 implements)
```

```java
// domain/repository/UserRepository.java
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByExternalIdAndProvider(String externalId, OAuthProvider provider);
}
```

```java
// infrastructure/repository/UserPersistenceAdapter.java
@Repository
public class UserPersistenceAdapter implements UserRepository {

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.from(user); // 도메인 → JPA 엔티티
        return userJpaRepository.save(entity).toDomain(); // JPA 엔티티 → 도메인
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(UserEntity::toDomain); // JPA 엔티티 → 도메인
    }
}
```

**핵심**: 도메인은 `UserRepository` 인터페이스만 안다. JPA가 어떻게 동작하는지 전혀 모른다.  
이것이 DIP(의존성 역전 원칙)다. 나중에 JPA를 다른 기술로 교체해도 도메인은 건드리지 않아도 된다.

---

## Application Service의 역할

Application Service는 **흐름만 조율**한다. 비즈니스 로직은 없다.

```java
// application/service/SpotQueryService.java
@Service
@RequiredArgsConstructor
public class SpotQueryService {

    private final SpotRepository spotRepository; // 도메인 Repository 인터페이스 주입

    public SpotResult getSpot(Long contentId) {
        // 1. 조회
        Spot spot = spotRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(SpotErrorCode.SPOT_NOT_FOUND));

        // 2. 도메인 규칙 실행 (로직은 Spot 안에 있음)
        spot.validateActive();

        // 3. 결과 변환
        return SpotResult.from(spot);
    }
}
```

Application Service가 해야 할 일:
- Repository에서 도메인 객체 꺼내기
- 도메인 메서드 호출하기
- 결과를 Application DTO로 변환해서 반환하기

Application Service가 하면 안 되는 일:
- `if (spot.getIsActive() == false)` 같은 비즈니스 판단
- 계산 로직 직접 구현
- 여러 도메인을 직접 조합해서 복잡한 규칙 처리

---

## VO(Value Object)가 하는 일

VO는 여러 필드를 묶어 의미 있는 개념으로 만든다.  
단순 데이터 컨테이너가 아니라, 그 개념과 관련된 판단 로직도 가질 수 있다.

```
Spot 도메인 모델이 가진 VO들:

Location         — 위치 (mapX, mapY, geog)
Address          — 주소 (addr1, addr2, zipcode)
BusinessHoursInfo — 운영시간 + AI 파싱 메타 (isStale() 판단 포함)
CategoryClassification — 관광API 3단계 분류
ScoreInfo        — 점수 집계 (trend, gem, popularity, avg_rating ...)
ConcentrationInfo — 혼잡도 집계
```

`Spot`이 `String mapX, String mapY, String geog`를 각각 들고 있지 않고  
`Location location` 하나로 묶는 이유:  
위치와 관련된 개념이 분산되지 않고 한 곳에 모인다.

---

## 자주 헷갈리는 것들

### Q. 도메인 모델에 @Entity 붙이면 안 되나요?
안 된다. @Entity가 붙는 순간 JPA에 의존하게 된다.  
도메인 모델은 JPA가 없어도 테스트할 수 있어야 한다.

### Q. Application Service가 도메인 모델을 직접 Controller에 반환하면 안 되나요?
안 된다. 도메인 모델이 HTTP 레이어에 노출되면
- 직렬화 문제 (Jackson이 도메인 내부를 그대로 시리얼라이즈)
- 도메인 변경이 API 스펙 변경으로 직결
- 보안 문제 (노출하면 안 되는 필드까지 반환 가능)

`SpotResult`(Application DTO) → `SpotResponse`(Presentation DTO)로 단계별로 변환한다.

### Q. restore()에서 유효성 검증을 하면 안 되나요?
restore()는 "이미 DB에 저장된 유효한 데이터"를 복원하는 것이므로  
검증하지 않는다. 검증은 create()나 도메인 메서드에서 한다.

### Q. 도메인 메서드가 Exception을 던져도 되나요?
된다. 오히려 도메인에서 던져야 한다.  
`BusinessException`은 도메인의 규칙 위반을 나타내는 공통 예외다.  
`GlobalExceptionHandler`가 이를 잡아 HTTP 응답으로 변환한다.

---

## 파일 위치 참고

```
src/main/java/com/nolleo/onna/
│
├── common/
│   ├── exception/         BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── infrastructure/    CreateAudit, UpdateAudit, SoftDeleteAudit (@Embeddable)
│   └── response/          ApiResponseDto, ErrorResponseDto
│
└── domain/
    ├── user/
    │   ├── domain/
    │   │   ├── model/     User.java, OAuthProvider.java, UserRole.java
    │   │   ├── repository/ UserRepository.java (인터페이스)
    │   │   └── exception/ UserErrorCode.java
    │   ├── application/
    │   │   └── service/   UserService.java
    │   ├── infrastructure/
    │   │   └── persistence/
    │   │       ├── entity/     UserEntity.java
    │   │       └── repository/ UserJpaRepository.java, UserPersistenceAdapter.java
    │   └── presentation/
    │       └── UserController.java
    │
    ├── spot/              (동일 구조)
    └── course/            (동일 구조)
```