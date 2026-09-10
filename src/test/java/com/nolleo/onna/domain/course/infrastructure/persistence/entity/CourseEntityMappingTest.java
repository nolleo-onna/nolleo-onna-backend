package com.nolleo.onna.domain.course.infrastructure.persistence.entity;

import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.model.CourseItem;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.CoursePlaceType;
import com.nolleo.onna.domain.course.domain.model.vo.PlaceRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** DB 없이 도메인 ↔ 엔티티 변환만 검증한다. 테이블 변경(place_type/original_id/like_count)이 매핑에 반영됐는지 본다. */
class CourseEntityMappingTest {

    private static final CourseIntent INTENT =
            new CourseIntent("광안리", false, 50000, "연인", List.of("로맨틱"), null, false);

    @Test
    @DisplayName("도메인 → 엔티티 → 도메인 왕복 시 PlaceRef·순번·비용·거리·좋아요 수가 보존된다")
    void roundTrip_preservesItemsAndShareInfo() {
        Course course = Course.createByAi(7L, UUID.randomUUID(), INTENT, "AI_CHAT");
        course.addItem(PlaceRef.spot("2760699"), null, 420);
        course.addItem(PlaceRef.spot("1924688"), 18000, 850);
        course.applyAiContent("광안리 데이트", "소개");

        CourseEntity entity = CourseEntity.fromDomain(course);
        Course restored = entity.toDomain();

        assertThat(restored.getUserId()).isEqualTo(7L);
        assertThat(restored.getPairId()).isEqualTo(course.getPairId());
        assertThat(restored.getTitle()).isEqualTo("광안리 데이트");
        assertThat(restored.getTotalCost()).isEqualTo(18000);
        assertThat(restored.getShareInfo().likeCount()).isZero();
        assertThat(restored.getShareInfo().isPublic()).isFalse();
        assertThat(restored.getIntent().startArea()).isEqualTo("광안리");

        List<CourseItem> items = restored.getItems();
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getSerialNum()).isEqualTo((short) 1);
        assertThat(items.get(0).getPlaceRef()).isEqualTo(PlaceRef.spot("2760699"));
        assertThat(items.get(0).getExpectedCost()).isNull();
        assertThat(items.get(0).getDistanceFromPrevM()).isEqualTo(420);
        assertThat(items.get(1).getPlaceRef()).isEqualTo(PlaceRef.spot("1924688"));
        assertThat(items.get(1).getExpectedCost()).isEqualTo(18000);
    }

    @Test
    @DisplayName("아이템 엔티티는 PlaceRef를 place_type + original_id 두 컬럼으로 분리해 담는다")
    void itemEntity_splitsPlaceRefIntoTwoColumns() {
        Course course = Course.createByAi(1L, UUID.randomUUID(), INTENT, "AI_CHAT");
        course.addItem(PlaceRef.spot("2760699"), null, 0);

        CourseEntity entity = CourseEntity.fromDomain(course);
        CourseItemEntity itemEntity = entity.getItems().get(0);

        assertThat(itemEntity.getPlaceType()).isEqualTo(CoursePlaceType.SPOT);
        assertThat(itemEntity.getOriginalId()).isEqualTo("2760699");
        assertThat(itemEntity.getCourse()).isSameAs(entity);
        assertThat(itemEntity.toDomain().getPlaceRef()).isEqualTo(PlaceRef.spot("2760699"));
    }

    @Test
    @DisplayName("32,767m를 넘는 거리도 엔티티에 손실 없이 담긴다 (SMALLINT → INTEGER)")
    void itemEntity_holdsLongDistance() {
        Course course = Course.createByAi(1L, UUID.randomUUID(), INTENT, "AI_CHAT");
        course.addItem(PlaceRef.spot("A"), null, 48_000);

        CourseItemEntity itemEntity = CourseEntity.fromDomain(course).getItems().get(0);

        assertThat(itemEntity.getDistanceFromPrevM()).isEqualTo(48_000);
    }

    @Test
    @DisplayName("applyEdit는 편집된 제목·소개·아이템을 반영하고, 다시 담긴 아이템 행의 created_by는 편집한 사용자다")
    void applyEdit_appliesContent_andRecordsEditorAsItemCreator() {
        Course course = Course.createByAi(7L, UUID.randomUUID(), INTENT, "AI_CHAT");
        course.applyAiContent("AI 제목", "AI 소개");
        course.addItem(PlaceRef.spot("2760699"), null, 420);
        CourseEntity entity = CourseEntity.fromDomain(course);
        assertThat(entity.getItems().get(0).getCreateAudit().getCreatedBy()).isEqualTo("AI_CHAT");

        course.edit("내가 고친 제목", null, List.of(new Course.VisitStop(PlaceRef.spot("1924688"), 35.1540, 129.1190, null)));
        entity.clearItems();
        entity.applyEdit(course, "7");

        assertThat(entity.getTitle()).isEqualTo("내가 고친 제목");
        assertThat(entity.getDescription()).isNull();
        assertThat(entity.getCreateAudit().getCreatedBy()).isEqualTo("AI_CHAT");
        assertThat(entity.isPublic()).isFalse(); // 공유 상태는 편집 대상이 아니다
        assertThat(entity.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getOriginalId()).isEqualTo("1924688");
                    assertThat(item.getCreateAudit().getCreatedBy()).isEqualTo("7");
                });
    }
}
