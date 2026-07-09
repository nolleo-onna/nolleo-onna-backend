# GeneratedCourse DDD 설계 리뷰

> 2026.07.08 · `domain/generatedcourse` 도메인 대상
> 도메인 주도 설계(DDD) 관점에서 레이어별 책임 분리를 검토하고 수정한 기록

---

## 1. DDD 레이어별 책임

| 레이어 | 책임 |
|--------|------|
| **Presentation** | HTTP 요청·응답 변환. 도메인 객체 → DTO 변환만 수행, 계산 로직 금지 |
| **Application** | 유스케이스 조율. 도메인 서비스·리포지토리 조합, 트랜잭션 관리. 비즈니스 규칙은 도메인에 위임 |
| **Domain** | 핵심 비즈니스 규칙의 집합. 외부 의존성 없이 순수하게 동작 |
| **Infrastructure** | DB, Redis, 외부 API 등 기술적 세부사항. 도메인 인터페이스를 구현 |

**핵심 질문**: "이 지식은 누가 알아야 하는가?" — 답이 도메인 객체라면 그 로직은 도메인 안에 있어야 한다.

---

## 2. 잘 설계된 부분 ✅

### 2-1. `CourseComposer` — Domain Service

슬롯 패턴 선택 로직(코스 타입 + duration → 장소 조합)이 도메인 서비스로 올바르게 분리되어 있다.
외부 의존성이 전혀 없는 순수 도메인 로직이므로 단위 테스트가 쉽다.

### 2-2. `DistrictCenter` — Domain Enum

지역명 → 위도·경도 매핑이 도메인 열거형으로 표현된다.
인프라(DB, API)에 의존하지 않고 도메인이 자체적으로 보유해야 할 지식.

### 2-3. `GeneratedCourse.addItem()` — Aggregate Root 캡슐화

아이템 추가가 반드시 Aggregate Root를 통해서만 이루어진다.
`getItems()`는 `unmodifiableList`를 반환하므로 외부에서 컬렉션을 직접 조작할 수 없다.

### 2-4. `CourseWarningService` — Application Layer

Redis(외부 인프라)를 조율하는 날씨·혼잡도 경고 로직이 Application 레이어에 위치한다.
도메인 모델이 인프라에 의존하지 않는다.

### 2-5. PostGIS 거리 정렬 — Application Layer

PostGIS 의존 쿼리(거리 정렬, 거리값 일괄 조회)가 Application 서비스에서 조율된다.
도메인이 DB 방언에 오염되지 않는다.

### 2-6. Repository 인터페이스 분리

도메인 레이어는 인터페이스(`GeneratedCourseRepository`)만 가지고, 구현체는 Infrastructure에 있다.
도메인이 JPA/JDBC 같은 기술에 의존하지 않는다 (의존성 역전).

---

## 3. 수정 사항 🔧

### 3-1. `buildTitle()` / `buildDescription()` — Application → Domain

**문제**
"ACTIVE 타입이면 제목에 '액티브'가 들어간다", "동행자를 포함한 설명을 만든다"는 **CourseType이 알아야 할 도메인 규칙**이다. 이 로직이 Application 서비스의 private 메서드로 있으면 CourseType을 쓰는 다른 컨텍스트에서 중복 구현이 생기고, 타입 추가 시 수정 지점이 분산된다.

**Before** — `CourseGenerationService.java`

```java
// Application 서비스에 도메인 규칙이 새어 있음
private static String buildTitle(String signgu, CourseType type, String duration) {
    String durationLabel = "FULL_DAY".equalsIgnoreCase(duration) ? "하루" : "반나절";
    String typeLabel = switch (type) {
        case ACTIVE    -> "액티브";
        case CULTURE   -> "문화 탐방";
        case FOOD_TOUR -> "맛집 투어";
    };
    return signgu + " " + typeLabel + " " + durationLabel + " 코스";
}

private static String buildDescription(CourseType type, String companion) {
    return switch (type) {
        case ACTIVE    -> companion + "과(와) 함께 체험·레저를 즐기는 액티브 코스입니다.";
        case CULTURE   -> companion + "과(와) 함께 자연, 전시, 역사를 둘러보는 문화 탐방 코스입니다.";
        case FOOD_TOUR -> companion + "과(와) 함께 맛집과 카페를 중심으로 즐기는 식도락 코스입니다.";
    };
}
```

**After** — `CourseType.java`

```java
// 도메인 규칙을 CourseType이 직접 소유
public enum CourseType {

    ACTIVE("액티브") {
        @Override
        public String buildDescription(String companion) {
            return companion + "과(와) 함께 체험·레저를 즐기는 액티브 코스입니다.";
        }
    },
    CULTURE("문화 탐방") { ... },
    FOOD_TOUR("맛집 투어") { ... };

    private final String label;

    /** 코스 제목 생성 — "{signgu} {유형라벨} {반나절|하루} 코스" */
    public String buildTitle(String signgu, String duration) {
        String durationLabel = "FULL_DAY".equalsIgnoreCase(duration) ? "하루" : "반나절";
        return signgu + " " + label + " " + durationLabel + " 코스";
    }

    public abstract String buildDescription(String companion);
}
```

```java
// 서비스에서는 위임만
type.buildTitle(command.signgu(), command.duration())
type.buildDescription(command.companion())
```

**효과**
- 새 CourseType 추가 시 컴파일 에러로 `buildDescription()` 구현을 강제
- 서비스가 아닌 타입 자체가 자신의 표현 방식을 책임짐

---

### 3-2. `totalMinutes` 계산 — Presentation DTO → Domain Model

**문제**
"코스의 총 소요시간은 아이템 체류시간의 합이다"는 **도메인 지식**이다. 이 계산이 응답 DTO의 팩토리 메서드 안에 있으면, 같은 계산이 필요한 다른 컨텍스트에서 중복 구현된다.

**Before** — `CourseResponse.java`

```java
// DTO가 도메인 계산을 직접 수행
int computedMinutes = course.getItems().stream()
        .mapToInt(item -> item.getDurationMinutes() != null ? item.getDurationMinutes() : 0)
        .sum();

Integer totalMinutes = (course.getCourseSummary() != null && course.getCourseSummary().totalMinutes() != null)
        ? course.getCourseSummary().totalMinutes()
        : computedMinutes;
```

**After** — `GeneratedCourse.java`

```java
/**
 * 코스 전체 예상 소요시간(분)을 반환한다.
 * CourseSummary에 값이 있으면 우선 사용, 없으면 아이템 체류시간 합산으로 계산.
 */
public int computeTotalMinutes() {
    if (courseSummary != null && courseSummary.totalMinutes() != null) {
        return courseSummary.totalMinutes();
    }
    return items.stream()
            .mapToInt(item -> item.getDurationMinutes() != null ? item.getDurationMinutes() : 0)
            .sum();
}
```

```java
// DTO에서는 위임만
course.computeTotalMinutes()
```

**효과**
- 총 소요시간 계산이 한 곳에만 존재
- CourseSummary 존재 여부에 따른 폴백 로직도 도메인이 책임짐

---

### 3-3. `mapPlaceIds` 수집 — Application Service → Domain Model

**문제**
"코스가 참조하는 장소 ID 목록"은 `GeneratedCourse`가 알아야 할 정보다. Application 서비스가 아이템 리스트를 직접 순회하며 수집하면 **Aggregate 내부 구조가 서비스에 노출**된다.

**Before** — `CourseGenerationService.java`

```java
// 서비스가 Aggregate 내부를 직접 순회
Set<Long> mapPlaceIds = new HashSet<>();
filtered.forEach(c -> c.getItems().forEach(item -> {
    if (item.getMapPlaceId() != null) mapPlaceIds.add(item.getMapPlaceId());
}));
```

**After** — `GeneratedCourse.java`

```java
/** 코스 아이템이 참조하는 mapPlaceId 목록을 반환한다. 장소 일괄 조회 시 사용. */
public Set<Long> getMapPlaceIds() {
    return items.stream()
            .map(GeneratedCourseItem::getMapPlaceId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
}
```

```java
// 서비스에서는 위임만
Set<Long> mapPlaceIds = filtered.stream()
        .flatMap(c -> c.getMapPlaceIds().stream())
        .collect(Collectors.toSet());
```

**효과**
- Aggregate 내부 구조(items 컬렉션)가 캡슐화됨
- 아이템 구조가 바뀌어도 서비스 코드 수정 불필요

---

## 4. 총 정리

| 로직 | 수정 전 | 수정 후 | 이유 |
|------|---------|---------|------|
| `buildTitle()` | ⚠️ Application Service | ✅ CourseType (Domain) | 타입이 자신의 표현 방식을 알아야 함 |
| `buildDescription()` | ⚠️ Application Service | ✅ CourseType (Domain) | 타입 추가 시 컴파일 에러로 구현 강제 |
| `computeTotalMinutes()` | ⚠️ CourseResponse DTO | ✅ GeneratedCourse (Domain) | 도메인 계산 로직의 단일 책임 |
| `getMapPlaceIds()` | ⚠️ Application Service | ✅ GeneratedCourse (Domain) | Aggregate 내부 캡슐화 |
| 슬롯 패턴 조합 | ✅ CourseComposer (Domain) | 유지 | 처음부터 올바른 위치 |
| PostGIS 거리 정렬 | ✅ Application Service | 유지 | 인프라 조율은 Application 레이어 |
| Redis 경고 계산 | ✅ Application Service | 유지 | 외부 인프라 조율 |

---

## 5. 핵심 원칙

> **"이 지식은 누가 알아야 하는가?"** 를 물어봤을 때 도메인 객체가 답이라면, 그 로직은 도메인 안에 있어야 한다.
>
> Application 서비스는 도메인과 인프라를 **조율(orchestrate)** 하는 역할이며, 비즈니스 규칙을 직접 구현하는 장소가 아니다.

DDD에서 이런 안티패턴을 **빈약한 도메인 모델(Anemic Domain Model)** 이라 부른다.
도메인 객체가 getter/setter만 가진 데이터 덩어리가 되고, 모든 로직이 서비스에 몰리는 형태 — 이번 수정은 그 반대 방향인 **풍부한 도메인 모델(Rich Domain Model)** 로 가는 리팩토링이다.
