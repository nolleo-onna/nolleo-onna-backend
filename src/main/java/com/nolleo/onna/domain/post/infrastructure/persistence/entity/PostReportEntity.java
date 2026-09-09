package com.nolleo.onna.domain.post.infrastructure.persistence.entity;

import com.nolleo.onna.common.infrastructure.CreateAudit;
import com.nolleo.onna.domain.post.domain.model.PostReport;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pt_post_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pt_post_reports",
                columnNames = {"post_id", "reporter_id"}),
        indexes = @Index(name = "idx_pt_post_reports_post_id", columnList = "post_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Embedded
    private CreateAudit createAudit;

    public static PostReportEntity from(PostReport domain) {
        PostReportEntity entity = new PostReportEntity();
        entity.postId = domain.postId();
        entity.reporterId = domain.reporterId();
        entity.reason = domain.reason();
        entity.createAudit = CreateAudit.now(domain.reporterId().toString());
        return entity;
    }

    public PostReport toDomain() {
        return new PostReport(id, postId, reporterId, reason,
                createAudit != null ? createAudit.getCreatedAt() : null);
    }
}
