package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.PendingChoice;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SpotPinResolverTest {

    @Mock SpotLookupPort spotLookupPort;

    @InjectMocks SpotPinResolver resolver;

    private static final double LAT = DistrictCenter.GWANGAN.getLatitude();
    private static final double LON = DistrictCenter.GWANGAN.getLongitude();

    private static SpotCandidate spot(String id, String title, double lat, double lon) {
        return new SpotCandidate(id, title, null, "NA", "자연/공원", BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
    }

    private static final SpotCandidate BEACH = spot("beach", "광안리해수욕장", 35.1532, 129.1188);   // 약 0.0km
    private static final SpotCandidate MILLAK = spot("millak", "민락해변", 35.1600, 129.1300);      // 약 1.3km
    private static final SpotCandidate SONGJEONG = spot("songjeong", "송정해수욕장", 35.1785, 129.2005);

    private static CourseIntent intent(String area, List<SpotPin> include, List<SpotPin> exclude) {
        return new CourseIntent(area, false, null, null, List.of(), null, false, include, exclude);
    }

    private void stubSearch(String keyword, SpotCandidate... results) {
        given(spotLookupPort.findActiveByTitleNear(eq(keyword), eq(LAT), eq(LON), anyInt())).willReturn(List.of(results));
    }

    @Test
    @DisplayName("결과가 하나면 확정 — contentId·제목을 채우고 원문 이름은 보존한다")
    void resolve_singleMatch_resolves() {
        stubSearch("광안리 바다", BEACH);

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(SpotPin.of("광안리 바다")), List.of()));

        assertThat(result.intent().includeSpots()).containsExactly(new SpotPin("광안리 바다", "beach", "광안리해수욕장"));
        assertThat(result.choices()).isEmpty();
        assertThat(result.intent().hasUnresolvedPins()).isFalse();
    }

    @Test
    @DisplayName("부분 일치가 여러 개여도 정확히 일치하는 제목이 있으면 묻지 않고 그것으로 확정한다")
    void resolve_exactMatchWins_amongMany() {
        stubSearch("광안리해수욕장", MILLAK, BEACH); // DB가 거리순으로 민락을 먼저 줘도 정확 일치가 이긴다

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(SpotPin.of("광안리해수욕장")), List.of()));

        assertThat(result.intent().includeSpots().get(0).contentId()).isEqualTo("beach");
        assertThat(result.choices()).isEmpty();
    }

    @Test
    @DisplayName("꼭 넣을 곳의 부분 일치가 여러 개면 확정하지 않고 거리 라벨이 붙은 후보 선택지를 돌려준다")
    void resolve_ambiguousInclude_asksChoice() {
        stubSearch("해수욕장", BEACH, SONGJEONG);

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(SpotPin.of("해수욕장")), List.of()));

        assertThat(result.intent().hasUnresolvedPins()).isTrue();
        assertThat(result.choices()).hasSize(1);
        PendingChoice choice = result.choices().get(0);
        assertThat(choice.kind()).isEqualTo(PendingChoice.Kind.INCLUDE);
        assertThat(choice.name()).isEqualTo("해수욕장");
        assertThat(choice.candidates()).extracting(PendingChoice.Candidate::contentId).containsExactly("beach", "songjeong");
        assertThat(choice.candidates().get(0).detail()).isEqualTo("0.0km");
        assertThat(choice.candidates().get(1).detail()).matches("\\d+\\.\\dkm");
        assertThat(choice.candidates().get(0).latitude()).isEqualTo(35.1532);
    }

    @Test
    @DisplayName("뺄 곳은 묻지 않고 걸린 후보를 전부 뺀다 — 첫 번째는 원문 pin에, 나머지는 제목을 이름으로 하는 pin으로")
    void resolve_ambiguousExclude_excludesAll() {
        stubSearch("해수욕장", BEACH, SONGJEONG);

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(), List.of(SpotPin.of("해수욕장"))));

        assertThat(result.choices()).isEmpty();
        assertThat(result.intent().excludeSpots()).containsExactly(
                new SpotPin("해수욕장", "beach", "광안리해수욕장"),
                new SpotPin("송정해수욕장", "songjeong", "송정해수욕장"));
        assertThat(result.intent().hasUnresolvedPins()).isFalse();
    }

    @Test
    @DisplayName("원문으로 못 찾으면 동의어(바다 → 해수욕장) 변형으로 다시 찾고, 원문 이름은 그대로 보존한다")
    void resolve_triesSynonymVariant_whenOriginalNotFound() {
        stubSearch("광안리 바다");
        stubSearch("광안리 해수욕장", BEACH);

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(SpotPin.of("광안리 바다")), List.of()));

        SpotPin pin = result.intent().includeSpots().get(0);
        assertThat(pin.name()).isEqualTo("광안리 바다");
        assertThat(pin.matchedTitle()).isEqualTo("광안리해수욕장");
        assertThat(pin.displayName()).isEqualTo("광안리해수욕장(광안리 바다)");
    }

    @Test
    @DisplayName("동의어로도 못 찾으면 AI 공식 명칭 힌트로 마지막 시도를 한다 — 힌트는 검색어일 뿐 판단은 DB가 한다")
    void resolve_triesHint_last() {
        stubSearch("자갈치");
        SpotCandidate market = spot("market", "자갈치시장", 35.0966, 129.0306);
        stubSearch("자갈치시장", market);

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(SpotPin.of("자갈치", "자갈치시장")), List.of()));

        assertThat(result.intent().includeSpots().get(0).contentId()).isEqualTo("market");
    }

    @Test
    @DisplayName("이름에 맞는 스팟이 없으면 미해결 그대로 두고 선택지도 만들지 않는다 — 확인 문구에서 사용자에게 알린다")
    void resolve_keepsUnresolved_whenNotFound() {
        stubSearch("없는 곳");

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(), List.of(SpotPin.of("없는 곳"))));

        assertThat(result.intent().excludeSpots()).containsExactly(SpotPin.of("없는 곳"));
        assertThat(result.intent().hasUnresolvedPins()).isTrue();
        assertThat(result.choices()).isEmpty();
    }

    @Test
    @DisplayName("이미 해결된 지정은 다시 조회하지 않는다")
    void resolve_skipsAlreadyResolved() {
        SpotPin resolved = new SpotPin("광안리 바다", "beach", "광안리해수욕장");
        stubSearch("민락수변공원");

        SpotPinResolver.PinResolution result = resolver.resolve(intent("광안리", List.of(resolved, SpotPin.of("민락수변공원")), List.of()));

        assertThat(result.intent().includeSpots().get(0)).isSameAs(resolved);
        verify(spotLookupPort, never()).findActiveByTitleNear(eq("광안리 바다"), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("resolveOrDefault는 되물을 수 없을 때(생성 직전) 후보가 여럿이면 추천 1순위로 확정한다")
    void resolveOrDefault_picksFirstCandidate() {
        stubSearch("해수욕장", BEACH, SONGJEONG);

        CourseIntent resolved = resolver.resolveOrDefault(intent("광안리", List.of(SpotPin.of("해수욕장")), List.of()));

        assertThat(resolved.includeSpots()).containsExactly(new SpotPin("해수욕장", "beach", "광안리해수욕장"));
    }

    @Test
    @DisplayName("지정이 없거나 전부 해결됐거나 검색 중심이 없으면 조회 없이 그대로 돌려준다")
    void resolve_noop_whenNothingToResolve() {
        CourseIntent noPins = intent("광안리", List.of(), List.of());
        CourseIntent allResolved = intent("광안리", List.of(new SpotPin("a", "c1", "A")), List.of());
        CourseIntent noCenter = intent(null, List.of(SpotPin.of("광안리 바다")), List.of());

        assertThat(resolver.resolve(noPins).intent()).isSameAs(noPins);
        assertThat(resolver.resolve(allResolved).intent()).isSameAs(allResolved);
        assertThat(resolver.resolve(noCenter).intent()).isSameAs(noCenter);
        verifyNoInteractions(spotLookupPort);
        verify(spotLookupPort, never()).findActiveByTitleNear(anyString(), anyDouble(), anyDouble(), anyInt());
    }
}
