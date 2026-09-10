package com.nolleo.onna.domain.course.presentation.dto.request;

import com.nolleo.onna.domain.course.application.dto.UpdateCourseItemsCommand;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
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
 * 코스 방문 스팟 목록 일괄 반영 요청 (Full State Replacement).
 *
 * 편집이 끝난 최종 리스트를 그대로 보낸다. 순번은 담지 않는다 —
 * 배열에서의 위치가 곧 방문 순번이 되므로, 추가·삭제·순서 변경으로 생기는
 * 순번 밀림은 클라이언트의 배열 조작에서 이미 반영된 상태다.
 */
@Schema(description = "코스 방문 스팟 목록 일괄 반영 요청 — 편집을 마친 최종 리스트")
public record UpdateCourseItemsRequest(

        @Schema(description = "방문할 장소 목록. 배열 순서가 곧 방문 순서(1번부터)이며, 같은 장소를 두 번 담을 수 없다.")
        @NotNull(message = "items는 필수입니다.")
        @NotEmpty(message = "코스에는 최소 1개의 방문 스팟이 필요합니다.")
        @Size(max = 15, message = "방문 스팟은 최대 15개까지 담을 수 있습니다.")
        @Valid
        List<Item> items
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

    public UpdateCourseItemsCommand toCommand(Long courseId, Long userId) {
        return new UpdateCourseItemsCommand(
                courseId,
                userId,
                items.stream().map(Item::toPlaceRef).toList()
        );
    }
}
