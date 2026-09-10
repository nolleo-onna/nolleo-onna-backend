package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseItemResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.ShareInfoResponse;
import com.nolleo.onna.domain.course.application.service.CourseEditService;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaces;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseEditController.class)
@WithMockUser
class CourseEditControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean CourseEditService courseEditService;
    @MockBean JwtProvider jwtProvider;

    private static final String TITLE = "광안리 바다 산책";
    private static final String DESCRIPTION = "바다를 따라 걷는 코스입니다.";
    private static final String ITEMS = "[{\"placeType\": \"SPOT\", \"originalId\": \"2760699\"}]";

    private static Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static MockHttpServletRequestBuilder putCourse(String body) {
        return put("/api/v1/courses/10")
                .with(authentication(authAs(1L)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    /** null인 필드는 JSON에서 생략한다 */
    private static String body(String title, String description, String itemsJson) {
        StringBuilder json = new StringBuilder("{");
        if (title != null) json.append("\"title\": \"").append(title).append("\", ");
        if (description != null) json.append("\"description\": \"").append(description).append("\", ");
        return json.append("\"items\": ").append(itemsJson).append("}").toString();
    }

    private static CourseResponse sampleResponse() {
        CourseItemResponse item = new CourseItemResponse((short) 1, "SPOT", "2760699", "광안리해수욕장",
                new BigDecimal("129.1187"), new BigDecimal("35.1531"), null, "자연/공원", null, 850);
        return new CourseResponse(10L, UUID.randomUUID(), "AI", TITLE, DESCRIPTION, null,
                List.of(item), ShareInfoResponse.from(null), OffsetDateTime.now());
    }

    // ── 정상 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/v1/courses/{courseId} - 제목·소개·스팟이 유효하면 200과 서버가 재계산한 코스를 반환한다")
    void updateCourse_returns200_whenValid() throws Exception {
        given(courseEditService.updateCourse(any())).willReturn(sampleResponse());

        mockMvc.perform(putCourse(body(TITLE, DESCRIPTION, ITEMS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(TITLE))
                .andExpect(jsonPath("$.data.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.data.items[0].serialNum").value(1))
                .andExpect(jsonPath("$.data.items[0].originalId").value("2760699"));

        ArgumentCaptor<UpdateCourseCommand> captor = ArgumentCaptor.forClass(UpdateCourseCommand.class);
        verify(courseEditService).updateCourse(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(10L);
        assertThat(captor.getValue().userId()).isEqualTo(1L); // 본문이 아니라 인증 주체에서 온다
        assertThat(captor.getValue().title()).isEqualTo(TITLE);
        assertThat(captor.getValue().description()).isEqualTo(DESCRIPTION);
        assertThat(captor.getValue().items()).containsExactly(PlaceRef.spot("2760699"));
    }

    @Test
    @DisplayName("소개는 선택이다 — 생략해도 200이고 커맨드의 description은 null이다")
    void updateCourse_returns200_whenDescriptionOmitted() throws Exception {
        given(courseEditService.updateCourse(any())).willReturn(sampleResponse());

        mockMvc.perform(putCourse(body(TITLE, null, ITEMS)))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateCourseCommand> captor = ArgumentCaptor.forClass(UpdateCourseCommand.class);
        verify(courseEditService).updateCourse(captor.capture());
        assertThat(captor.getValue().description()).isNull();
    }

    // ── 400 — 요청 검증에서 막히면 서비스는 호출되지 않는다 ───────────────────

    @Test
    @DisplayName("제목이 없으면 400")
    void updateCourse_returns400_whenTitleMissing() throws Exception {
        mockMvc.perform(putCourse(body(null, DESCRIPTION, ITEMS)))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("제목이 공백뿐이면 400")
    void updateCourse_returns400_whenTitleBlank() throws Exception {
        mockMvc.perform(putCourse(body("   ", DESCRIPTION, ITEMS)))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("제목이 최대 길이를 넘으면 400")
    void updateCourse_returns400_whenTitleTooLong() throws Exception {
        mockMvc.perform(putCourse(body("가".repeat(Course.MAX_TITLE_LENGTH + 1), DESCRIPTION, ITEMS)))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("소개가 최대 길이를 넘으면 400")
    void updateCourse_returns400_whenDescriptionTooLong() throws Exception {
        mockMvc.perform(putCourse(body(TITLE, "가".repeat(Course.MAX_DESCRIPTION_LENGTH + 1), ITEMS)))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("items에 null 원소가 있으면 500이 아니라 400")
    void updateCourse_returns400_whenItemIsNull() throws Exception {
        mockMvc.perform(putCourse(body(TITLE, DESCRIPTION, "[null]")))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("placeType이 SPOT/FOOD가 아니면 400")
    void updateCourse_returns400_whenPlaceTypeInvalid() throws Exception {
        mockMvc.perform(putCourse(body(TITLE, DESCRIPTION, "[{\"placeType\": \"HOTEL\", \"originalId\": \"1\"}]")))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("방문 스팟이 상한을 넘으면 400")
    void updateCourse_returns400_whenOverLimit() throws Exception {
        String items = IntStream.rangeClosed(1, CoursePlaces.MAX_ITEMS + 1)
                .mapToObj(i -> "{\"placeType\": \"SPOT\", \"originalId\": \"S" + i + "\"}")
                .collect(Collectors.joining(",", "[", "]"));

        mockMvc.perform(putCourse(body(TITLE, DESCRIPTION, items)))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateCourse(any());
    }

    @Test
    @DisplayName("본문 형식이 깨졌으면(배열 자리에 문자열) 500이 아니라 400 INVALID_REQUEST")
    void updateCourse_returns400_whenBodyMalformed() throws Exception {
        mockMvc.perform(putCourse(body(TITLE, DESCRIPTION, "\"abc\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verify(courseEditService, never()).updateCourse(any());
    }

    // ── 409 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("동시 저장으로 낙관적 락 충돌이 나면 500이 아니라 409 CONCURRENT_MODIFICATION")
    void updateCourse_returns409_whenConcurrentlyModified() throws Exception {
        given(courseEditService.updateCourse(any()))
                .willThrow(new ObjectOptimisticLockingFailureException("generated_course_items", 10L));

        mockMvc.perform(putCourse(body(TITLE, DESCRIPTION, ITEMS)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));
    }
}
