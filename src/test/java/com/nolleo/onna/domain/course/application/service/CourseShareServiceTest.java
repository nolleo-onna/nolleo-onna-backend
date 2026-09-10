package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.UpdateCourseVisibilityCommand;
import com.nolleo.onna.domain.course.application.dto.response.CourseItemResponse;
import com.nolleo.onna.domain.course.application.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.application.dto.response.SharedCourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.domain.service.ShareTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseShareServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock SpotLookupPort spotLookupPort;
    @Mock UserLookupPort userLookupPort;

    @InjectMocks CourseShareService service;

    private static final Long OWNER = 1L;
    private static final Long COURSE_ID = 10L;
    private static final String TOKEN = "existing-token";
    private static final CourseIntent INTENT =
            new CourseIntent("광안리", false, null, null, List.of(), null, false);
    private static final SpotCandidate A = new SpotCandidate("A", "A명", null, "NA", "자연/공원",
            BigDecimal.valueOf(129.1190), BigDecimal.valueOf(35.1540));

    /** 아이템 1개(A)를 가진 저장된 코스 — share 상태를 지정한다 */
    private static Course savedCourse(Long userId, ShareInfo share) {
        List<CourseItem> items = List.of(
                CourseItem.restore(1L, COURSE_ID, (short) 1, PlaceRef.spot("A"), null, 100));
        return Course.restore(COURSE_ID, userId, UUID.randomUUID(), GenerationMode.AI, null,
                "제목", "소개", INTENT, null, share, items, OffsetDateTime.now(), "AI_CHAT");
    }

    private void stubOwnerCourse(ShareInfo share) {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(savedCourse(OWNER, share)));
    }

    private void stubSaveShareStateReturnsArgument() {
        given(courseRepository.saveShareState(any(Course.class), eq(String.valueOf(OWNER))))
                .willAnswer(inv -> inv.getArgument(0));
    }

    private void stubSpots() {
        given(spotLookupPort.findByIds(List.of("A"))).willReturn(Map.of("A", A));
    }

    // ── 공개 전환 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("소유자가 공개 전환하면 토큰이 발급되고 공유 상태만 저장되며, 응답 share에 토큰이 담긴다")
    void updateVisibility_publishes_andIssuesToken() {
        stubOwnerCourse(ShareInfo.initial());
        stubSaveShareStateReturnsArgument();
        stubSpots();

        CourseResponse response = service.updateVisibility(new UpdateCourseVisibilityCommand(COURSE_ID, OWNER, true));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).saveShareState(captor.capture(), eq("1"));
        Course saved = captor.getValue();
        assertThat(saved.isPublic()).isTrue();
        assertThat(saved.getShareInfo().shareToken())
                .hasSize(ShareTokenGenerator.TOKEN_LENGTH)
                .matches("[A-Za-z0-9_-]+");

        assertThat(response.share().isPublic()).isTrue();
        assertThat(response.share().shareToken()).isEqualTo(saved.getShareInfo().shareToken());
        assertThat(response.items()).extracting(CourseItemResponse::title).containsExactly("A명");
        verify(courseRepository, never()).saveEdited(any(), anyString()); // 편집 경로를 타지 않는다
    }

    @Test
    @DisplayName("비공개 전환은 토큰·조회수·좋아요를 보존한다")
    void updateVisibility_unpublishes_andKeepsTokenAndCounters() {
        stubOwnerCourse(ShareInfo.of(true, TOKEN, 42, 7));
        stubSaveShareStateReturnsArgument();
        stubSpots();

        CourseResponse response = service.updateVisibility(new UpdateCourseVisibilityCommand(COURSE_ID, OWNER, false));

        assertThat(response.share().isPublic()).isFalse();
        assertThat(response.share().shareToken()).isEqualTo(TOKEN);
        assertThat(response.share().viewCount()).isEqualTo(42);
        assertThat(response.share().likeCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("비공개였다가 다시 공개하면 기존 토큰이 그대로 쓰여 같은 링크가 살아난다")
    void updateVisibility_republish_reusesToken() {
        stubOwnerCourse(ShareInfo.of(false, TOKEN, 42, 7));
        stubSaveShareStateReturnsArgument();
        stubSpots();

        CourseResponse response = service.updateVisibility(new UpdateCourseVisibilityCommand(COURSE_ID, OWNER, true));

        assertThat(response.share().isPublic()).isTrue();
        assertThat(response.share().shareToken()).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("타인이 전환을 시도하면 COURSE_ACCESS_DENIED — 저장하지 않는다 (IDOR)")
    void updateVisibility_throws_whenNotOwner() {
        stubOwnerCourse(ShareInfo.initial());

        assertThatThrownBy(() -> service.updateVisibility(new UpdateCourseVisibilityCommand(COURSE_ID, 99L, true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_ACCESS_DENIED);
        verify(courseRepository, never()).saveShareState(any(), anyString());
    }

    @Test
    @DisplayName("없는 코스면 COURSE_NOT_FOUND")
    void updateVisibility_throws_whenCourseNotFound() {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateVisibility(new UpdateCourseVisibilityCommand(COURSE_ID, OWNER, true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_NOT_FOUND);
        verify(courseRepository, never()).saveShareState(any(), anyString());
    }

    // ── 공유 링크 조회 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("공개 코스를 토큰으로 조회하면 조회수를 원자 증가시키고, 작성자 닉네임과 스팟 상세를 병합한 응답을 돌려준다")
    void getShared_returnsCourse_andIncrementsViewCount() {
        given(courseRepository.findPublicByShareToken(TOKEN))
                .willReturn(Optional.of(savedCourse(OWNER, ShareInfo.of(true, TOKEN, 42, 7))));
        given(userLookupPort.findById(OWNER)).willReturn(Optional.of(new UserProfile("부산러버", "https://img/1.png")));
        stubSpots();

        SharedCourseResponse response = service.getShared(TOKEN);

        verify(courseRepository).incrementViewCount(COURSE_ID);
        assertThat(response.id()).isEqualTo(COURSE_ID);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.authorNickname()).isEqualTo("부산러버");
        assertThat(response.authorProfileImageUrl()).isEqualTo("https://img/1.png");
        assertThat(response.viewCount()).isEqualTo(43); // 이번 조회 포함
        assertThat(response.likeCount()).isEqualTo(7);
        assertThat(response.items()).extracting(CourseItemResponse::title).containsExactly("A명");
    }

    @Test
    @DisplayName("작성자 프로필을 찾지 못해도(탈퇴 등) 코스는 열리고 닉네임만 null이다")
    void getShared_succeeds_whenAuthorMissing() {
        given(courseRepository.findPublicByShareToken(TOKEN))
                .willReturn(Optional.of(savedCourse(OWNER, ShareInfo.of(true, TOKEN, 0, 0))));
        given(userLookupPort.findById(OWNER)).willReturn(Optional.empty());
        stubSpots();

        SharedCourseResponse response = service.getShared(TOKEN);

        assertThat(response.authorNickname()).isNull();
        assertThat(response.authorProfileImageUrl()).isNull();
        assertThat(response.title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("토큰이 없거나 비공개·삭제된 코스면 COURSE_NOT_FOUND — 조회수도 올리지 않는다")
    void getShared_throws_whenNotPublicOrMissing() {
        given(courseRepository.findPublicByShareToken("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShared("unknown"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_NOT_FOUND);
        verify(courseRepository, never()).incrementViewCount(anyLong());
        verify(userLookupPort, never()).findById(anyLong());
    }
}
