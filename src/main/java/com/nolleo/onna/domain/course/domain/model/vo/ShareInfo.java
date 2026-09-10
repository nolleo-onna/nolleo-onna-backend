package com.nolleo.onna.domain.course.domain.model.vo;

import java.util.function.Supplier;

/**
 * 코스 공개 공유 상태 묶음 — 값 객체. 상태 전환은 새 인스턴스를 돌려준다.
 *
 * 토큰 정책: 최초 공개 때 한 번 발급되면 이후 비공개·재공개를 거쳐도 유지된다.
 * 재공개 시 같은 링크가 다시 살아나고, 좋아요·조회수도 그대로 이어진다.
 * 비공개 상태에서는 토큰이 남아 있어도 조회 경로가 is_public=true만 허용하므로 열리지 않는다.
 */
public record ShareInfo(
        /** 코스 공개 여부 */
        boolean isPublic,
        /** 공유 URL 토큰 — 한 번도 공개한 적 없으면 null */
        String shareToken,
        /** 공유 코스 누적 조회수 */
        int viewCount,
        /** 좋아요 수 — generated_course_likes 행 수의 역정규화 값 */
        int likeCount
) {
    public static ShareInfo of(boolean isPublic, String shareToken, int viewCount, int likeCount) {
        return new ShareInfo(isPublic, shareToken, viewCount, likeCount);
    }

    /** 생성 직후 초기 상태 — 비공개, 토큰 없음, 조회수·좋아요 0 */
    public static ShareInfo initial() {
        return new ShareInfo(false, null, 0, 0);
    }

    /**
     * 공개로 전환한다. 이미 공개면 상태 불변(멱등).
     * 토큰이 이미 있으면 유지하고, 없을 때만 tokenSupplier를 호출해 발급한다 — 불필요한 생성을 피한다.
     */
    public ShareInfo publish(Supplier<String> tokenSupplier) {
        if (isPublic) return this;
        String token = shareToken != null ? shareToken : tokenSupplier.get();
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("공유 토큰이 필요합니다.");
        }
        return new ShareInfo(true, token, viewCount, likeCount);
    }

    /** 비공개로 전환한다. 토큰·조회수·좋아요 수는 보존한다 — 재공개 시 그대로 이어진다. 이미 비공개면 불변. */
    public ShareInfo unpublish() {
        if (!isPublic) return this;
        return new ShareInfo(false, shareToken, viewCount, likeCount);
    }

    /** 공유 링크 열람 1회를 메모리 값에 반영한다. DB 카운터는 원자 UPDATE로 따로 증가한다. */
    public ShareInfo viewed() {
        return new ShareInfo(isPublic, shareToken, viewCount + 1, likeCount);
    }
}
