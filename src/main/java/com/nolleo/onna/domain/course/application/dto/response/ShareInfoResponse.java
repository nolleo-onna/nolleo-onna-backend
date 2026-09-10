package com.nolleo.onna.domain.course.application.dto.response;

import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 공개 공유 상태 — 소유자에게만 내려간다")
public record ShareInfoResponse(

        @Schema(description = "공개 여부", example = "true")
        boolean isPublic,

        @Schema(description = "공유 링크 토큰 — GET /courses/shared/{shareToken} 에 사용. 한 번도 공개한 적 없으면 null. 비공개로 돌려도 유지되어 재공개 시 같은 링크가 살아난다",
                example = "Qm9vay1zaGFyZS10b2tlbi1leGFtcGxl")
        String shareToken,

        @Schema(description = "공유 링크 누적 조회수", example = "42")
        int viewCount,

        @Schema(description = "좋아요 수", example = "7")
        int likeCount

) {
    public static ShareInfoResponse from(ShareInfo share) {
        if (share == null) {
            return new ShareInfoResponse(false, null, 0, 0);
        }
        return new ShareInfoResponse(share.isPublic(), share.shareToken(), share.viewCount(), share.likeCount());
    }
}
