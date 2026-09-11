package com.nolleo.onna.domain.course.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareInfoTest {

    @Test
    @DisplayName("초기 상태는 비공개·토큰 없음·조회수 0·좋아요 0이다")
    void initial_state() {
        ShareInfo share = ShareInfo.initial();

        assertThat(share.isPublic()).isFalse();
        assertThat(share.shareToken()).isNull();
        assertThat(share.viewCount()).isZero();
        assertThat(share.likeCount()).isZero();
    }

    @Test
    @DisplayName("publish는 토큰이 없으면 공급자에서 받아 공개 상태가 되고, 카운터는 유지된다")
    void publish_takesTokenFromSupplier_whenAbsent() {
        ShareInfo share = ShareInfo.of(false, null, 5, 2);

        ShareInfo published = share.publish(() -> "abc");

        assertThat(published.isPublic()).isTrue();
        assertThat(published.shareToken()).isEqualTo("abc");
        assertThat(published.viewCount()).isEqualTo(5);
        assertThat(published.likeCount()).isEqualTo(2);
        assertThat(share.isPublic()).isFalse(); // 값 객체 — 원본 불변
    }

    @Test
    @DisplayName("publish는 토큰이 이미 있으면 공급자를 호출하지 않고 기존 토큰을 유지한다")
    void publish_keepsExistingToken_withoutCallingSupplier() {
        ShareInfo share = ShareInfo.of(false, "old", 0, 0);

        ShareInfo published = share.publish(() -> { throw new AssertionError("호출되면 안 된다"); });

        assertThat(published.shareToken()).isEqualTo("old");
        assertThat(published.isPublic()).isTrue();
    }

    @Test
    @DisplayName("이미 공개면 publish는 같은 인스턴스를 돌려준다 (멱등)")
    void publish_returnsSelf_whenAlreadyPublic() {
        ShareInfo share = ShareInfo.of(true, "tok", 0, 0);

        assertThat(share.publish(() -> "new")).isSameAs(share);
    }

    @Test
    @DisplayName("공급자가 빈 토큰을 주면 공개 전환할 수 없다")
    void publish_throws_whenTokenBlank() {
        ShareInfo share = ShareInfo.initial();

        assertThatThrownBy(() -> share.publish(() -> " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> share.publish(() -> null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("unpublish는 공개만 풀고 토큰·조회수·좋아요는 그대로 둔다. 이미 비공개면 같은 인스턴스")
    void unpublish_preservesTokenAndCounters() {
        ShareInfo share = ShareInfo.of(true, "tok", 10, 3);

        ShareInfo unpublished = share.unpublish();

        assertThat(unpublished.isPublic()).isFalse();
        assertThat(unpublished.shareToken()).isEqualTo("tok");
        assertThat(unpublished.viewCount()).isEqualTo(10);
        assertThat(unpublished.likeCount()).isEqualTo(3);
        assertThat(unpublished.unpublish()).isSameAs(unpublished);
    }

    @Test
    @DisplayName("withPendingViews는 대기 조회수를 더한 새 상태를 돌려주고, 대기분이 0이면 같은 인스턴스다")
    void withPendingViews_addsPendingToViewCount() {
        ShareInfo share = ShareInfo.of(true, "tok", 10, 3);

        ShareInfo displayed = share.withPendingViews(5);

        assertThat(displayed.viewCount()).isEqualTo(15);
        assertThat(displayed.likeCount()).isEqualTo(3);
        assertThat(displayed.isPublic()).isTrue();
        assertThat(displayed.shareToken()).isEqualTo("tok");
        assertThat(share.viewCount()).isEqualTo(10); // 값 객체 — 원본 불변
        assertThat(share.withPendingViews(0)).isSameAs(share);
    }

    @Test
    @DisplayName("withPendingViews는 음수 대기분을 거부한다")
    void withPendingViews_throws_whenNegative() {
        assertThatThrownBy(() -> ShareInfo.initial().withPendingViews(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
