package com.nolleo.onna.common.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.OffsetDateTime;

@Embeddable
public class SoftDeleteAudit {

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by", length = 50)
    private String deletedBy;

    protected SoftDeleteAudit() {}

    public static SoftDeleteAudit active() {
        return new SoftDeleteAudit();
    }

    public void softDelete(String deletedBy) {
        if (this.deletedAt != null) return;  // 이미 삭제된 경우 무시
        this.deletedAt = OffsetDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public String getDeletedBy()         { return deletedBy; }
}