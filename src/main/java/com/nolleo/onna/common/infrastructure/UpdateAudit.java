// [Audit] 수정 감사 정보 — updated_at(수정 일시), updated_by(수정자)를 엔티티에 @Embedded로 주입.
// empty()로 초기 생성 후 touch(actor) 호출 시 현재 시각으로 갱신.
package com.nolleo.onna.common.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.OffsetDateTime;

@Embeddable
public class UpdateAudit {

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    protected UpdateAudit() {}

    public static UpdateAudit now() {
        UpdateAudit a = new UpdateAudit();
        a.updatedAt = OffsetDateTime.now();
        return a;
    }

    public void touch(String updatedBy) {
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy()         { return updatedBy; }
}