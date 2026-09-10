package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseEditService;
import com.nolleo.onna.domain.course.presentation.dto.request.UpdateCourseRequest;
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

    @PutMapping("/{courseId}")
    @Operation(
            summary = "코스 수정 (제목·소개·방문 스팟 일괄 반영)",
            description = """
                    편집을 마친 제목 · 소개 · 방문 스팟 목록을 한 번에 반영한다 (Full State Replacement).
                    - 편집을 클라이언트에서 모두 끝낸 뒤 저장 시 1회 호출한다. 바뀌지 않은 필드도 현재 값을 담아 보낸다.
                    - title은 필수이며 앞뒤 공백은 제거된다. description은 선택이며, 비우거나 생략하면 소개가 지워진다.
                      최대 길이는 요청 스키마를 따르고, 초과 시 400이다.
                    - 방문 스팟 순번은 보내지 않는다. 배열 순서가 곧 방문 순번(1부터)이 된다.
                    - 순번·이전 지점 거리·예상 비용·총비용은 서버가 전부 재계산해 응답에 담는다.
                      저장 후 재조회 없이 이 응답으로 화면을 갱신하면 된다.
                    - courseId는 GET /courses/{pairId} 응답의 id(숫자)이며 pairId(UUID)가 아니다.
                    - 현재는 placeType=SPOT만 허용한다. FOOD는 COURSE_PLACE_TYPE_NOT_SUPPORTED로 거부된다.
                    - 존재하지 않거나 비활성인 장소, 좌표가 없는 장소가 하나라도 섞이면 COURSE_PLACE_NOT_FOUND로 전체를 거부한다.
                    - 같은 코스를 거의 동시에 저장해 충돌하면 409 CONCURRENT_MODIFICATION을 받는다. 먼저 반영된 저장은 유지되므로 재조회 후 다시 편집한다.
                    - 검증 실패 시 코스는 변경 전 상태 그대로 유지된다. 제목·소개·스팟 중 하나라도 실패하면 아무것도 반영되지 않는다.
                    """
    )
    public ResponseEntity<ApiResponseDto<CourseResponse>> updateCourse(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseRequest request
    ) {
        CourseResponse data = courseEditService.updateCourse(request.toCommand(courseId, principal.userId()));
        return ApiResponseDto.success(200, "코스 수정 성공", data);
    }
}
