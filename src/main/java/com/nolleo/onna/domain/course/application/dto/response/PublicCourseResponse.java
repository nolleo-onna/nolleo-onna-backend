package com.nolleo.onna.domain.course.application.dto.response;

import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 공개 코스 목록 카드 — 홈 인기 코스 · 공개 코스 전체보기. 로그인 없이 내려간다.
 * 공개 영역의 식별자는 shareToken 하나다. 코스 id · userId · pairId는 SharedCourseResponse와 같은 이유로 담지 않는다.
 */
@Schema(description = "공개 코스 목록 카드 — 카드 클릭은 shareToken으로 공유 페이지에 연결한다")
public record PublicCourseResponse(

        @Schema(description = "공유 링크 토큰 — GET /courses/shared/{shareToken} 에 사용",
                example = "Qm9vay1zaGFyZS10b2tlbi1leGFtcGxl")
        String shareToken,

        @Schema(description = "코스 제목", example = "광안리 로맨틱 데이트 코스")
        String title,

        @Schema(description = "코스 소개 문구")
        String description,

        @Schema(description = "예상 총 비용 (원) — 음식점 미포함 시 null")
        Integer totalCost,

        @Schema(description = "방문 순서대로의 스팟 이름 목록", example = "[\"광안리해수욕장\", \"OO카페\"]")
        List<String> spotTitles,

        @Schema(description = "첫 스팟 대표 이미지 URL — 없으면 null")
        String thumbnailImageUrl,

        @Schema(description = "작성자 닉네임 — 탈퇴했으면 null", example = "부산러버")
        String authorNickname,

        @Schema(description = "작성자 프로필 이미지 URL — 없거나 탈퇴했으면 null")
        String authorProfileImageUrl,

        @Schema(description = "조회수 — DB 반영값 (버퍼 대기분은 최대 5분 뒤 반영)", example = "42")
        int viewCount,

        @Schema(description = "좋아요 수", example = "7")
        int likeCount,

        @Schema(description = "생성 시각")
        OffsetDateTime createdAt

) {
    public static PublicCourseResponse of(Course course, Map<PlaceRef, SpotCandidate> spotByRef, UserProfile author) {
        List<SpotCandidate> spots = course.getItems().stream()
                .map(CourseItem::getPlaceRef)
                .map(spotByRef::get)
                .toList();

        List<String> spotTitles = spots.stream()
                .map(spot -> spot != null ? spot.title() : null)
                .toList();

        String thumbnail = spots.stream()
                .filter(Objects::nonNull)
                .map(SpotCandidate::firstImage)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);

        return new PublicCourseResponse(
                course.getShareInfo().shareToken(),
                course.getTitle(),
                course.getDescription(),
                course.getTotalCost(),
                spotTitles,
                thumbnail,
                author != null ? author.nickname() : null,
                author != null ? author.profileImageUrl() : null,
                course.getShareInfo().viewCount(),
                course.getShareInfo().likeCount(),
                course.getCreatedAt()
        );
    }
}
