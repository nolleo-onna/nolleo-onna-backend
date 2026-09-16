package com.nolleo.onna.domain.course.infrastructure.persistence.repository;

import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
import com.nolleo.onna.domain.course.domain.model.vo.CourseSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 공개 코스 목록의 정렬 기준 → JPA Sort 변환과 2단계 조회 흐름을 본다.
 * 실제 SQL 정렬 결과는 DB 연동으로만 확인되며, 여기서는 "어떤 컬럼을 어떤 순서로 요청하는가"를 고정한다.
 */
@ExtendWith(MockitoExtension.class)
class CourseRepositoryImplTest {

    @Mock CourseJpaRepository jpaRepository;

    @InjectMocks CourseRepositoryImpl repository;

    private static List<String> orders(Sort sort) {
        return sort.stream().map(o -> o.getProperty() + " " + o.getDirection()).toList();
    }

    @Test
    @DisplayName("VIEWS: 조회수 내림차순 → 최신순 → id 내림차순")
    void toSort_views() {
        assertThat(orders(CourseRepositoryImpl.toSort(CourseSort.VIEWS)))
                .containsExactly("viewCount DESC", "createAudit.createdAt DESC", "id DESC");
    }

    @Test
    @DisplayName("LIKES: 좋아요 내림차순 → 최신순 → id 내림차순")
    void toSort_likes() {
        assertThat(orders(CourseRepositoryImpl.toSort(CourseSort.LIKES)))
                .containsExactly("likeCount DESC", "createAudit.createdAt DESC", "id DESC");
    }

    @Test
    @DisplayName("LATEST: 최신순 → id 내림차순 (카운터는 보지 않는다)")
    void toSort_latest() {
        assertThat(orders(CourseRepositoryImpl.toSort(CourseSort.LATEST)))
                .containsExactly("createAudit.createdAt DESC", "id DESC");
    }

    @Test
    @DisplayName("findPublic은 정렬·페이지를 담은 Pageable로 id를 먼저 조회하고, 결과가 없으면 본문 조회를 건너뛴다")
    void findPublic_pagesIdsWithSort_andSkipsBodyFetchWhenEmpty() {
        given(jpaRepository.findPublicIds(any(Pageable.class))).willReturn(List.of());

        List<Course> result = repository.findPublic(CourseSort.LIKES, 2, 9);

        assertThat(result).isEmpty();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findPublicIds(pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(9);
        assertThat(orders(pageable.getValue().getSort())).startsWith("likeCount DESC");
        verify(jpaRepository, never()).findWithItemsByIdIn(anyCollection());
    }

    @Test
    @DisplayName("findRecentSpotContentIds는 최신순 id를 courseLimit개 뗀 뒤 그 코스들의 SPOT originalId를 집합으로 돌려준다")
    void findRecentSpotContentIds_twoStep() {
        given(jpaRepository.findRecentIdsByUserId(eq(7L), any(Pageable.class))).willReturn(List.of(30L, 20L, 10L));
        given(jpaRepository.findOriginalIdsByCourseIdIn(List.of(30L, 20L, 10L), CoursePlaceType.SPOT))
                .willReturn(List.of("s1", "s2", "s1"));

        Set<String> result = repository.findRecentSpotContentIds(7L, 5);

        assertThat(result).containsExactlyInAnyOrder("s1", "s2");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jpaRepository).findRecentIdsByUserId(eq(7L), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(orders(pageable.getValue().getSort())).containsExactly("createAudit.createdAt DESC", "id DESC");
    }

    @Test
    @DisplayName("findRecentSpotContentIds는 코스가 없으면 아이템 조회를 건너뛰고, courseLimit이 0 이하면 조회 자체를 하지 않는다")
    void findRecentSpotContentIds_skipsWhenNoCourses() {
        given(jpaRepository.findRecentIdsByUserId(eq(7L), any(Pageable.class))).willReturn(List.of());

        assertThat(repository.findRecentSpotContentIds(7L, 5)).isEmpty();
        verify(jpaRepository, never()).findOriginalIdsByCourseIdIn(anyCollection(), any());

        assertThat(repository.findRecentSpotContentIds(7L, 0)).isEmpty();
        verify(jpaRepository, times(1)).findRecentIdsByUserId(eq(7L), any(Pageable.class));
    }
}
