package com.nolleo.onna.domain.course.presentation;

import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseItemsCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseItemResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.service.CourseEditService;
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

    private static Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static MockHttpServletRequestBuilder putItems(String body) {
        return put("/api/v1/courses/10/items")
                .with(authentication(authAs(1L)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static CourseResponse sampleResponse() {
        CourseItemResponse item = new CourseItemResponse((short) 1, "SPOT", "2760699", "광안리해수욕장",
                new BigDecimal("129.1187"), new BigDecimal("35.1531"), null, "자연/공원", null, 850);
        return new CourseResponse(10L, UUID.randomUUID(), "AI", "광안리 코스", "소개", null,
                List.of(item), OffsetDateTime.now());
    }

    @Test
    @DisplayName("PUT /api/v1/courses/{courseId}/items - 유효한 요청이면 200과 서버가 재계산한 코스를 반환한다")
    void updateItems_returns200_whenValid() throws Exception {
        given(courseEditService.updateItems(any())).willReturn(sampleResponse());

        mockMvc.perform(putItems("""
                        {"items": [{"placeType": "SPOT", "originalId": "2760699"}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].serialNum").value(1))
                .andExpect(jsonPath("$.data.items[0].originalId").value("2760699"));

        ArgumentCaptor<UpdateCourseItemsCommand> captor = ArgumentCaptor.forClass(UpdateCourseItemsCommand.class);
        verify(courseEditService).updateItems(captor.capture());
        assertThat(captor.getValue().courseId()).isEqualTo(10L);
        assertThat(captor.getValue().userId()).isEqualTo(1L); // 본문이 아니라 인증 주체에서 온다
        assertThat(captor.getValue().items()).containsExactly(PlaceRef.spot("2760699"));
    }

    @Test
    @DisplayName("items에 null 원소가 있으면 500이 아니라 400을 반환하고 서비스는 호출되지 않는다")
    void updateItems_returns400_whenItemIsNull() throws Exception {
        mockMvc.perform(putItems("""
                        {"items": [null]}
                        """))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateItems(any());
    }

    @Test
    @DisplayName("placeType이 SPOT/FOOD가 아니면 400")
    void updateItems_returns400_whenPlaceTypeInvalid() throws Exception {
        mockMvc.perform(putItems("""
                        {"items": [{"placeType": "HOTEL", "originalId": "1"}]}
                        """))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateItems(any());
    }

    @Test
    @DisplayName("방문 스팟이 상한을 넘으면 400")
    void updateItems_returns400_whenOverLimit() throws Exception {
        String items = IntStream.rangeClosed(1, CoursePlaces.MAX_ITEMS + 1)
                .mapToObj(i -> "{\"placeType\": \"SPOT\", \"originalId\": \"S" + i + "\"}")
                .collect(Collectors.joining(","));

        mockMvc.perform(putItems("{\"items\": [" + items + "]}"))
                .andExpect(status().isBadRequest());

        verify(courseEditService, never()).updateItems(any());
    }

    @Test
    @DisplayName("본문 형식이 깨졌으면(배열 자리에 문자열) 500이 아니라 400 INVALID_REQUEST")
    void updateItems_returns400_whenBodyMalformed() throws Exception {
        mockMvc.perform(putItems("""
                        {"items": "abc"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verify(courseEditService, never()).updateItems(any());
    }

    @Test
    @DisplayName("동시 저장으로 낙관적 락 충돌이 나면 500이 아니라 409 CONCURRENT_MODIFICATION")
    void updateItems_returns409_whenConcurrentlyModified() throws Exception {
        given(courseEditService.updateItems(any()))
                .willThrow(new ObjectOptimisticLockingFailureException("generated_course_items", 10L));

        mockMvc.perform(putItems("""
                        {"items": [{"placeType": "SPOT", "originalId": "2760699"}]}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));
    }
}
