package com.nolleo.onna.domain.course.application.dto.response;

import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * 폼 기반 코스 생성 응답 — 생성된 코스 본문 + 요청 중 무엇을 반영했고 무엇을 못 찾았는지.
 * 폼은 되묻기가 없으므로 매칭 결과를 여기서 알린다. 못 찾은 항목은 생성을 막지 않는다.
 */
@Schema(description = "폼 기반 코스 생성 결과")
public record CreateCourseResponse(

        @Schema(description = "생성된 코스 묶음 UUID — GET /courses/{pairId}로 다시 조회할 수 있다")
        UUID pairId,

        @Schema(description = "생성된 코스 상세 (조회 응답과 동일 구조)")
        List<CourseResponse> courses,

        @Schema(description = "요청 중 실제로 반영된 것")
        Applied applied,

        @Schema(description = "요청했지만 데이터에서 찾지 못해 반영하지 않은 것")
        Unmatched unmatched
) {

    @Schema(description = "반영된 조건")
    public record Applied(
            @Schema(description = "최종 시작 지역 — 축제를 찾았으면 축제 위치의 지역으로 바뀔 수 있다", example = "광안리")
            String startArea,
            Budget budget,
            @Schema(description = "코스에 담은 꼭 포함 장소")
            List<MatchedSpot> includeSpots,
            @Schema(description = "검색 중심이 된 축제 — 못 찾았으면 null")
            MatchedFestival festival
    ) {}

    @Schema(description = "예산 반영 결과")
    public record Budget(
            BudgetTier tier,
            @Schema(description = "예산 상한 안에서 식사·카페를 다 채우지 못해 필터를 풀고 채웠는지")
            boolean filterRelaxed
    ) {}

    @Schema(description = "매칭된 장소")
    public record MatchedSpot(
            @Schema(description = "사용자가 입력한 이름", example = "광안리 바다") String name,
            @Schema(description = "데이터에서 찾은 제목", example = "광안리해수욕장") String matchedTitle,
            String contentId
    ) {}

    @Schema(description = "매칭된 축제")
    public record MatchedFestival(
            @Schema(description = "사용자가 입력한 이름", example = "부산불꽃축제") String name,
            @Schema(description = "데이터에서 찾은 제목", example = "제20회 부산불꽃축제") String matchedTitle,
            @Schema(description = "행사 기간", example = "11.1~11.1") String period
    ) {}

    @Schema(description = "찾지 못한 조건")
    public record Unmatched(
            @Schema(description = "찾지 못한 꼭 포함 장소명") List<String> includeSpots,
            @Schema(description = "찾지 못한 축제명 — 없으면 null") String festival
    ) {}

    public static CreateCourseResponse of(UUID pairId, List<CourseResponse> courses, CourseIntent intent,
                                          BudgetTier tier, boolean filterRelaxed) {
        List<MatchedSpot> matched = intent.includeSpots().stream()
                .filter(SpotPin::isResolved)
                .map(pin -> new MatchedSpot(pin.name(), pin.matchedTitle(), pin.contentId()))
                .toList();
        List<String> unmatchedSpots = intent.includeSpots().stream()
                .filter(pin -> !pin.isResolved())
                .map(SpotPin::name)
                .toList();

        CourseAnchor anchor = intent.anchor();
        MatchedFestival festival = anchor != null && anchor.isResolved()
                ? new MatchedFestival(anchor.name(), anchor.matchedTitle(), anchor.period())
                : null;
        String unmatchedFestival = anchor != null && !anchor.isResolved() ? anchor.name() : null;

        return new CreateCourseResponse(
                pairId,
                courses,
                new Applied(intent.startArea(), new Budget(tier, filterRelaxed), matched, festival),
                new Unmatched(unmatchedSpots, unmatchedFestival)
        );
    }
}
