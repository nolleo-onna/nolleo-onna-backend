package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.CreateCourseCommand;
import com.nolleo.onna.domain.course.application.dto.GenerationOptions;
import com.nolleo.onna.domain.course.application.dto.GenerationResult;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.CreateCourseResponse;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseFormServiceTest {

    @Mock CourseAnchorResolver anchorResolver;
    @Mock SpotPinResolver spotPinResolver;
    @Mock CourseGenerationService courseGenerationService;
    @Mock CourseQueryService courseQueryService;

    @InjectMocks CourseFormService service;

    private static final Long USER_ID = 1L;
    private static final GeoPoint VENUE = new GeoPoint(35.1531, 129.1187);

    private static CreateCourseCommand command(String area, BudgetTier budget, List<String> spots, String festival) {
        return new CreateCourseCommand(USER_ID, area, budget, spots, festival);
    }

    /** 리졸버가 intent를 그대로(또는 가공해서) 돌려주도록 스텁 */
    private void passThroughAnchor() {
        given(anchorResolver.resolve(any(CourseIntent.class))).willAnswer(inv -> inv.getArgument(0));
    }

    private void passThroughPins() {
        given(spotPinResolver.resolve(any(CourseIntent.class))).willAnswer(inv -> inv.getArgument(0));
    }

    /** 생성 서비스가 넘겨받은 intent·제목으로 폼 코스를 만들어 돌려주도록 스텁 */
    private void stubGeneration(boolean relaxed) {
        given(courseGenerationService.generate(eq(USER_ID), any(CourseIntent.class), any(GenerationOptions.class)))
                .willAnswer(inv -> {
                    CourseIntent intent = inv.getArgument(1);
                    GenerationOptions options = inv.getArgument(2);
                    return new GenerationResult(Course.createByForm(USER_ID, UUID.randomUUID(), options.templateTitle(), intent, "FORM"), relaxed);
                });
        given(courseQueryService.getByPairId(anyLong(), any(UUID.class))).willReturn(List.of());
    }

    private GenerationOptions capturedOptions() {
        ArgumentCaptor<GenerationOptions> captor = ArgumentCaptor.forClass(GenerationOptions.class);
        verify(courseGenerationService).generate(eq(USER_ID), any(CourseIntent.class), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("지역만으로 만들면 리랭킹 없는 폼 옵션과 '{지역} 중심 코스' 제목으로 생성하고, 예산은 제한없음이 기본이다")
    void create_areaOnly_usesFormOptions() {
        passThroughAnchor();
        passThroughPins();
        stubGeneration(false);

        CreateCourseResponse response = service.create(command("광안리", null, List.of(), null));

        GenerationOptions options = capturedOptions();
        assertThat(options.mode()).isEqualTo(GenerationMode.ALGORITHM);
        assertThat(options.createdBy()).isEqualTo("FORM");
        assertThat(options.useRerank()).isFalse();
        assertThat(options.templateTitle()).isEqualTo("광안리 중심 코스");

        assertThat(response.applied().startArea()).isEqualTo("광안리");
        assertThat(response.applied().budget().tier()).isEqualTo(BudgetTier.UNLIMITED);
        assertThat(response.applied().includeSpots()).isEmpty();
        assertThat(response.applied().festival()).isNull();
        assertThat(response.unmatched().includeSpots()).isEmpty();
        assertThat(response.unmatched().festival()).isNull();
        assertThat(response.pairId()).isNotNull();
    }

    @Test
    @DisplayName("꼭 포함 장소는 매칭 결과대로 applied/unmatched로 나뉘고, 예산 등급 금액이 intent에 실린다")
    void create_reportsMatchedAndUnmatchedSpots() {
        passThroughAnchor();
        given(spotPinResolver.resolve(any(CourseIntent.class))).willAnswer(inv -> {
            CourseIntent intent = inv.getArgument(0);
            return intent.withPins(List.of(
                    new SpotPin("광안리 바다", "beach", "광안리해수욕장"),
                    SpotPin.of("동백섬 바다")), List.of());
        });
        stubGeneration(true);

        CreateCourseResponse response = service.create(command("광안리", BudgetTier.UNDER_30K, List.of("광안리 바다", "동백섬 바다"), null));

        assertThat(response.applied().includeSpots())
                .containsExactly(new CreateCourseResponse.MatchedSpot("광안리 바다", "광안리해수욕장", "beach"));
        assertThat(response.unmatched().includeSpots()).containsExactly("동백섬 바다");
        assertThat(response.applied().budget()).isEqualTo(new CreateCourseResponse.Budget(BudgetTier.UNDER_30K, true));

        ArgumentCaptor<CourseIntent> intent = ArgumentCaptor.forClass(CourseIntent.class);
        verify(spotPinResolver).resolve(intent.capture());
        assertThat(intent.getValue().budget()).isEqualTo(30000);
        assertThat(intent.getValue().includeSpots()).extracting(SpotPin::name).containsExactly("광안리 바다", "동백섬 바다");
        assertThat(intent.getValue().clarifiedOnce()).isTrue(); // 폼은 되묻기 단계가 없다
    }

    @Test
    @DisplayName("축제를 찾으면 축제 위치의 지역으로 바뀌고 제목은 입력한 축제명 기준이 된다")
    void create_festivalFound_overridesAreaAndTitle() {
        given(anchorResolver.resolve(any(CourseIntent.class))).willAnswer(inv -> {
            CourseIntent intent = inv.getArgument(0);
            CourseAnchor resolved = intent.anchor().resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", "제20회 부산불꽃축제", VENUE, "11.1~11.1");
            return intent.withAnchor(resolved).withStartArea("광안리"); // 리졸버가 축제 위치(광안리)로 덮어쓴 결과
        });
        passThroughPins();
        stubGeneration(false);

        CreateCourseResponse response = service.create(command("서면", BudgetTier.UNLIMITED, List.of(), "부산불꽃축제"));

        assertThat(capturedOptions().templateTitle()).isEqualTo("부산불꽃축제 중심 코스");
        assertThat(response.applied().startArea()).isEqualTo("광안리");
        assertThat(response.applied().festival())
                .isEqualTo(new CreateCourseResponse.MatchedFestival("부산불꽃축제", "제20회 부산불꽃축제", "11.1~11.1"));
        assertThat(response.unmatched().festival()).isNull();
    }

    @Test
    @DisplayName("축제를 못 찾으면 선택 지역 중심으로 만들고 unmatched.festival로 알린다")
    void create_festivalNotFound_fallsBackToArea() {
        passThroughAnchor(); // 미해결 anchor 그대로
        passThroughPins();
        stubGeneration(false);

        CreateCourseResponse response = service.create(command("광안리", BudgetTier.NONE, List.of(), "없는축제"));

        assertThat(capturedOptions().templateTitle()).isEqualTo("광안리 중심 코스");
        assertThat(response.applied().festival()).isNull();
        assertThat(response.unmatched().festival()).isEqualTo("없는축제");
        assertThat(response.applied().budget().tier()).isEqualTo(BudgetTier.NONE);
    }

    @Test
    @DisplayName("지원하지 않는 지역이면 UNKNOWN_START_AREA로 거절하고 아무것도 조회·생성하지 않는다")
    void create_throws_whenUnknownArea() {
        assertThatThrownBy(() -> service.create(command("화성", null, List.of(), null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.UNKNOWN_START_AREA);

        verifyNoInteractions(anchorResolver, spotPinResolver, courseGenerationService, courseQueryService);
    }

    @Test
    @DisplayName("응답의 courses는 저장된 코스를 pairId로 다시 읽은 조회 응답이다")
    void create_returnsCoursesFromQueryService() {
        passThroughAnchor();
        passThroughPins();
        CourseResponse sample = new CourseResponse(10L, UUID.randomUUID(), "ALGORITHM", "광안리 중심 코스", null, null,
                List.of(), null, null);
        given(courseGenerationService.generate(eq(USER_ID), any(CourseIntent.class), any(GenerationOptions.class)))
                .willAnswer(inv -> new GenerationResult(
                        Course.createByForm(USER_ID, UUID.randomUUID(), "광안리 중심 코스", inv.getArgument(1), "FORM"), false));
        given(courseQueryService.getByPairId(anyLong(), any(UUID.class))).willReturn(List.of(sample));

        CreateCourseResponse response = service.create(command("광안리", null, List.of(), null));

        assertThat(response.courses()).containsExactly(sample);
    }
}
