package com.nolleo.onna.domain.course.presentation.dto.request;

import com.nolleo.onna.domain.course.application.dto.CreateCourseCommand;
import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 폼 기반 코스 생성 요청 — 지역 · 예산 등급 · 꼭 포함할 장소명 · 축제명.
 * 장소명·축제명은 데이터 제목과 정확히 일치하거나 하나만 걸릴 때 반영되고, 못 찾으면 응답의 unmatched로 알린다.
 */
@Schema(description = "폼 기반 코스 생성 요청")
public record CreateCourseRequest(

        @Schema(description = "시작 지역 — 지원 지역명 중 하나", example = "광안리")
        @NotBlank(message = "시작 지역은 필수입니다.")
        String startArea,

        @Schema(description = "예산 등급. 생략하면 UNLIMITED", example = "UNDER_30K",
                allowableValues = {"NONE", "UNDER_10K", "UNDER_30K", "UNDER_50K", "UNLIMITED"})
        BudgetTier budget,

        @Schema(description = "꼭 포함할 장소명 목록 (최대 5개)", example = "[\"광안리해수욕장\", \"해운대해수욕장\"]")
        @Size(max = MAX_INCLUDE_SPOTS, message = "꼭 포함할 장소는 최대 {max}개까지 지정할 수 있습니다.")
        List<@NotBlank(message = "장소명은 비어 있을 수 없습니다.")
             @Size(max = MAX_NAME_LENGTH, message = "장소명은 최대 {max}자까지 입력할 수 있습니다.") String> includeSpots,

        @Schema(description = "축제(행사)명 — 찾으면 축제 위치가 검색 중심이 되고 시작 지역도 그 위치로 바뀐다", example = "부산불꽃축제")
        @Size(max = MAX_NAME_LENGTH, message = "축제명은 최대 {max}자까지 입력할 수 있습니다.")
        String festival
) {
    public static final int MAX_INCLUDE_SPOTS = 5;
    public static final int MAX_NAME_LENGTH = 50;

    public CreateCourseCommand toCommand(Long userId) {
        return new CreateCourseCommand(userId, startArea, budget, includeSpots, festival);
    }
}
