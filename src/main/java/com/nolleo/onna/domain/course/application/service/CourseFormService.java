package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.CreateCourseCommand;
import com.nolleo.onna.domain.course.application.dto.GenerationOptions;
import com.nolleo.onna.domain.course.application.dto.GenerationResult;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.CreateCourseResponse;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 폼 기반 코스 생성 유스케이스 — 지역 · 예산 등급 · 꼭 포함 장소 · 축제.
 *
 * 챗봇과 달리 외부 AI 호출이 없어 일일 제한을 두지 않는다:
 *   - 동행·무드를 받지 않으므로 벡터 리랭킹(OpenAI)을 끈다
 *   - 제목은 템플릿, 소개는 비운다 (Gemini 미사용). 사용자가 편집으로 채운다
 * 이름 매칭·후보 조립은 챗봇과 같은 파이프라인을 쓴다.
 *
 * 흐름: 요청 → CourseIntent 조립 → 축제(기준점) 매칭 → 꼭 포함 장소 매칭 → 생성 → 반영/미반영 집계.
 *   - 축제를 찾으면 축제 좌표가 검색 중심이 되고 startArea는 축제 위치의 지역으로 바뀐다 (사용자가 고른 지역과 달라질 수 있다)
 *   - 못 찾은 장소·축제는 생성을 막지 않고 응답의 unmatched로 알린다
 */
@Service
@RequiredArgsConstructor
public class CourseFormService {

    private final CourseAnchorResolver anchorResolver;
    private final SpotPinResolver spotPinResolver;
    private final CourseGenerationService courseGenerationService;
    private final CourseQueryService courseQueryService;

    public CreateCourseResponse create(CreateCourseCommand command) {
        String startArea = DistrictCenter.of(command.startArea())
                .map(DistrictCenter::getSigngu)
                .orElseThrow(() -> new BusinessException(CourseErrorCode.UNKNOWN_START_AREA));

        CourseIntent intent = new CourseIntent(
                startArea, false, command.budget().amount(), null, List.of(), null, true,
                command.includeSpots().stream().map(SpotPin::of).toList(), List.of(),
                command.festival() != null ? CourseAnchor.of(command.festival()) : null);

        intent = anchorResolver.resolve(intent);
        intent = spotPinResolver.resolve(intent);

        GenerationResult result = courseGenerationService.generate(
                command.userId(), intent, GenerationOptions.form(titleFor(intent, command.festival())));
        Course course = result.course();

        // 저장된 코스를 스팟 상세와 함께 다시 읽어 조회 응답과 같은 형태로 돌려준다
        List<CourseResponse> courses = courseQueryService.getByPairId(command.userId(), course.getPairId());
        return CreateCourseResponse.of(course.getPairId(), courses, course.getIntent(),
                command.budget(), result.budgetFilterRelaxed());
    }

    /** 축제를 찾았으면 입력한 축제명으로, 아니면 지역으로 — "{광안리|부산불꽃축제} 중심 코스" */
    static String titleFor(CourseIntent intent, String festival) {
        boolean festivalFound = intent.anchor() != null && intent.anchor().isResolved();
        return (festivalFound ? festival.strip() : intent.startArea()) + " 중심 코스";
    }
}
