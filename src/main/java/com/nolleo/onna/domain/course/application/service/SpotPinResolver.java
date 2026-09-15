package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.SpotCandidate;
import com.nolleo.onna.domain.course.application.port.SpotLookupPort;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 사용자가 이름으로 지정한 장소(SpotPin)를 실제 스팟과 맞춘다 — 판단은 DB가 한다.
 *
 * 검색어는 SpotNameVariants 순서(원문 → 동의어 → 접미어 제거 → AI 힌트)로 시도하고, 결과가 나온 첫 검색어의 결과에
 * TitleMatch 규칙(정확 일치 또는 단일 결과만 확정)을 적용한다. 부분 일치가 여럿이면 못 찾은 것으로 두고
 * 안내 문구에서 "정확한 이름을 알려달라"고 한다 — 되묻기로 대화를 복잡하게 만들지 않는다.
 *
 * 어느 검색어로 찾았든 pin의 name(원문)은 그대로 두고 contentId·matchedTitle만 채우므로,
 * 확인 문구에서 "광안리해수욕장(광안리 바다)"처럼 무엇을 무엇으로 이해했는지 사용자가 확인할 수 있다.
 * 이미 해결된 pin은 다시 조회하지 않고, 못 찾은 pin은 미해결 그대로 둔다 — 사용자가 이름을 고쳐 말하면 다음 턴에 다시 시도한다.
 * 검색 중심(기준점 좌표 또는 지역 중심)이 없으면 아무것도 하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SpotPinResolver {

    private final SpotLookupPort spotLookupPort;

    public CourseIntent resolve(CourseIntent intent) {
        if (!intent.hasUnresolvedPins()) return intent;

        Optional<GeoPoint> center = intent.center();
        if (center.isEmpty()) return intent;
        double lat = center.get().latitude();
        double lon = center.get().longitude();

        return intent.withPins(
                resolveAll(intent.includeSpots(), lat, lon),
                resolveAll(intent.excludeSpots(), lat, lon));
    }

    private List<SpotPin> resolveAll(List<SpotPin> pins, double lat, double lon) {
        return pins.stream()
                .map(pin -> pin.isResolved() ? pin : resolveOne(pin, lat, lon))
                .toList();
    }

    /** 결과가 나오는 첫 검색어의 결과로 판단한다 */
    private SpotPin resolveOne(SpotPin pin, double lat, double lon) {
        for (String keyword : SpotNameVariants.of(pin)) {
            List<SpotCandidate> matches = spotLookupPort
                    .findActiveByTitleNear(keyword, lat, lon, TitleMatch.LOOKUP_LIMIT).stream()
                    .filter(SpotCandidate::hasCoordinate)
                    .toList();
            if (matches.isEmpty()) continue;
            return TitleMatch.decide(pin.name(), matches, SpotCandidate::title)
                    .map(spot -> pin.resolvedTo(spot.contentId(), spot.title()))
                    .orElse(pin);
        }
        return pin;
    }
}
