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
 * 소유자 식별 정보(userId · pairId)와 공유 토큰은 담지 않는다 — 작성자는 닉네임으로만 노출한다.
 */
@Schema(description = "공유 링크로 열람하는 공개 코스 — 작성자는 닉네임으로만 노출")
public record SharedCourseResponse(

        @Schema(description = "코스 ID — 좋아요 등 후속 동작의 대상 식별자", example = "1")
        Long id,

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

        @Schema(description = "생성 시각")
        OffsetDateTime createdAt

) {
    public static SharedCourseResponse of(Course course, Map<PlaceRef, SpotCandidate> spotByRef, UserProfile author) {
        List<CourseItemResponse> items = course.getItems().stream()
                .map(item -> CourseItemResponse.of(item, spotByRef.get(item.getPlaceRef())))
                .toList();

        return new SharedCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getTotalCost(),
                items,
                author != null ? author.nickname() : null,
                author != null ? author.profileImageUrl() : null,
                course.getShareInfo() != null ? course.getShareInfo().viewCount() : 0,
                course.getShareInfo() != null ? course.getShareInfo().likeCount() : 0,
                course.getCreatedAt()
        );
    }
}
