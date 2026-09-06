package com.nolleo.onna.domain.post.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pt_post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    public PostImageEntity(PostEntity post, String imageUrl, short displayOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}
