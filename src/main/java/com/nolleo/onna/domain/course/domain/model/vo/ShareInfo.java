package com.nolleo.onna.domain.course.domain.model.vo;

/** 코스 공개 공유 상태 묶음 */
public record ShareInfo(
        /** 코스 공개 여부 */
        boolean isPublic,
        /** 공유 URL 토큰 — isPublic=true 일 때만 유효한 값을 가짐 */
        String shareToken,
        /** 공유 코스 누적 조회수 */
        int viewCount
) {
    public static ShareInfo of(boolean isPublic, String shareToken, int viewCount) {
        return new ShareInfo(isPublic, shareToken, viewCount);
    }
}
