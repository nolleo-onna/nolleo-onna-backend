package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.course.application.dto.ChatResult;
import com.nolleo.onna.domain.course.application.service.CourseChatService;
import com.nolleo.onna.domain.course.application.service.CourseQueryService;
import com.nolleo.onna.domain.course.presentation.dto.request.CourseChatRequest;
import com.nolleo.onna.domain.course.presentation.dto.response.CourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
                    자연어 메시지를 파싱해 코스 생성 의도를 추출한다.
                    - 여행과 무관한 메시지면 OFF_TOPIC + 안내(reply) 반환.
                    - 정보 부족 시 NEED_MORE_INFO + 되묻기(reply) 반환. conversationId로 대화를 이어간다.
                    - 생성 가능 시 코스 생성 파이프라인을 실행하고 COMPLETED + pairId 반환.
                    - intent 필드는 검증용으로 현재까지 파싱된 상태를 보여준다.
                    """
    )
    public ResponseEntity<ApiResponseDto<Object>> chat(@Valid @RequestBody CourseChatRequest request) {
        ChatResult result = courseChatService.chat(request.message(), request.conversationId());
        return ApiResponseDto.success(200, "요청 성공", result);
    }

    @GetMapping("/{pairId}")
    @Operation(
            summary = "생성된 코스 조회",
            description = "chat API의 COMPLETED 응답에서 받은 pairId로 생성된 코스(들)을 방문 스팟 상세와 함께 조회한다."
    )
    public ResponseEntity<ApiResponseDto<List<CourseResponse>>> getCourse(@PathVariable UUID pairId) {
        List<CourseResponse> data = courseQueryService.getByPairId(pairId);
        return ApiResponseDto.success(200, "코스 조회 성공", data);
    }
}
