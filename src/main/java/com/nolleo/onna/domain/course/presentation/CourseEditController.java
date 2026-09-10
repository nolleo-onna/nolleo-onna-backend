package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseEditService;
import com.nolleo.onna.domain.course.presentation.dto.request.UpdateCourseItemsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Edit", description = "코스 수정 API")
public class CourseEditController {

    private final CourseEditService courseEditService;

    @PutMapping("/{courseId}/items")
    @Operation(
            summary = "코스 방문 스팟 목록 일괄 수정",
            description = """
                    편집을 마친 최종 방문 스팟 리스트를 한 번에 반영한다 (Full State Replacement).
                    - 추가·삭제·순서 변경을 클라이언트에서 모두 끝낸 뒤 저장 시 1회 호출한다.
                    - 순번은 보내지 않는다. 배열 순서가 곧 방문 순번(1부터)이 된다.
                    - 순번·이전 지점 거리·예상 비용·총비용은 서버가 전부 재계산해 응답에 담는다.
                      저장 후 재조회 없이 이 응답으로 화면을 갱신하면 된다.
                    - courseId는 GET /courses/{pairId} 응답의 id(숫자)이며 pairId(UUID)가 아니다.
                    - 현재는 placeType=SPOT만 허용한다. FOOD는 COURSE_PLACE_TYPE_NOT_SUPPORTED로 거부된다.
                    - 존재하지 않거나 비활성인 장소, 좌표가 없는 장소가 하나라도 섞이면 COURSE_PLACE_NOT_FOUND로 전체를 거부한다.
                    - 같은 코스를 거의 동시에 저장해 충돌하면 409 CONCURRENT_MODIFICATION을 받는다. 먼저 반영된 저장은 유지되므로 재조회 후 다시 편집한다.
                    - 검증 실패 시 코스는 변경 전 상태 그대로 유지된다.
                    """
    )
    public ResponseEntity<ApiResponseDto<CourseResponse>> updateItems(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseItemsRequest request
    ) {
        CourseResponse data = courseEditService.updateItems(request.toCommand(courseId, principal.userId()));
        return ApiResponseDto.success(200, "코스 수정 성공", data);
    }
}
