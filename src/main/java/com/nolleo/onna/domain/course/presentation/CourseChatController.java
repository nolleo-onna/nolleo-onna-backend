package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.course.application.dto.ChatResult;
import com.nolleo.onna.domain.course.application.service.CourseChatService;
import com.nolleo.onna.domain.course.application.service.CourseQueryService;
import com.nolleo.onna.domain.course.presentation.dto.request.CourseChatRequest;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Chat", description = "자연어 코스 생성 대화 API")
public class CourseChatController {

    private final CourseChatService courseChatService;
    private final CourseQueryService courseQueryService;

    @PostMapping("/chat")
    @Operation(
            summary = "자연어 코스 대화",
            description = """
                    자연어 메시지를 파싱해 코스 생성 의도를 추출한다. 로그인이 필요하다.
                    - 여행과 무관한 메시지면 OFF_TOPIC + 안내(reply) 반환.
                    - 정보 부족 시 NEED_MORE_INFO + 되묻기(reply) 반환. conversationId로 대화를 이어간다.
                    - 생성 확인 후 "코스 생성 시작" 문구가 있으면 코스 생성 파이프라인을 실행하고
                      COMPLETED + pairId 반환.
                    - 하루 최대 3회까지만 생성 가능하며, 초과 시 LIMIT_EXCEEDED 반환.
                    - intent 필드는 검증용으로 현재까지 파싱된 상태를 보여준다.
                    """
    )
    public ResponseEntity<ApiResponseDto<Object>> chat(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CourseChatRequest request
    ) {
        ChatResult result = courseChatService.chat(principal.userId(), request.message(), request.conversationId());
        return ApiResponseDto.success(200, "요청 성공", result);
    }

    @GetMapping("/me")
    @Operation(
            summary = "내가 생성한 코스 목록 조회",
            description = "로그인한 사용자가 지금까지 생성한 코스 목록을 요약 정보로 조회한다. " +
                    "상세(좌표·가격·거리 등)는 pairId로 GET /courses/{pairId}를 호출한다."
    )
    public ResponseEntity<ApiResponseDto<List<CourseSummaryResponse>>> getMyCourses(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<CourseSummaryResponse> data = courseQueryService.getByUserId(principal.userId());
        return ApiResponseDto.success(200, "코스 조회 성공", data);
    }

    @GetMapping("/{pairId}")
    @Operation(
            summary = "생성된 코스 조회",
            description = "chat API의 COMPLETED 응답에서 받은 pairId로 생성된 코스(들)을 방문 스팟 상세와 함께 조회한다. " +
                    "로그인이 필요하며, 본인이 생성한 코스만 조회할 수 있다."
    )
    public ResponseEntity<ApiResponseDto<List<CourseResponse>>> getCourse(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID pairId
    ) {
        List<CourseResponse> data = courseQueryService.getByPairId(principal.userId(), pairId);
        return ApiResponseDto.success(200, "코스 조회 성공", data);
    }
}
