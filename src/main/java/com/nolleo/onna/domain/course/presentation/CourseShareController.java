package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.SharedCourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseShareService;
import com.nolleo.onna.domain.course.presentation.dto.request.UpdateCourseVisibilityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Share", description = "코스 공유 API — 공개 전환 및 공유 링크 조회")
public class CourseShareController {

    private final CourseShareService courseShareService;

    @PatchMapping("/{courseId}/visibility")
    @Operation(
            summary = "코스 공개 상태 전환",
            description = """
                    코스를 공개 또는 비공개로 전환한다. 로그인이 필요하며 본인이 생성한 코스만 가능하다.
                    - 목표 상태를 isPublic으로 명시한다. 토글이 아니라 같은 요청을 반복해도 상태가 뒤집히지 않는다.
                    - 공개 전환 시 공유 토큰이 발급되어 응답의 share.shareToken으로 내려간다.
                      공유 링크는 GET /courses/shared/{shareToken} 이다.
                    - 비공개로 돌려도 토큰·좋아요·조회수는 보존된다. 다시 공개하면 같은 링크가 살아난다.
                    - courseId는 GET /courses/{pairId} 응답의 id(숫자)이며 pairId(UUID)가 아니다.
                    """
    )
    public ResponseEntity<ApiResponseDto<CourseResponse>> updateVisibility(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long courseId,
            @Valid @RequestBody UpdateCourseVisibilityRequest request
    ) {
        CourseResponse data = courseShareService.updateVisibility(request.toCommand(courseId, principal.userId()));
        return ApiResponseDto.success(200, "코스 공개 상태 변경 성공", data);
    }

    @GetMapping("/shared/{shareToken}")
    @Operation(
            summary = "공유 링크로 공개 코스 조회",
            description = """
                    공유 토큰으로 공개 코스를 조회한다. 로그인 없이 접근할 수 있다.
                    - 조회할 때마다 조회수가 1 증가한다.
                    - 작성자는 닉네임으로만 노출되며 userId · pairId · 토큰은 응답에 담기지 않는다.
                    - 토큰이 없거나, 코스가 비공개이거나, 삭제된 경우 모두 404 COURSE_NOT_FOUND 로 응답한다.
                      세 경우를 구분하지 않아 코스의 존재 여부를 노출하지 않는다.
                    """
    )
    public ResponseEntity<ApiResponseDto<SharedCourseResponse>> getShared(
            @PathVariable String shareToken
    ) {
        SharedCourseResponse data = courseShareService.getShared(shareToken);
        return ApiResponseDto.success(200, "공유 코스 조회 성공", data);
    }
}
