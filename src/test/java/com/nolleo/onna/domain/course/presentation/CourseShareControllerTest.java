package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseVisibilityCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseItemResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.ShareInfoResponse;
import com.nolleo.onna.domain.course.application.dto.response.SharedCourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseShareService;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 슬라이스 — 요청 매핑 · 검증 · 오류 변환만 본다.
 * SecurityConfig는 이 슬라이스에 로드되지 않으므로 permitAll(비로그인 공유 조회)은 여기서 검증할 수 없다.
 * 그 규칙은 개발서버에서 토큰 없이 GET /api/v1/courses/shared/{token} 을 호출해 확인한다.
 */
@WebMvcTest(CourseShareController.class)
@WithMockUser
class CourseShareControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CourseShareService courseShareService;
    @MockBean JwtProvider jwtProvider;

    private static final String TOKEN = "Qm9vay1zaGFyZS10b2tlbi1leGFtcGxl";

    private static Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static CourseItemResponse sampleItem() {
        return new CourseItemResponse((short) 1, "SPOT", "2760699", "광안리해수욕장",
                new BigDecimal("129.1187"), new BigDecimal("35.1531"), null, "자연/공원", null, 850);
    }

    private static CourseResponse ownerResponse(boolean isPublic) {
        return new CourseResponse(10L, UUID.randomUUID(), "AI", "광안리 데이트", "소개", null,
                List.of(sampleItem()), new ShareInfoResponse(isPublic, TOKEN, 3, 1), OffsetDateTime.now());
    }

    // ── PATCH /courses/{courseId}/visibility ──────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/visibility - 공개 전환 요청은 JWT userId와 목표 상태를 커맨드로 넘기고 share 블록을 돌려준다")
    void updateVisibility_publish_returns200WithShare() throws Exception {
        given(courseShareService.updateVisibility(any(UpdateCourseVisibilityCommand.class)))
                .willReturn(ownerResponse(true));

        mockMvc.perform(patch("/api/v1/courses/10/visibility")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPublic\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("코스 공개 상태 변경 성공"))
                .andExpect(jsonPath("$.data.share.isPublic").value(true))
                .andExpect(jsonPath("$.data.share.shareToken").value(TOKEN))
                .andExpect(jsonPath("$.data.share.viewCount").value(3))
                .andExpect(jsonPath("$.data.share.likeCount").value(1));

        ArgumentCaptor<UpdateCourseVisibilityCommand> captor = ArgumentCaptor.forClass(UpdateCourseVisibilityCommand.class);
        verify(courseShareService).updateVisibility(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(10L);
        assertThat(captor.getValue().userId()).isEqualTo(1L);   // 본문이 아니라 인증 주체에서 온다
        assertThat(captor.getValue().isPublic()).isTrue();
    }

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/visibility - isPublic=false 는 비공개 전환 커맨드로 전달된다")
    void updateVisibility_unpublish_passesFalse() throws Exception {
        given(courseShareService.updateVisibility(any(UpdateCourseVisibilityCommand.class)))
                .willReturn(ownerResponse(false));

        mockMvc.perform(patch("/api/v1/courses/10/visibility")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPublic\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.share.isPublic").value(false))
                .andExpect(jsonPath("$.data.share.shareToken").value(TOKEN)); // 비공개여도 토큰은 유지

        ArgumentCaptor<UpdateCourseVisibilityCommand> captor = ArgumentCaptor.forClass(UpdateCourseVisibilityCommand.class);
        verify(courseShareService).updateVisibility(captor.capture());
        assertThat(captor.getValue().isPublic()).isFalse();
    }

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/visibility - isPublic 누락은 400이고 서비스는 호출되지 않는다")
    void updateVisibility_returns400_whenIsPublicMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/10/visibility")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(courseShareService, never()).updateVisibility(any());
    }

    @Test
    @DisplayName("PATCH /api/v1/courses/{id}/visibility - 타인 코스면 403 COURSE_ACCESS_DENIED")
    void updateVisibility_returns403_whenNotOwner() throws Exception {
        willThrow(new BusinessException(CourseErrorCode.COURSE_ACCESS_DENIED))
                .given(courseShareService).updateVisibility(any(UpdateCourseVisibilityCommand.class));

        mockMvc.perform(patch("/api/v1/courses/10/visibility")
                        .with(authentication(authAs(2L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isPublic\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COURSE_ACCESS_DENIED"));
    }

    // ── GET /courses/shared/{shareToken} ─────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/courses/shared/{token} - 공개 코스를 작성자 닉네임·조회수와 함께 돌려주고 userId·pairId·토큰은 담지 않는다")
    void getShared_returns200_withoutOwnerIdentity() throws Exception {
        given(courseShareService.getShared(TOKEN)).willReturn(new SharedCourseResponse(
                10L, "광안리 데이트", "소개", 15000, List.of(sampleItem()),
                "부산러버", "https://img/1.png", 43, 7, OffsetDateTime.now()));

        mockMvc.perform(get("/api/v1/courses/shared/" + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("공유 코스 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("광안리 데이트"))
                .andExpect(jsonPath("$.data.authorNickname").value("부산러버"))
                .andExpect(jsonPath("$.data.viewCount").value(43))
                .andExpect(jsonPath("$.data.likeCount").value(7))
                .andExpect(jsonPath("$.data.items[0].originalId").value("2760699"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.pairId").doesNotExist())
                .andExpect(jsonPath("$.data.shareToken").doesNotExist())
                .andExpect(jsonPath("$.data.share").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/courses/shared/{token} - 없거나 비공개인 토큰은 404 COURSE_NOT_FOUND")
    void getShared_returns404_whenNotPublic() throws Exception {
        willThrow(new BusinessException(CourseErrorCode.COURSE_NOT_FOUND))
                .given(courseShareService).getShared("unknown");

        mockMvc.perform(get("/api/v1/courses/shared/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COURSE_NOT_FOUND"));
    }
}
