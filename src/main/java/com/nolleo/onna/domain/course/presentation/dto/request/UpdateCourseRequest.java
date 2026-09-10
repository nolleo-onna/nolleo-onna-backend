package com.nolleo.onna.domain.course.presentation.dto.request;

import com.nolleo.onna.domain.course.application.dto.UpdateCourseCommand;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 코스 수정 요청 — 편집을 마친 제목 · 소개 · 방문 스팟 목록을 한 번에 보낸다 (Full State Replacement).
 *
 * 바뀌지 않은 필드도 현재 값을 담아 보낸다. 소개를 비우거나 생략하면 소개가 지워진다(null).
 * 방문 스팟은 순번을 담지 않는다 — 배열에서의 위치가 곧 방문 순번이 되므로, 추가·삭제·순서 변경으로 생기는
 * 순번 밀림은 클라이언트의 배열 조작에서 이미 반영된 상태다.
 */
@Schema(description = "코스 수정 요청 — 편집을 마친 제목·소개·방문 스팟 목록")
public record UpdateCourseRequest(

        @Schema(description = "코스 제목. 앞뒤 공백은 제거된다.", example = "광안리 바다 산책 코스")
        @NotBlank(message = "코스 제목은 필수입니다.")
        @Size(max = Course.MAX_TITLE_LENGTH, message = "코스 제목은 최대 {max}자까지 입력할 수 있습니다.")
        String title,

        @Schema(description = "코스 소개. 선택이며, 비우거나 생략하면 소개가 지워진다(null).", example = "바다를 따라 걷고 카페에서 쉬어 가는 코스입니다.")
        @Size(max = Course.MAX_DESCRIPTION_LENGTH, message = "코스 소개는 최대 {max}자까지 입력할 수 있습니다.")
        String description,

        @Schema(description = "방문할 장소 목록. 배열 순서가 곧 방문 순서(1번부터)이며, 같은 장소를 두 번 담을 수 없다.")
        @NotEmpty(message = "코스에는 최소 1개의 방문 스팟이 필요합니다.")
        @Size(max = CoursePlaces.MAX_ITEMS, message = "방문 스팟은 최대 {max}개까지 담을 수 있습니다.")
        List<@NotNull(message = "items에 null 원소를 담을 수 없습니다.") @Valid Item> items
) {

    @Schema(description = "방문 장소 — Map API의 placeType / originalId를 그대로 전달")
    public record Item(

            @Schema(description = "장소 타입. 현재는 SPOT만 허용", example = "SPOT", allowableValues = {"SPOT", "FOOD"})
            @NotBlank(message = "placeType은 필수입니다.")
            @Pattern(regexp = "SPOT|FOOD", message = "placeType은 SPOT 또는 FOOD여야 합니다.")
            String placeType,

            @Schema(description = "원본 식별자. SPOT → sp_spots.content_id", example = "2760699")
            @NotBlank(message = "originalId는 필수입니다.")
            String originalId
    ) {
        PlaceRef toPlaceRef() {
            return new PlaceRef(CoursePlaceType.valueOf(placeType), originalId);
        }
    }

    public UpdateCourseCommand toCommand(Long courseId, Long userId) {
        return new UpdateCourseCommand(
                courseId,
                userId,
                title,
                description,
                items.stream().map(Item::toPlaceRef).toList()
        );
    }
}
