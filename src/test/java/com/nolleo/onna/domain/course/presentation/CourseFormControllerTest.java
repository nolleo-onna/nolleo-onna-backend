package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.course.application.dto.CreateCourseCommand;
import com.nolleo.onna.domain.course.application.dto.response.CreateCourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseFormService;
import com.nolleo.onna.domain.course.domain.model.vo.BudgetTier;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseFormController.class)
@WithMockUser
class CourseFormControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean CourseFormService courseFormService;
    @MockBean JwtProvider jwtProvider;

    private static Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static MockHttpServletRequestBuilder postCourse(String body) {
        return post("/api/v1/courses")
                .with(authentication(authAs(1L)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static CreateCourseResponse sampleResponse() {
        UUID pairId = UUID.randomUUID();
        return new CreateCourseResponse(pairId, List.of(),
                new CreateCourseResponse.Applied("광안리", new CreateCourseResponse.Budget(BudgetTier.UNDER_30K, false),
                        List.of(new CreateCourseResponse.MatchedSpot("광안리 바다", "광안리해수욕장", "126081")), null),
                new CreateCourseResponse.Unmatched(List.of("동백섬 바다"), null));
    }

    @Test
    @DisplayName("POST /api/v1/courses - 유효한 요청이면 200과 생성 결과(applied/unmatched)를 반환하고 userId는 인증 주체에서 온다")
    void createCourse_returns200_whenValid() throws Exception {
        given(courseFormService.create(any())).willReturn(sampleResponse());

        mockMvc.perform(postCourse("""
                        {"startArea":"광안리","budget":"UNDER_30K","includeSpots":["광안리 바다","동백섬 바다"],"festival":"부산불꽃축제"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied.startArea").value("광안리"))
                .andExpect(jsonPath("$.data.applied.budget.tier").value("UNDER_30K"))
                .andExpect(jsonPath("$.data.applied.includeSpots[0].matchedTitle").value("광안리해수욕장"))
                .andExpect(jsonPath("$.data.unmatched.includeSpots[0]").value("동백섬 바다"));

        ArgumentCaptor<CreateCourseCommand> captor = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(courseFormService).create(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(1L);
        assertThat(captor.getValue().startArea()).isEqualTo("광안리");
        assertThat(captor.getValue().budget()).isEqualTo(BudgetTier.UNDER_30K);
        assertThat(captor.getValue().includeSpots()).containsExactly("광안리 바다", "동백섬 바다");
        assertThat(captor.getValue().festival()).isEqualTo("부산불꽃축제");
    }

    @Test
    @DisplayName("POST /api/v1/courses - 지역만 보내도 되며 예산은 UNLIMITED, 장소 목록은 빈 목록으로 채워진다")
    void createCourse_defaults_whenOptionalFieldsOmitted() throws Exception {
        given(courseFormService.create(any())).willReturn(sampleResponse());

        mockMvc.perform(postCourse("{\"startArea\":\"광안리\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateCourseCommand> captor = ArgumentCaptor.forClass(CreateCourseCommand.class);
        verify(courseFormService).create(captor.capture());
        assertThat(captor.getValue().budget()).isEqualTo(BudgetTier.UNLIMITED);
        assertThat(captor.getValue().includeSpots()).isEmpty();
        assertThat(captor.getValue().festival()).isNull();
    }

    @Test
    @DisplayName("POST /api/v1/courses - 시작 지역이 없으면 400")
    void createCourse_returns400_whenStartAreaMissing() throws Exception {
        mockMvc.perform(postCourse("{\"budget\":\"UNLIMITED\"}"))
                .andExpect(status().isBadRequest());
        verify(courseFormService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/courses - 꼭 포함 장소가 5개를 넘거나 빈 이름이 섞이면 400")
    void createCourse_returns400_whenIncludeSpotsInvalid() throws Exception {
        mockMvc.perform(postCourse("{\"startArea\":\"광안리\",\"includeSpots\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(postCourse("{\"startArea\":\"광안리\",\"includeSpots\":[\"광안리해수욕장\",\"  \"]}"))
                .andExpect(status().isBadRequest());
        verify(courseFormService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/courses - 축제명이 50자를 넘으면 400")
    void createCourse_returns400_whenFestivalTooLong() throws Exception {
        mockMvc.perform(postCourse("{\"startArea\":\"광안리\",\"festival\":\"" + "축".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest());
        verify(courseFormService, never()).create(any());
    }

    @Test
    @DisplayName("POST /api/v1/courses - 예산 값이 정의된 등급이 아니면 400")
    void createCourse_returns400_whenBudgetUnknown() throws Exception {
        mockMvc.perform(postCourse("{\"startArea\":\"광안리\",\"budget\":\"UNDER_70K\"}"))
                .andExpect(status().isBadRequest());
        verify(courseFormService, never()).create(any());
    }
}
