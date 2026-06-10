# Embedded Audit — `@Embeddable` 감사 Value Objects

> **[DDD] Common Infrastructure — Embeddable Audit Value Objects**
>
> JPA `@Embeddable` 기반 감사(Audit) 정보 패키지입니다.
> `@MappedSuperclass` 상속 대신 각 엔티티가 필요한 감사 객체만 **선택적으로 조합**합니다.

---

## 클래스 목록

### `CreateAudit`
생성 감사 정보

| 필드 | 타입 | 컬럼 | 설명 |
|---|---|---|---|
| `createdAt` | `OffsetDateTime` | `created_at` | 생성 일시 (updatable = false) |
| `createdBy` | `String` | `created_by` | 생성자 ID (updatable = false) |

```java
// 팩토리 메서드
CreateAudit.now(createdBy)
```

- 사용 대상: **모든 엔티티** (생성 시각은 항상 필요)

---

### `UpdateAudit`
수정 감사 정보

| 필드 | 타입 | 컬럼 | 설명 |
|---|---|---|---|
| `updatedAt` | `OffsetDateTime` | `updated_at` | 수정 일시 |
| `updatedBy` | `String` | `updated_by` | 수정자 ID |

```java
// 초기화 (생성 시점 — 아직 수정 없음)
UpdateAudit.empty()

// 수정 시 호출
updateAudit.touch(updatedBy)
```

- 사용 대상: 수정이 발생하는 엔티티 (User, Spot, Course, Editorial 등)

---

### `SoftDeleteAudit`
소프트 삭제 감사 정보

| 필드 | 타입 | 컬럼 | 설명 |
|---|---|---|---|
| `deletedAt` | `OffsetDateTime` | `deleted_at` | 삭제 일시 (`null` = 활성) |
| `deletedBy` | `String` | `deleted_by` | 삭제자 ID |

```java
// 초기화 (활성 상태)
SoftDeleteAudit.active()

// 소프트 삭제 (이미 삭제된 경우 무시)
softDeleteAudit.softDelete(deletedBy)

// 복구
softDeleteAudit.restore()

// 삭제 여부 확인
softDeleteAudit.isDeleted()
```

- 사용 대상: 소프트 삭제가 필요한 엔티티 (Review, Report, Editorial 등)
- 미사용 대상: 물리 삭제 대상 (IngestionJob 등 로그성 데이터)

---

## 엔티티 적용 패턴

```java
// ① 생성 + 수정만 필요한 엔티티 (예: UserEntity)
@Embedded private CreateAudit createAudit;
@Embedded private UpdateAudit updateAudit;

// ② 소프트 삭제까지 필요한 엔티티 (예: ReviewEntity)
@Embedded private CreateAudit     createAudit;
@Embedded private UpdateAudit     updateAudit;
@Embedded private SoftDeleteAudit softDeleteAudit;

// ③ 생성만 필요한 엔티티 (예: ActivityLogEntity)
@Embedded private CreateAudit createAudit;

// 엔티티 생성 시 초기화
entity.createAudit     = CreateAudit.now(currentUserId);
entity.updateAudit     = UpdateAudit.empty();
entity.softDeleteAudit = SoftDeleteAudit.active();
```

---

## `@MappedSuperclass` 대비 장점

| | `@MappedSuperclass` | `@Embeddable` |
|---|---|---|
| **선택적 조합** | 불가 (전부 상속 강제) | 가능 (필요한 것만) |
| **소프트 삭제** | 별도 처리 필요 | `SoftDeleteAudit`으로 명시적 표현 |
| **createdBy/updatedBy** | `AuditorAware` 별도 구현 | 자연스럽게 포함 |
| **로직 캡슐화** | 불가 | `touch()`, `softDelete()` 등 행위 포함 |

---

## 의존 규칙

- **허용** : `jakarta.persistence.*`, `java.time.OffsetDateTime`
- **금지** : `domain.*`, `application.*`, `presentation.*`, 특정 Bounded Context