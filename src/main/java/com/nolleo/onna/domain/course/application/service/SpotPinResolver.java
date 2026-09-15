package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.PendingChoice;
import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import com.nolleo.onna.domain.course.domain.service.CourseAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 사용자가 이름으로 지정한 장소(SpotPin)를 실제 스팟과 맞춘다 — 판단은 DB가 한다.
 *
 * 검색어는 SpotNameVariants 순서(원문 → 동의어 → 접미어 제거 → AI 힌트)로 시도하고, 결과가 나온 첫 검색어의 결과로
 * TitleMatch 규칙(정확 일치 → 단일 결과 → 그 외 후보)을 적용한다.
 *   - 꼭 넣을 곳: 확정되면 채우고, 후보가 여럿이면 PendingChoice로 돌려줘 사용자에게 묻는다 (하나만 넣어야 하므로)
 *   - 뺄 곳: 묻지 않고 걸린 후보를 전부 뺀다 — 빼달라는 걸 하나만 빼고 나머지를 넣는 게 더 큰 실수다.
 *     첫 번째는 원래 pin에, 나머지는 제목을 이름으로 하는 pin으로 늘려 담는다
 *
 * 어느 검색어로 찾았든 pin의 name(원문)은 그대로 두고 contentId·matchedTitle만 채우므로,
 * 확인 문구에서 "광안리해수욕장(광안리 바다)"처럼 무엇을 무엇으로 이해했는지 사용자가 확인할 수 있다.
 * 이미 해결된 pin은 다시 조회하지 않고, 못 찾은 pin은 미해결 그대로 둔다 — 안내 문구에서 알려준다.
 * 검색 중심(기준점 좌표 또는 지역 중심)이 없으면 아무것도 하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SpotPinResolver {

    /** 후보로 보여줄 최대 개수 — 그 이상은 골라달라고 하기에도 많다 */
    static final int CANDIDATE_LIMIT = 5;

    private final SpotLookupPort spotLookupPort;

    /** 매칭 결과 — 확정된 것은 intent에 채워져 있고, 골라야 하는 것은 choices에 있다 */
    public record PinResolution(CourseIntent intent, List<PendingChoice> choices) {
    }

    public PinResolution resolve(CourseIntent intent) {
        if (!intent.hasUnresolvedPins()) return new PinResolution(intent, List.of());

        Optional<GeoPoint> center = intent.center();
        if (center.isEmpty()) return new PinResolution(intent, List.of());
        double lat = center.get().latitude();
        double lon = center.get().longitude();

        List<PendingChoice> choices = new ArrayList<>();
        List<SpotPin> includes = new ArrayList<>();
        for (SpotPin pin : intent.includeSpots()) {
            if (pin.isResolved()) { includes.add(pin); continue; }
            TitleMatch.Result<SpotCandidate> result = search(pin, lat, lon);
            if (result.isResolved()) {
                includes.add(pin.resolvedTo(result.resolved().contentId(), result.resolved().title()));
            } else {
                includes.add(pin);
                if (result.isAmbiguous()) choices.add(toChoice(pin, result.candidates(), lat, lon));
            }
        }

        List<SpotPin> excludes = new ArrayList<>();
        for (SpotPin pin : intent.excludeSpots()) {
            if (pin.isResolved()) { excludes.add(pin); continue; }
            TitleMatch.Result<SpotCandidate> result = search(pin, lat, lon);
            List<SpotCandidate> all = result.isResolved() ? List.of(result.resolved()) : result.candidates();
            if (all.isEmpty()) { excludes.add(pin); continue; }
            excludes.add(pin.resolvedTo(all.get(0).contentId(), all.get(0).title()));
            for (SpotCandidate extra : all.subList(1, all.size())) {
                excludes.add(new SpotPin(extra.title(), extra.contentId(), extra.title()));
            }
        }

        return new PinResolution(intent.withPins(includes, excludes), choices);
    }

    /** 묻지 않고 바로 확정해야 할 때(생성 직전) — 후보가 여럿이면 추천 1순위를 쓴다 */
    public CourseIntent resolveOrDefault(CourseIntent intent) {
        PinResolution resolution = resolve(intent);
        CourseIntent resolved = resolution.intent();
        for (PendingChoice choice : resolution.choices()) {
            resolved = ChoiceSelector.applyChoice(resolved, choice, choice.first());
        }
        return resolved;
    }

    /** 결과가 나오는 첫 검색어의 결과로 판단한다 */
    private TitleMatch.Result<SpotCandidate> search(SpotPin pin, double lat, double lon) {
        for (String keyword : SpotNameVariants.of(pin)) {
            List<SpotCandidate> matches = spotLookupPort.findActiveByTitleNear(keyword, lat, lon, CANDIDATE_LIMIT).stream()
                    .filter(SpotCandidate::hasCoordinate)
                    .toList();
            if (!matches.isEmpty()) return TitleMatch.decide(pin.name(), matches, SpotCandidate::title);
        }
        return TitleMatch.Result.none();
    }

    private static PendingChoice toChoice(SpotPin pin, List<SpotCandidate> candidates, double lat, double lon) {
        List<PendingChoice.Candidate> items = candidates.stream()
                .map(spot -> new PendingChoice.Candidate(
                        spot.contentId(), spot.title(),
                        spot.mapY().doubleValue(), spot.mapX().doubleValue(),
                        distanceLabel(lat, lon, spot), null))
                .toList();
        return new PendingChoice(PendingChoice.Kind.INCLUDE, pin.name(), items);
    }

    /** "0.3km" — 검색 중심에서의 직선거리 */
    private static String distanceLabel(double lat, double lon, SpotCandidate spot) {
        double meters = CourseAssembler.distanceMeters(lat, lon, spot.mapY().doubleValue(), spot.mapX().doubleValue());
        return String.format(Locale.ROOT, "%.1fkm", meters / 1000.0);
    }
}
