package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.EventCandidate;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.EventLookupPort;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.DistrictCenter;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * "X 근처"의 X(CourseAnchor)를 데이터에서 찾아 좌표를 채운다 — 행사 → 스팟 순.
 *
 * 위치는 데이터에서만 온다. AI는 이름과 "근처"라는 관계만 뽑고, 어디쯤인지는 추측하지 않는다.
 * TitleMatch 규칙(정확 일치 또는 단일 결과)으로 확정될 때만 좌표를 채운다. "부산국제"처럼 부분 일치가 여럿이면
 * 못 찾은 것으로 두고 정확한 이름을 되묻는다 — 기준점은 하나여야 하고, 후보 선택으로 대화를 복잡하게 만들지 않는다.
 * 찾으면 기준점 좌표에 가장 가까운 지원 지역을 startArea로 채운다 — 사용자가 지역을 직접 말했더라도 덮어쓴다.
 * 검색 중심은 기준점 좌표가 되고, startArea는 제목·조회·편집 등 기존 경로에서 쓰는 지역 표시다.
 * 못 찾으면 미해결 그대로 둔다 — 지역이 없으면 되묻기에서 "찾지 못했다"고 알리고, 지역이 있으면 지역 중심으로 진행한다.
 */
@Service
@RequiredArgsConstructor
public class CourseAnchorResolver {

    /** 스팟 제목 검색은 기준 좌표가 필요하다 — 기준점을 찾는 중이라 아직 없으므로 부산 시청을 쓴다(정확 일치 우선이라 영향은 동점 처리뿐) */
    static final GeoPoint BUSAN_CENTER = new GeoPoint(35.1796, 129.0756);

    /** 이름 뒤에 붙는 일반어 — "부산불꽃축제 행사 축제"처럼 AI가 수식어를 못 뗐을 때 떼고 다시 찾는다 (별도 단어일 때만) */
    private static final List<String> GENERIC_TRAILING_WORDS = List.of("행사", "축제", "페스티벌", "관련", "근처", "주변");

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("M.d");

    private final EventLookupPort eventLookupPort;
    private final SpotLookupPort spotLookupPort;

    /**
     * 행사에서 결과가 나오면 그 결과로 끝낸다 — 확정이든 애매(못 찾음)든 스팟으로 넘어가지 않는다.
     * "부산국제"가 행사 여러 개에 걸렸는데 스팟 "부산국제금융센터"로 조용히 잡히는 일을 막기 위해서다.
     * 행사에 아무것도 없을 때만 스팟을 본다.
     */
    public CourseIntent resolve(CourseIntent intent) {
        if (!intent.hasUnresolvedAnchor()) return intent;
        CourseAnchor anchor = intent.anchor();

        List<EventCandidate> events = searchEvents(anchor.name());
        if (!events.isEmpty()) {
            return TitleMatch.decide(anchor.name(), events, EventCandidate::title)
                    .map(event -> anchor.resolvedTo(
                            CourseAnchor.AnchorSource.EVENT, event.contentId(), event.title(),
                            new GeoPoint(event.mapY().doubleValue(), event.mapX().doubleValue()),
                            period(event.startDate(), event.endDate())))
                    .map(resolved -> settle(intent, resolved))
                    .orElse(intent);
        }

        List<SpotCandidate> spots = searchSpots(anchor.name());
        return TitleMatch.decide(anchor.name(), spots, SpotCandidate::title)
                .map(spot -> anchor.resolvedTo(
                        CourseAnchor.AnchorSource.SPOT, spot.contentId(), spot.title(),
                        new GeoPoint(spot.mapY().doubleValue(), spot.mapX().doubleValue()), null))
                .map(resolved -> settle(intent, resolved))
                .orElse(intent);
    }

    /** 행사 — 끝나지 않은 것만, 좌표 있는 것만. 결과가 나오는 첫 검색어(원문 → 일반어 뗀 이름)의 결과 */
    private List<EventCandidate> searchEvents(String name) {
        for (String keyword : keywords(name)) {
            List<EventCandidate> events = eventLookupPort.findUpcomingByTitle(keyword, TitleMatch.LOOKUP_LIMIT).stream()
                    .filter(EventCandidate::hasCoordinate)
                    .toList();
            if (!events.isEmpty()) return events;
        }
        return List.of();
    }

    /** 스팟 — 같은 검색어 순서 */
    private List<SpotCandidate> searchSpots(String name) {
        for (String keyword : keywords(name)) {
            List<SpotCandidate> spots = spotLookupPort
                    .findActiveByTitleNear(keyword, BUSAN_CENTER.latitude(), BUSAN_CENTER.longitude(), TitleMatch.LOOKUP_LIMIT).stream()
                    .filter(SpotCandidate::hasCoordinate)
                    .toList();
            if (!spots.isEmpty()) return spots;
        }
        return List.of();
    }

    /**
     * 검색어 후보 — 원문, 그리고 뒤에 별도 단어로 붙은 일반어("행사", "축제" 등)를 뗀 이름.
     * "부산불꽃축제"처럼 이름 자체에 붙어 있는 글자는 건드리지 않는다 (공백으로 나뉜 뒷단어만 뗀다).
     */
    static List<String> keywords(String name) {
        List<String> tokens = new ArrayList<>(List.of(name.strip().split("\\s+")));
        while (tokens.size() > 1 && GENERIC_TRAILING_WORDS.contains(tokens.get(tokens.size() - 1))) {
            tokens.remove(tokens.size() - 1);
        }
        String stripped = String.join(" ", tokens);
        return stripped.equals(name.strip()) ? List.of(name.strip()) : List.of(name.strip(), stripped);
    }

    /**
     * 찾은 기준점을 intent에 넣고, startArea를 기준점 좌표에서 가장 가까운 지원 지역으로 맞춘다 —
     * 사용자가 지역을 직접 말했더라도 덮어쓴다. 검색 중심이 기준점 좌표이므로 지역 표시가 실제 스팟 위치와
     * 어긋나면("광안리 코스"인데 중구 스팟만 담기는) 안 되기 때문이다. 폼·챗봇 공통 규칙.
     */
    static CourseIntent settle(CourseIntent intent, CourseAnchor resolved) {
        return intent.withAnchor(resolved)
                .withStartArea(DistrictCenter.nearestTo(resolved.point()).getSigngu());
    }

    /** "10.14~10.16" / "10.14~" / null */
    static String period(LocalDate start, LocalDate end) {
        if (start == null && end == null) return null;
        String from = start != null ? PERIOD.format(start) : "";
        String to = end != null ? PERIOD.format(end) : "";
        return from + "~" + to;
    }
}
