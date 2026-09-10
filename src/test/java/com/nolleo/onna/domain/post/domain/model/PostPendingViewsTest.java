package com.nolleo.onna.domain.post.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostPendingViewsTest {

    private static Post postWithViews(int viewCount) {
        return Post.restore(1L, 1L, "제목", "내용", List.of(), List.of(), null,
                0, 0, viewCount, OffsetDateTime.now(), null);
    }

    @Test
    @DisplayName("applyPendingViews는 DB 미반영 조회수를 표시 조회수에 더한다")
    void applyPendingViews_addsPendingToViewCount() {
        Post post = postWithViews(10);

        post.applyPendingViews(3);

        assertThat(post.getViewCount()).isEqualTo(13);
    }

    @Test
    @DisplayName("대기분이 0이면 조회수가 그대로다")
    void applyPendingViews_keepsViewCount_whenNoPending() {
        Post post = postWithViews(10);

        post.applyPendingViews(0);

        assertThat(post.getViewCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("음수 대기분은 거부한다")
    void applyPendingViews_throws_whenNegative() {
        Post post = postWithViews(10);

        assertThatThrownBy(() -> post.applyPendingViews(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
