package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.course.application.dto.response.CreateCourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseFormService;
import com.nolleo.onna.domain.course.presentation.dto.request.CreateCourseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Form", description = "폼 기반 코스 생성 API")
public class CourseFormController {

    private final CourseFormService courseFormService;

    @PostMapping
    @Operation(
            summary = "폼으로 코스 생성 (지역·예산·꼭 포함 장소·축제)",
            description = """
                    폼 입력으로 코스를 한 번에 만든다. 로그인이 필요하며 외부 AI 호출이 없어 일일 제한이 없다.
                    - startArea: 지원 지역명 중 하나(필수). budget: NONE/UNDER_10K/UNDER_30K/UNDER_50K/UNLIMITED (생략 시 UNLIMITED).
                    - 예산 등급이 식사·카페 슬롯 수와 1곳당 가격 상한을 정한다. 무지출은 식사·카페 없이 관광지만 담는다.
                      상한 안에서 다 채우지 못하면 필터를 풀고 채우며 applied.budget.filterRelaxed=true로 알린다.
                    - includeSpots: 꼭 포함할 장소명(최대 5). 데이터 제목과 정확히 일치하거나 하나만 걸릴 때 반영된다.
                      "해수욕장"처럼 여럿에 걸리는 이름은 못 찾은 것으로 보고 unmatched.includeSpots로 알린다. 생성은 진행된다.
                    - festival: 축제(행사)명. 찾으면 축제 좌표가 검색 중심이 되고 startArea는 축제 위치의 지역으로 바뀐다
                      (광안리 선택 + 중구 축제 → applied.startArea="중구"). 못 찾으면 선택 지역 중심으로 만들고 unmatched.festival로 알린다.
                    - 제목은 "{지역} 중심 코스" 또는 "{축제명} 중심 코스" 템플릿이며 소개는 비어 있다. 편집 API로 바꿀 수 있다.
                    - 응답의 courses는 GET /courses/{pairId}와 같은 구조라 재조회 없이 바로 화면에 쓸 수 있다.
                    - 지원하지 않는 지역이면 UNKNOWN_START_AREA(400), 반경 안에 스팟이 없으면 NO_SPOT_CANDIDATES(404).
                    """
    )
    public ResponseEntity<ApiResponseDto<CreateCourseResponse>> createCourse(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateCourseRequest request
    ) {
        CreateCourseResponse data = courseFormService.create(request.toCommand(principal.userId()));
        return ApiResponseDto.success(200, "코스 생성 성공", data);
    }
}
