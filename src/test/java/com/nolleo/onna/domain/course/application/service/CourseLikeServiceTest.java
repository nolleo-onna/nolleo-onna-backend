package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.application.dto.response.CourseLikeToggleResponse;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.domain.repository.CourseLikeRepository;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseLikeServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock CourseLikeRepository courseLikeRepository;

    @InjectMocks CourseLikeService service;

    private static final Long OWNER = 1L;
    private static final Long USER = 7L;
    private static final Long COURSE_ID = 10L;
    private static final String TOKEN = "share-token";
    private static final CourseIntent INTENT =
            new CourseIntent("광안리", false, null, null, List.of(), null, false);

    private static Course publicCourse(int likeCount) {
        return Course.restore(COURSE_ID, OWNER, UUID.randomUUID(), GenerationMode.AI, null,
                "제목", "소개", INTENT, null, ShareInfo.of(true, TOKEN, 0, likeCount),
                List.of(), OffsetDateTime.now(), "AI_CHAT");
    }

    private void stubPublicCourse(int likeCount) {
        given(courseRepository.findPublicByShareToken(TOKEN)).willReturn(Optional.of(publicCourse(likeCount)));
    }

    @Test
    @DisplayName("좋아요가 없으면 추가하고 like_count 를 올려 liked=true 와 반영된 수를 돌려준다")
    void toggle_adds_whenNotLiked() {
        stubPublicCourse(7);
        given(courseLikeRepository.exists(COURSE_ID, USER)).willReturn(false);
        given(courseLikeRepository.add(COURSE_ID, USER)).willReturn(true);
        given(courseRepository.incrementLikeCount(COURSE_ID)).willReturn(8);

        CourseLikeToggleResponse response = service.toggle(TOKEN, USER);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(8);
        verify(courseLikeRepository, never()).remove(anyLong(), anyLong());
        verify(courseRepository, never()).decrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("좋아요가 있으면 삭제하고 like_count 를 내려 liked=false 와 반영된 수를 돌려준다")
    void toggle_removes_whenLiked() {
        stubPublicCourse(7);
        given(courseLikeRepository.exists(COURSE_ID, USER)).willReturn(true);
        given(courseLikeRepository.remove(COURSE_ID, USER)).willReturn(true);
        given(courseRepository.decrementLikeCount(COURSE_ID)).willReturn(6);

        CourseLikeToggleResponse response = service.toggle(TOKEN, USER);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(6);
        verify(courseLikeRepository, never()).add(anyLong(), anyLong());
        verify(courseRepository, never()).incrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("동시 좋아요가 먼저 행을 넣어 add 가 no-op 이면 카운터를 올리지 않고 현재 값으로 liked=true 를 돌려준다")
    void toggle_doesNotIncrement_whenConcurrentAddWonTheRace() {
        stubPublicCourse(7);
        given(courseLikeRepository.exists(COURSE_ID, USER)).willReturn(false); // 판정 시점엔 없었다
        given(courseLikeRepository.add(COURSE_ID, USER)).willReturn(false);    // 삽입 시점엔 이미 있었다 (ON CONFLICT)
        given(courseRepository.findLikeCount(COURSE_ID)).willReturn(8);        // 경쟁 요청이 이미 올린 값

        CourseLikeToggleResponse response = service.toggle(TOKEN, USER);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(8);
        verify(courseRepository, never()).incrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("동시 취소가 먼저 행을 지워 remove 가 no-op 이면 카운터를 내리지 않고 현재 값으로 liked=false 를 돌려준다")
    void toggle_doesNotDecrement_whenConcurrentRemoveWonTheRace() {
        stubPublicCourse(7);
        given(courseLikeRepository.exists(COURSE_ID, USER)).willReturn(true);
        given(courseLikeRepository.remove(COURSE_ID, USER)).willReturn(false);
        given(courseRepository.findLikeCount(COURSE_ID)).willReturn(6);

        CourseLikeToggleResponse response = service.toggle(TOKEN, USER);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(6);
        verify(courseRepository, never()).decrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("코스 소유자 본인도 좋아요를 누를 수 있다")
    void toggle_allowsOwner() {
        stubPublicCourse(0);
        given(courseLikeRepository.exists(COURSE_ID, OWNER)).willReturn(false);
        given(courseLikeRepository.add(COURSE_ID, OWNER)).willReturn(true);
        given(courseRepository.incrementLikeCount(COURSE_ID)).willReturn(1);

        CourseLikeToggleResponse response = service.toggle(TOKEN, OWNER);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("토큰이 없거나 비공개·삭제된 코스면 COURSE_NOT_FOUND — 좋아요 행과 카운터를 건드리지 않는다")
    void toggle_throws_whenNotPublicOrMissing() {
        given(courseRepository.findPublicByShareToken("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle("unknown", USER))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CourseErrorCode.COURSE_NOT_FOUND);
        verify(courseLikeRepository, never()).exists(anyLong(), anyLong());
        verify(courseLikeRepository, never()).add(anyLong(), anyLong());
        verify(courseLikeRepository, never()).remove(anyLong(), anyLong());
        verify(courseRepository, never()).incrementLikeCount(anyLong());
        verify(courseRepository, never()).decrementLikeCount(anyLong());
    }
}
