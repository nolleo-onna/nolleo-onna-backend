package com.nolleo.onna.domain.course.domain.model.vo;

/** 코스 공개 공유 상태 묶음 */
public record ShareInfo(
        /** 코스 공개 여부 */
        boolean isPublic,
        /** 공유 URL 토큰 — isPublic=true 일 때만 유효한 값을 가짐 */
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
}
