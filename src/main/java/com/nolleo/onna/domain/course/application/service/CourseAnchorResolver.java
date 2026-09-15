package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.EventCandidate;
import com.nolleo.onna.domain.course.application.dto.PendingChoice;
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
import java.util.Optional;

/**
 * "X 근처"의 X(CourseAnchor)를 데이터에서 찾아 좌표를 채운다 — 행사 → 스팟 순.
 *
 * 위치는 데이터에서만 온다. AI는 이름과 "근처"라는 관계만 뽑고, 어디쯤인지는 추측하지 않는다.
 * TitleMatch 규칙으로 확정되면 좌표를 채우고, 부분 일치가 여러 개면("부산국제" → 항만컨퍼런스·영화제…) PendingChoice로
 * 돌려줘 사용자에게 묻는다. 기준점은 하나여야 하기 때문이다.
 * 찾으면 기준점 좌표에 가장 가까운 지원 지역을 startArea로 채운다(사용자가 지역을 직접 말했으면 그대로 둔다) —
 * 검색 중심은 기준점 좌표가 되고, startArea는 조회·편집 등 기존 경로와의 호환용이다.
 * 못 찾으면 미해결 그대로 둔다 — 지역이 없으면 되묻기에서 "찾지 못했다"고 알리고, 지역이 있으면 지역 중심으로 진행한다.
 */
@Service
@RequiredArgsConstructor
public class CourseAnchorResolver {

    /** 스팟 제목 검색은 기준 좌표가 필요하다 — 기준점을 찾는 중이라 아직 없으므로 부산 시청을 쓴다(정확 일치 우선이라 영향은 동점 처리뿐) */
    static final GeoPoint BUSAN_CENTER = new GeoPoint(35.1796, 129.0756);

    /** 후보로 보여줄 최대 개수 */
    static final int CANDIDATE_LIMIT = 5;

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("M.d");

    private final EventLookupPort eventLookupPort;
    private final SpotLookupPort spotLookupPort;

    /** 매칭 결과 — 확정됐으면 intent에 채워져 있고, 골라야 하면 choice가 있다 */
    public record AnchorResolution(CourseIntent intent, Optional<PendingChoice> choice) {
        static AnchorResolution settled(CourseIntent intent) { return new AnchorResolution(intent, Optional.empty()); }
    }

    /** 이름 뒤에 붙는 일반어 — "부산불꽃축제 행사 축제"처럼 AI가 수식어를 못 뗐을 때 떼고 다시 찾는다 (별도 단어일 때만) */
    private static final List<String> GENERIC_TRAILING_WORDS = List.of("행사", "축제", "페스티벌", "관련", "근처", "주변");

    public AnchorResolution resolve(CourseIntent intent) {
        if (!intent.hasUnresolvedAnchor()) return AnchorResolution.settled(intent);
        CourseAnchor anchor = intent.anchor();

        // 1. 행사 — 끝나지 않은 것만, 좌표 있는 것만. 결과가 나오는 첫 검색어(원문 → 일반어 뗀 이름)로 판단한다
        List<EventCandidate> events = List.of();
        for (String keyword : keywords(anchor.name())) {
            events = eventLookupPort.findUpcomingByTitle(keyword, CANDIDATE_LIMIT).stream()
                    .filter(EventCandidate::hasCoordinate)
                    .toList();
            if (!events.isEmpty()) break;
        }
        TitleMatch.Result<EventCandidate> eventMatch = TitleMatch.decide(anchor.name(), events, EventCandidate::title);
        if (eventMatch.isResolved()) {
            EventCandidate event = eventMatch.resolved();
            return AnchorResolution.settled(settle(intent, anchor.resolvedTo(
                    CourseAnchor.AnchorSource.EVENT, event.contentId(), event.title(),
                    new GeoPoint(event.mapY().doubleValue(), event.mapX().doubleValue()),
                    period(event.startDate(), event.endDate()))));
        }
        if (eventMatch.isAmbiguous()) {
            return new AnchorResolution(intent, Optional.of(new PendingChoice(PendingChoice.Kind.ANCHOR, anchor.name(),
                    eventMatch.candidates().stream().map(CourseAnchorResolver::toCandidate).toList())));
        }

        // 2. 스팟 — 같은 검색어 순서
        List<SpotCandidate> spots = List.of();
        for (String keyword : keywords(anchor.name())) {
            spots = spotLookupPort
                    .findActiveByTitleNear(keyword, BUSAN_CENTER.latitude(), BUSAN_CENTER.longitude(), CANDIDATE_LIMIT).stream()
                    .filter(SpotCandidate::hasCoordinate)
                    .toList();
            if (!spots.isEmpty()) break;
        }
        TitleMatch.Result<SpotCandidate> spotMatch = TitleMatch.decide(anchor.name(), spots, SpotCandidate::title);
        if (spotMatch.isResolved()) {
            SpotCandidate spot = spotMatch.resolved();
            return AnchorResolution.settled(settle(intent, anchor.resolvedTo(
                    CourseAnchor.AnchorSource.SPOT, spot.contentId(), spot.title(),
                    new GeoPoint(spot.mapY().doubleValue(), spot.mapX().doubleValue()), null)));
        }
        if (spotMatch.isAmbiguous()) {
            return new AnchorResolution(intent, Optional.of(new PendingChoice(PendingChoice.Kind.ANCHOR, anchor.name(),
                    spotMatch.candidates().stream().map(CourseAnchorResolver::toCandidate).toList())));
        }
        return AnchorResolution.settled(intent);
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

    /** 찾은 기준점을 intent에 넣고, 지역이 없으면 좌표에서 가장 가까운 지원 지역을 채운다 */
    static CourseIntent settle(CourseIntent intent, CourseAnchor resolved) {
        CourseIntent withAnchor = intent.withAnchor(resolved);
        if (withAnchor.canGenerate()) return withAnchor;
        return withAnchor.withStartArea(DistrictCenter.nearestTo(resolved.point()).getSigngu());
    }

    private static PendingChoice.Candidate toCandidate(EventCandidate event) {
        return new PendingChoice.Candidate(event.contentId(), event.title(),
                event.mapY().doubleValue(), event.mapX().doubleValue(),
                period(event.startDate(), event.endDate()), CourseAnchor.AnchorSource.EVENT.name());
    }

    private static PendingChoice.Candidate toCandidate(SpotCandidate spot) {
        return new PendingChoice.Candidate(spot.contentId(), spot.title(),
                spot.mapY().doubleValue(), spot.mapX().doubleValue(),
                null, CourseAnchor.AnchorSource.SPOT.name());
    }

    /** "10.14~10.16" / "10.14~" / null */
    static String period(LocalDate start, LocalDate end) {
        if (start == null && end == null) return null;
        String from = start != null ? PERIOD.format(start) : "";
        String to = end != null ? PERIOD.format(end) : "";
        return from + "~" + to;
    }
}
