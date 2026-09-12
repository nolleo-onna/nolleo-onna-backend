package com.nolleo.onna.domain.course.application.dto.response;

import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 공유 링크로 열람하는 공개 코스.
 *
 * 코스 id · 소유자 식별 정보(userId · pairId) · 공유 토큰은 담지 않는다 — 작성자는 닉네임으로만 노출한다.
 * 공개 영역의 식별자는 URL의 shareToken 하나다. 순차 증가하는 내부 id를 비로그인 사용자에게 주면
 * 코스 수가 드러나고, id로 받는 후속 API가 매번 공개 여부를 따로 검사해야 한다.
 * 후속 좋아요 · 가져오기도 /courses/shared/{shareToken} 하위 경로로 열어 조회 쿼리가 공개 여부를 강제하게 한다.
 */
@Schema(description = "공유 링크로 열람하는 공개 코스 — 작성자는 닉네임으로만 노출, 식별자는 URL의 shareToken")
public record SharedCourseResponse(

        @Schema(description = "코스 제목", example = "광안리 로맨틱 데이트 코스")
        String title,

        @Schema(description = "코스 소개 문구")
        String description,

        @Schema(description = "예상 총 비용 (원) — 음식점 미포함 시 null")
        Integer totalCost,

        @Schema(description = "방문 스팟 목록 (방문 순서대로)")
        List<CourseItemResponse> items,

        @Schema(description = "작성자 닉네임 — 탈퇴 등으로 조회 불가 시 null", example = "부산러버")
        String authorNickname,

        @Schema(description = "작성자 프로필 이미지 URL — 없으면 null")
        String authorProfileImageUrl,

        @Schema(description = "공유 링크 누적 조회수 (이번 조회 포함)", example = "43")
        int viewCount,

        @Schema(description = "좋아요 수", example = "7")
        int likeCount,

        @Schema(description = "내가 좋아요를 눌렀는지 — 비로그인이면 항상 false", example = "false")
        boolean likedByMe,

        @Schema(description = "생성 시각")
        OffsetDateTime createdAt

) {
    public static SharedCourseResponse of(Course course, Map<PlaceRef, SpotCandidate> spotByRef,
                                          UserProfile author, boolean likedByMe) {
        List<CourseItemResponse> items = course.getItems().stream()
                .map(item -> CourseItemResponse.of(item, spotByRef.get(item.getPlaceRef())))
                .toList();

        return new SharedCourseResponse(
                course.getTitle(),
                course.getDescription(),
                course.getTotalCost(),
                items,
                author != null ? author.nickname() : null,
                author != null ? author.profileImageUrl() : null,
                course.getShareInfo().viewCount(),
                course.getShareInfo().likeCount(),
                likedByMe,
                course.getCreatedAt()
        );
    }
}
