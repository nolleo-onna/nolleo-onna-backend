// [Audit] 생성 감사 정보 — created_at(생성 일시), created_by(생성자)를 엔티티에 @Embedded로 주입.
// updatable = false로 최초 세팅 후 DB 업데이트 차단. now(actor) 팩토리로 생성 시점에 한 번만 세팅.
package com.nolleo.onna.common.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.OffsetDateTime;

@Embeddable
public class CreateAudit {

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 50, updatable = false)
    private String createdBy;

    protected CreateAudit() {}

    private CreateAudit(OffsetDateTime createdAt, String createdBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static CreateAudit now(String createdBy) {
        return new CreateAudit(OffsetDateTime.now(), createdBy);
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy()         { return createdBy; }
}