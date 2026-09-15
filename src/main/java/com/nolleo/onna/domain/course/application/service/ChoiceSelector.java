package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.domain.course.application.dto.PendingChoice;
import com.nolleo.onna.domain.course.domain.model.vo.CourseAnchor;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.GeoPoint;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 지난 턴에 물어둔 후보 선택(PendingChoice)을 이번 메시지에서 읽어 intent에 확정한다 — AI 호출 없음.
 *
 * 선택 읽는 순서:
 *   1. "기준점 1", "넣을 곳 2"처럼 항목 이름 + 번호
 *   2. 물어본 항목이 하나뿐이면 메시지의 첫 번호 ("2", "2번")
 *   3. 후보 제목이 메시지에 포함되어 있으면 그 후보 (여럿이면 가장 긴 제목)
 * 아무것도 못 읽으면 추천 1순위로 확정한다 — "추천대로"라고 했든 다른 조건만 말했든, 되묻기가 생성을 막지 않게 한다.
 */
@Service
public class ChoiceSelector {

    private static final Pattern ANCHOR_NUMBER = Pattern.compile("기준점\\s*(\\d+)");
    private static final Pattern INCLUDE_NUMBER = Pattern.compile("(?:넣을\\s*곳|포함)\\s*(\\d+)");
    private static final Pattern ANY_NUMBER = Pattern.compile("(\\d+)\\s*번?");

    public CourseIntent apply(CourseIntent intent, List<PendingChoice> pending, String message) {
        CourseIntent result = intent;
        boolean single = pending.size() == 1;
        for (PendingChoice choice : pending) {
            PendingChoice.Candidate picked = select(choice, message, single).orElse(choice.first());
            result = applyChoice(result, choice, picked);
        }
        return result;
    }

    /** 고른 후보를 intent에 반영한다 — 기준점이면 좌표·지역, 꼭 넣을 곳이면 해당 이름의 pin */
    static CourseIntent applyChoice(CourseIntent intent, PendingChoice choice, PendingChoice.Candidate picked) {
        if (choice.kind() == PendingChoice.Kind.ANCHOR) {
            if (intent.anchor() == null || intent.anchor().isResolved()) return intent;
            CourseAnchor resolved = intent.anchor().resolvedTo(
                    CourseAnchor.AnchorSource.valueOf(picked.source()), picked.contentId(), picked.title(),
                    new GeoPoint(picked.latitude(), picked.longitude()), picked.detail());
            return CourseAnchorResolver.settle(intent, resolved);
        }
        List<SpotPin> includes = intent.includeSpots().stream()
                .map(pin -> !pin.isResolved() && pin.name().equals(choice.name())
                        ? pin.resolvedTo(picked.contentId(), picked.title())
                        : pin)
                .toList();
        return intent.withPins(includes, intent.excludeSpots());
    }

    private static Optional<PendingChoice.Candidate> select(PendingChoice choice, String message, boolean single) {
        Pattern labeled = choice.kind() == PendingChoice.Kind.ANCHOR ? ANCHOR_NUMBER : INCLUDE_NUMBER;
        Optional<PendingChoice.Candidate> byLabel = number(labeled, message).flatMap(choice::pick);
        if (byLabel.isPresent()) return byLabel;

        if (single) {
            Optional<PendingChoice.Candidate> byNumber = number(ANY_NUMBER, message).flatMap(choice::pick);
            if (byNumber.isPresent()) return byNumber;
        }

        String compactMessage = TitleMatch.compact(message);
        return choice.candidates().stream()
                .filter(candidate -> compactMessage.contains(TitleMatch.compact(candidate.title())))
                .max(Comparator.comparingInt(candidate -> candidate.title().length()));
    }

    private static Optional<Integer> number(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
