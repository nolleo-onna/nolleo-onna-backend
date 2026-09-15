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
                    - "광안리 바다는 꼭 넣어줘 / 해운대 해수욕장은 빼줘"처럼 장소를 지정하면 intent.includeSpots /
                      excludeSpots에 {name, contentId, matchedTitle}로 누적된다. 생성 확인 시점에 이름을 실제 스팟과 맞춰
                      contentId·matchedTitle을 채우고, 확인 문구에 "무엇으로 이해했는지"(📍)와 "못 찾은 이름"(⚠️)을 보여준다.
                      같은 이름을 나중에 반대로 말하면 나중 것이 이긴다. 못 찾은 지정은 생성에서 건너뛴다.
                      이름 매칭은 원문 → 동의어(바다→해수욕장 등) → AI 공식 명칭 힌트 순으로 우리 데이터에서만 찾는다.
                    - "부산국제항만컨퍼런스 근처 갈만한 곳"처럼 기준점을 말하면 intent.anchor에 담기고, 행사 → 스팟 데이터 순으로
                      좌표를 찾아 검색 중심으로 쓴다. startArea는 그 좌표에 가장 가까운 지원 지역으로 자동 채워진다(직접 말했으면 유지).
                      못 찾으면 지역을 되묻고, 확인 문구에 "🎯 기준점: …(제목, 기간) 근처"로 무엇으로 이해했는지 보여준다.
                    - 이름 매칭은 제목이 정확히 일치하거나 결과가 하나일 때만 확정한다. "해수욕장"처럼 여러 곳에 걸리는 이름은
                      못 찾은 것으로 보고 ⚠️로 정확한 이름을 알려달라고 안내한다 (되묻기·후보 선택 없음).
                    - 생성 확인 후 "코스 생성 시작" 문구가 있으면 코스 생성 파이프라인을 실행하고
                      COMPLETED + pairId 반환.
                    - 하루 최대 3회까지만 생성 가능하며, 초과 시 LIMIT_EXCEEDED 반환.
                    - 비용 안전장치: 한 대화의 메시지 수가 상한(기본 10)을 넘거나 여행 무관 메시지가 연속(기본 3회)되면
                      CONVERSATION_ENDED 반환 — 그 conversationId는 종료되었으니 새 대화(conversationId=null)로 시작한다.
                      하루 메시지 수 상한(기본 40)을 넘으면 MESSAGE_LIMIT_EXCEEDED 반환 (파싱 전 거절, intent 비어 있음).
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
