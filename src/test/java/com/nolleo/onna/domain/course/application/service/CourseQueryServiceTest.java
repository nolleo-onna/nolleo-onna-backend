package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.application.port.UserLookupPort.UserProfile;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.dto.response.PublicCourseResponse;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CourseSort;
import com.nolleo.onna.domain.course.domain.model.vo.GenerationMode;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import com.nolleo.onna.domain.course.domain.model.vo.ShareInfo;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseQueryServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock SpotLookupPort spotLookupPort;
    @Mock UserLookupPort userLookupPort;

    @InjectMocks CourseQueryService service;

    private static final CourseIntent INTENT =
            new CourseIntent("광안리", false, null, null, List.of(), null, false);
    private static final SpotCandidate A = new SpotCandidate("A", "A명", "https://img/a.jpg", "NA", "자연/공원",
            BigDecimal.valueOf(129.1190), BigDecimal.valueOf(35.1540));
    private static final SpotCandidate B = new SpotCandidate("B", "B명", null, "VE", "관광지",
            BigDecimal.valueOf(129.1200), BigDecimal.valueOf(35.1550));
    private static final SpotCandidate C = new SpotCandidate("C", "C명", "https://img/c.jpg", "VE", "관광지",
            BigDecimal.valueOf(129.1210), BigDecimal.valueOf(35.1560));

    /** 방문 순서대로 스팟 contentId를 담은 공개 코스 */
    private static Course publicCourse(long id, long userId, String token, int viewCount, int likeCount,
                                       String... spotIds) {
        List<CourseItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < spotIds.length; i++) {
            items.add(CourseItem.restore((long) i + 1, id, (short) (i + 1), PlaceRef.spot(spotIds[i]), null, 100));
        }
        return Course.restore(id, userId, UUID.randomUUID(), GenerationMode.AI, null,
                "제목" + id, "소개" + id, INTENT, 15000, ShareInfo.of(true, token, viewCount, likeCount),
                items, OffsetDateTime.parse("2026-09-10T10:00:00+09:00"), "AI_CHAT");
    }

    // ── 공개 코스 목록 (인기순) ───────────────────────────────────────────────

    @Test
    @DisplayName("공개 코스를 저장소가 준 순서 그대로 카드로 만들고, 스팟 이름은 방문 순서·작성자는 닉네임으로 붙인다")
    void getPublicCourses_mapsCards_inRepositoryOrder() {
        Course first = publicCourse(10L, 1L, "tok-10", 42, 7, "A", "B");
        Course second = publicCourse(11L, 2L, "tok-11", 5, 0, "B");
        given(courseRepository.findPublic(CourseSort.VIEWS, 0, 6)).willReturn(List.of(first, second));
        given(spotLookupPort.findByIds(anyList())).willReturn(Map.of("A", A, "B", B));
        given(userLookupPort.findByIds(anyCollection()))
                .willReturn(Map.of(1L, new UserProfile("부산러버", "https://img/1.png"),
                                   2L, new UserProfile("해운대", null)));

        List<PublicCourseResponse> cards = service.getPublicCourses(CourseSort.VIEWS, 0, 6);

        assertThat(cards).extracting(PublicCourseResponse::shareToken).containsExactly("tok-10", "tok-11");
        PublicCourseResponse card = cards.get(0);
        assertThat(card.title()).isEqualTo("제목10");
        assertThat(card.description()).isEqualTo("소개10");
        assertThat(card.totalCost()).isEqualTo(15000);
        assertThat(card.spotTitles()).containsExactly("A명", "B명");
        assertThat(card.thumbnailImageUrl()).isEqualTo("https://img/a.jpg");
        assertThat(card.authorNickname()).isEqualTo("부산러버");
        assertThat(card.authorProfileImageUrl()).isEqualTo("https://img/1.png");
        assertThat(card.viewCount()).isEqualTo(42);
        assertThat(card.likeCount()).isEqualTo(7);
        assertThat(card.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-10T10:00:00+09:00"));
        assertThat(cards.get(1).authorNickname()).isEqualTo("해운대");
        assertThat(cards.get(1).authorProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("스팟과 작성자는 목록 전체를 묶어 각각 한 번씩만 조회한다 (N+1 없음)")
    void getPublicCourses_batchesSpotAndAuthorLookups() {
        given(courseRepository.findPublic(CourseSort.VIEWS, 0, 6)).willReturn(List.of(
                publicCourse(10L, 1L, "tok-10", 3, 0, "A", "B"),
                publicCourse(11L, 2L, "tok-11", 2, 0, "B", "C"),
                publicCourse(12L, 1L, "tok-12", 1, 0, "C")));
        given(spotLookupPort.findByIds(anyList())).willReturn(Map.of("A", A, "B", B, "C", C));
        given(userLookupPort.findByIds(anyCollection())).willReturn(Map.of());

        service.getPublicCourses(CourseSort.VIEWS, 0, 6);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> spotIds = ArgumentCaptor.forClass(List.class);
        verify(spotLookupPort).findByIds(spotIds.capture());
        assertThat(spotIds.getValue()).containsExactlyInAnyOrder("A", "B", "C"); // 중복 제거

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> authorIds = ArgumentCaptor.forClass(Collection.class);
        verify(userLookupPort).findByIds(authorIds.capture());
        assertThat(authorIds.getValue()).containsExactlyInAnyOrder(1L, 2L);   // 같은 작성자는 한 번
        verify(userLookupPort, never()).findById(any());
    }

    @Test
    @DisplayName("첫 스팟에 이미지가 없으면 순서상 다음 스팟의 이미지를 썸네일로 쓰고, 하나도 없으면 null")
    void getPublicCourses_thumbnailFallsBackToNextSpotImage() {
        given(courseRepository.findPublic(CourseSort.VIEWS, 0, 6)).willReturn(List.of(
                publicCourse(10L, 1L, "tok-10", 3, 0, "B", "C"),
                publicCourse(11L, 1L, "tok-11", 2, 0, "B")));
        given(spotLookupPort.findByIds(anyList())).willReturn(Map.of("B", B, "C", C));
        given(userLookupPort.findByIds(anyCollection())).willReturn(Map.of());

        List<PublicCourseResponse> cards = service.getPublicCourses(CourseSort.VIEWS, 0, 6);

        assertThat(cards.get(0).thumbnailImageUrl()).isEqualTo("https://img/c.jpg");
        assertThat(cards.get(1).thumbnailImageUrl()).isNull();
    }

    @Test
    @DisplayName("탈퇴한 작성자(조회 결과에 없음)는 닉네임·프로필이 null이고, 조회에 없는 스팟은 이름 자리가 null이다")
    void getPublicCourses_toleratesMissingAuthorAndSpot() {
        given(courseRepository.findPublic(CourseSort.VIEWS, 0, 6))
                .willReturn(List.of(publicCourse(10L, 99L, "tok-10", 3, 0, "A", "ZZ")));
        given(spotLookupPort.findByIds(anyList())).willReturn(Map.of("A", A));
        given(userLookupPort.findByIds(anyCollection())).willReturn(Map.of());

        PublicCourseResponse card = service.getPublicCourses(CourseSort.VIEWS, 0, 6).get(0);

        assertThat(card.authorNickname()).isNull();
        assertThat(card.authorProfileImageUrl()).isNull();
        assertThat(card.spotTitles()).containsExactly("A명", null);
        assertThat(card.thumbnailImageUrl()).isEqualTo("https://img/a.jpg");
    }

    @Test
    @DisplayName("공개 코스가 없으면 빈 목록을 돌려주고 스팟·작성자를 조회하지 않는다")
    void getPublicCourses_returnsEmpty_withoutLookups() {
        given(courseRepository.findPublic(CourseSort.VIEWS, 3, 6)).willReturn(List.of());

        List<PublicCourseResponse> cards = service.getPublicCourses(CourseSort.VIEWS, 3, 6);

        assertThat(cards).isEmpty();
        verify(spotLookupPort, never()).findByIds(anyList());
        verify(userLookupPort, never()).findByIds(anyCollection());
    }

    @Test
    @DisplayName("정렬 기준·page·size는 그대로 저장소에 전달된다 — 정렬 구현과 상한 처리는 각각 저장소·컨트롤러의 책임")
    void getPublicCourses_passesSortAndPagingThrough() {
        given(courseRepository.findPublic(CourseSort.LATEST, 2, 9)).willReturn(List.of());
        given(courseRepository.findPublic(CourseSort.LIKES, 0, 6)).willReturn(List.of());

        service.getPublicCourses(CourseSort.LATEST, 2, 9);
        service.getPublicCourses(CourseSort.LIKES, 0, 6);

        verify(courseRepository).findPublic(CourseSort.LATEST, 2, 9);
        verify(courseRepository).findPublic(CourseSort.LIKES, 0, 6);
    }

    @Test
    @DisplayName("응답에 코스 id·userId·pairId가 없다 — 공개 영역의 식별자는 shareToken 하나")
    void getPublicCourses_exposesOnlyShareTokenAsIdentifier() {
        Set<String> fields = java.util.Arrays.stream(PublicCourseResponse.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(fields).contains("shareToken").doesNotContain("id", "courseId", "userId", "pairId");
    }
}
