package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.ViewerKeyResolver;
import com.nolleo.onna.domain.course.application.dto.response.CourseLikeToggleResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.PublicCourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.SharedCourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseLikeService;
import com.nolleo.onna.domain.course.application.service.CourseQueryService;
import com.nolleo.onna.domain.course.application.service.CourseShareService;
import com.nolleo.onna.domain.course.domain.model.vo.CourseSort;
import com.nolleo.onna.domain.course.presentation.dto.request.UpdateCourseVisibilityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Share", description = "코스 공유 API — 공개 전환 및 공유 링크 조회")
public class CourseShareController {

    private final CourseShareService courseShareService;
    private final CourseLikeService courseLikeService;
    private final CourseQueryService courseQueryService;

    /** 공개 코스 목록 페이지 크기 상한 — 게시글 목록(50)과 같은 값 */
    static final int MAX_PUBLIC_PAGE_SIZE = 50;

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

    @GetMapping("/popular")
    @Operation(
            summary = "공개 코스 목록 조회 (최신순 · 좋아요순 · 조회수순)",
            description = """
                    공개로 전환된 코스 목록을 조회한다. 로그인 없이 접근할 수 있다. 홈 인기 코스와 전체보기가 함께 쓴다.
                    - sort: VIEWS(기본, 조회수순) · LIKES(좋아요순) · LATEST(최신순). 값이 같으면 최신순 → id 내림차순.
                      대소문자를 구분하며 그 외 값은 400.
                    - page(0부터) · size(기본 6, 최대 50)로 넘겨본다. 응답 개수가 size보다 작으면 마지막 페이지다.
                    - 카드 클릭은 응답의 shareToken으로 GET /courses/shared/{shareToken} 에 연결한다.
                      코스 id · userId · pairId는 담기지 않는다 — 공개 영역의 식별자는 shareToken 하나다.
                    - viewCount는 DB 반영값이다. 조회 직후의 대기분은 최대 5분 뒤 반영된다.
                    - 비공개·삭제된 코스는 포함되지 않는다. 탈퇴한 작성자는 닉네임·프로필이 null로 내려간다.
                    """
    )
    public ResponseEntity<ApiResponseDto<List<PublicCourseResponse>>> getPublicCourses(
            @RequestParam(defaultValue = "VIEWS") CourseSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        int clampedPage = Math.max(page, 0);
        int clampedSize = Math.max(1, Math.min(size, MAX_PUBLIC_PAGE_SIZE));
        List<PublicCourseResponse> data = courseQueryService.getPublicCourses(sort, clampedPage, clampedSize);
        return ApiResponseDto.success(200, "공개 코스 목록 조회 성공", data);
    }

    @GetMapping("/shared/{shareToken}")
    @Operation(
            summary = "공유 링크로 공개 코스 조회",
            description = """
                    공유 토큰으로 공개 코스를 조회한다. 로그인 없이 접근할 수 있다.
                    - 같은 사람(로그인: 회원, 비로그인: IP 기준)의 조회는 10분에 1회만 조회수에 집계된다.
                    - 응답의 viewCount는 이번 조회까지 반영된 값이다. DB에는 주기적으로(기본 5분) 일괄 반영된다.
                    - 작성자는 닉네임으로만 노출되며 코스 id · userId · pairId · 토큰은 응답에 담기지 않는다.
                      공개 영역의 식별자는 URL의 shareToken 하나다.
                    - 토큰이 없거나, 코스가 비공개이거나, 삭제된 경우 모두 404 COURSE_NOT_FOUND 로 응답한다.
                      세 경우를 구분하지 않아 코스의 존재 여부를 노출하지 않는다.
                    """
    )
    public ResponseEntity<ApiResponseDto<SharedCourseResponse>> getShared(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String shareToken,
            HttpServletRequest request
    ) {
        String viewerKey = ViewerKeyResolver.resolve(principal, request);
        Long viewerUserId = principal != null ? principal.userId() : null;
        SharedCourseResponse data = courseShareService.getShared(shareToken, viewerKey, viewerUserId);
        return ApiResponseDto.success(200, "공유 코스 조회 성공", data);
    }

    @PostMapping("/shared/{shareToken}/likes/toggle")
    @Operation(
            summary = "공유 코스 좋아요 토글",
            description = """
                    공유 링크로 열람 중인 공개 코스에 좋아요를 누르거나 취소한다. 로그인이 필요하다.
                    - 사용자당 코스 1회. 이미 눌렀으면 취소되고, 아니면 추가된다.
                    - 코스 소유자 본인도 누를 수 있다.
                    - 응답 likeCount는 DB에 반영된 값이며, liked는 토글 후 내 상태다.
                    - 같은 요청이 거의 동시에 두 번 들어와도 한 번만 반영되고 두 응답 모두 반영된 상태를 돌려준다.
                    - 토큰이 없거나 코스가 비공개·삭제된 경우 404 COURSE_NOT_FOUND. 비공개로 돌려도 기존 좋아요는 보존된다.
                    """
    )
    public ResponseEntity<ApiResponseDto<CourseLikeToggleResponse>> toggleLike(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String shareToken
    ) {
        CourseLikeToggleResponse data = courseLikeService.toggle(shareToken, principal.userId());
        return ApiResponseDto.success(200, "코스 좋아요 토글 성공", data);
    }
}
