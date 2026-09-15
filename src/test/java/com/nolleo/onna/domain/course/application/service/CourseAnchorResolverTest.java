package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.EventCandidate;
import com.nolleo.onna.domain.course.application.dto.PendingChoice;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.EventLookupPort;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseAnchorResolverTest {

    @Mock EventLookupPort eventLookupPort;
    @Mock SpotLookupPort spotLookupPort;

    @InjectMocks CourseAnchorResolver resolver;

    private static final String NAME = "부산국제항만컨퍼런스";
    /** 해운대해수욕장 근방 좌표 → 가장 가까운 지원 지역은 해운대 */
    private static final EventCandidate EVENT = new EventCandidate("ev1", "부산국제항만컨퍼런스 2026",
            BigDecimal.valueOf(129.1604), BigDecimal.valueOf(35.1587),
            LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 16), "벡스코");
    private static final EventCandidate FILM = new EventCandidate("ev2", "부산국제영화제",
            BigDecimal.valueOf(129.1302), BigDecimal.valueOf(35.1690),
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), "영화의전당");

    private static CourseIntent intent(String area, CourseAnchor anchor) {
        return new CourseIntent(area, false, null, null, List.of(), null, false, List.of(), List.of(), anchor);
    }

    private void stubEvents(String name, EventCandidate... events) {
        given(eventLookupPort.findUpcomingByTitle(eq(name), anyInt())).willReturn(List.of(events));
    }

    private void stubSpots(String name, SpotCandidate... spots) {
        given(spotLookupPort.findActiveByTitleNear(eq(name),
                eq(CourseAnchorResolver.BUSAN_CENTER.latitude()), eq(CourseAnchorResolver.BUSAN_CENTER.longitude()), anyInt()))
                .willReturn(List.of(spots));
    }

    @Test
    @DisplayName("행사에서 하나 찾으면 좌표·기간을 채우고, 지역이 없으면 좌표에 가장 가까운 지원 지역을 startArea로 넣는다")
    void resolve_fromEvent_fillsPointAndNearestArea() {
        stubEvents(NAME, EVENT);

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent(null, CourseAnchor.of(NAME)));

        CourseAnchor anchor = result.intent().anchor();
        assertThat(result.choice()).isEmpty();
        assertThat(anchor.isResolved()).isTrue();
        assertThat(anchor.source()).isEqualTo(CourseAnchor.AnchorSource.EVENT);
        assertThat(anchor.point()).isEqualTo(new GeoPoint(35.1587, 129.1604));
        assertThat(anchor.period()).isEqualTo("10.14~10.16");
        assertThat(anchor.displayName()).isEqualTo("부산국제항만컨퍼런스(부산국제항만컨퍼런스 2026, 10.14~10.16)");
        assertThat(result.intent().startArea()).isEqualTo("해운대");
        assertThat(result.intent().center()).contains(anchor.point());
        verifyNoInteractions(spotLookupPort);
    }

    @Test
    @DisplayName("'부산국제'처럼 부분 일치 행사가 여러 개면 확정하지 않고 기간이 붙은 후보 선택지를 돌려준다")
    void resolve_ambiguousEvents_asksChoice() {
        stubEvents("부산국제", EVENT, FILM);

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent(null, CourseAnchor.of("부산국제")));

        assertThat(result.intent().hasUnresolvedAnchor()).isTrue();
        assertThat(result.intent().canGenerate()).isFalse();
        PendingChoice choice = result.choice().orElseThrow();
        assertThat(choice.kind()).isEqualTo(PendingChoice.Kind.ANCHOR);
        assertThat(choice.name()).isEqualTo("부산국제");
        assertThat(choice.candidates()).extracting(PendingChoice.Candidate::title)
                .containsExactly("부산국제항만컨퍼런스 2026", "부산국제영화제");
        assertThat(choice.candidates().get(0).detail()).isEqualTo("10.14~10.16");
        assertThat(choice.candidates().get(0).source()).isEqualTo("EVENT");
        assertThat(choice.candidates().get(1).latitude()).isEqualTo(35.1690);
        verifyNoInteractions(spotLookupPort); // 행사 후보가 있으면 스팟은 보지 않는다
    }

    @Test
    @DisplayName("AI가 '부산불꽃축제 행사 축제'처럼 일반어를 못 뗐으면 뒷단어를 떼고 다시 찾는다 — 이름에 붙은 '축제'는 건드리지 않는다")
    void resolve_retriesWithGenericWordsStripped() {
        EventCandidate fireworks = new EventCandidate("fw", "제20회 부산불꽃축제",
                BigDecimal.valueOf(129.1187), BigDecimal.valueOf(35.1531),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), "광안리해수욕장");
        stubEvents("부산불꽃축제 행사 축제");
        stubEvents("부산불꽃축제", fireworks);

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent(null, CourseAnchor.of("부산불꽃축제 행사 축제")));

        assertThat(result.intent().anchor().contentId()).isEqualTo("fw");
        assertThat(result.intent().startArea()).isEqualTo("광안리");
        assertThat(CourseAnchorResolver.keywords("부산불꽃축제 행사 축제")).containsExactly("부산불꽃축제 행사 축제", "부산불꽃축제");
        assertThat(CourseAnchorResolver.keywords("부산불꽃축제")).containsExactly("부산불꽃축제");
        assertThat(CourseAnchorResolver.keywords("축제")).containsExactly("축제"); // 이름 전체가 일반어면 그대로 둔다
    }

    @Test
    @DisplayName("사용자가 지역을 직접 말했으면 startArea는 유지하고 좌표만 채운다")
    void resolve_keepsExplicitStartArea() {
        stubEvents(NAME, EVENT);

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent("서면", CourseAnchor.of(NAME)));

        assertThat(result.intent().startArea()).isEqualTo("서면");
        assertThat(result.intent().center()).contains(new GeoPoint(35.1587, 129.1604));
    }

    @Test
    @DisplayName("행사에 없으면(또는 좌표가 없으면) 스팟 데이터에서 찾는다 — 스팟 검색은 부산 중심 좌표 기준")
    void resolve_fallsBackToSpot() {
        EventCandidate noCoords = new EventCandidate("ev9", "좌표 없는 행사", null, null, null, null, null);
        SpotCandidate beach = new SpotCandidate("beach", "광안리해수욕장", null, "NA", "자연/공원",
                BigDecimal.valueOf(129.1188), BigDecimal.valueOf(35.1532));
        stubEvents("광안리 바다", noCoords);
        stubSpots("광안리 바다", beach);

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent(null, CourseAnchor.of("광안리 바다")));

        CourseAnchor anchor = result.intent().anchor();
        assertThat(anchor.source()).isEqualTo(CourseAnchor.AnchorSource.SPOT);
        assertThat(anchor.period()).isNull();
        assertThat(anchor.displayName()).isEqualTo("광안리 바다(광안리해수욕장)");
        assertThat(result.intent().startArea()).isEqualTo("광안리");
    }

    @Test
    @DisplayName("스팟 후보가 여러 개면 역시 선택지로 돌려준다 (출처 SPOT, 기간 없음)")
    void resolve_ambiguousSpots_asksChoice() {
        SpotCandidate beach = new SpotCandidate("beach", "광안리해수욕장", null, "NA", "자연/공원",
                BigDecimal.valueOf(129.1188), BigDecimal.valueOf(35.1532));
        SpotCandidate songjeong = new SpotCandidate("songjeong", "송정해수욕장", null, "NA", "자연/공원",
                BigDecimal.valueOf(129.2005), BigDecimal.valueOf(35.1785));
        stubEvents("해수욕장");
        stubSpots("해수욕장", beach, songjeong);

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent(null, CourseAnchor.of("해수욕장")));

        PendingChoice choice = result.choice().orElseThrow();
        assertThat(choice.candidates()).extracting(PendingChoice.Candidate::source).containsOnly("SPOT");
        assertThat(choice.candidates()).extracting(PendingChoice.Candidate::detail).containsOnlyNulls();
    }

    @Test
    @DisplayName("어디에도 없으면 기준점은 미해결로, 지역도 비운 채 돌려준다 — 위치를 추측하지 않는다")
    void resolve_keepsUnresolved_whenNotFound() {
        stubEvents("없는행사");
        stubSpots("없는행사");

        CourseAnchorResolver.AnchorResolution result = resolver.resolve(intent(null, CourseAnchor.of("없는행사")));

        assertThat(result.choice()).isEmpty();
        assertThat(result.intent().hasUnresolvedAnchor()).isTrue();
        assertThat(result.intent().canGenerate()).isFalse();
        assertThat(result.intent().center()).isEmpty();
    }

    @Test
    @DisplayName("기준점이 없거나 이미 찾았으면 조회 없이 그대로 돌려준다")
    void resolve_noop_whenNothingToResolve() {
        CourseIntent noAnchor = intent("광안리", null);
        CourseIntent alreadyResolved = intent("광안리", CourseAnchor.of(NAME)
                .resolvedTo(CourseAnchor.AnchorSource.EVENT, "ev1", NAME, new GeoPoint(35.1, 129.1), null));

        assertThat(resolver.resolve(noAnchor).intent()).isSameAs(noAnchor);
        assertThat(resolver.resolve(alreadyResolved).intent()).isSameAs(alreadyResolved);
        verifyNoInteractions(eventLookupPort, spotLookupPort);
    }
}
