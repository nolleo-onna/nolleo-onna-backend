package com.nolleo.onna.domain.generatedcourse.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.generatedcourse.domain.model.GeneratedCourse;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseInput;
import com.nolleo.onna.domain.generatedcourse.domain.model.vo.CourseType;
import com.nolleo.onna.domain.generatedcourse.domain.repository.GeneratedCourseRepository;
import com.nolleo.onna.domain.generatedcourse.domain.service.CourseComposer;
import com.nolleo.onna.domain.generatedcourse.domain.service.DistrictCenter;
import com.nolleo.onna.domain.generatedcourse.application.dto.GenerateCourseCommand;
import com.nolleo.onna.domain.generatedcourse.presentation.dto.response.CourseResponse;
import com.nolleo.onna.domain.generatedcourse.presentation.dto.response.GenerateCourseResponse;
import com.nolleo.onna.domain.generatedcourse.presentation.dto.response.PairCourseResponse;
import com.nolleo.onna.domain.map.domain.model.MapPlace;
import com.nolleo.onna.domain.map.domain.repository.MapPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CourseGenerationService {

    private final MapPlaceRepository mapPlaceRepository;
    private final GeneratedCourseRepository courseRepository;

    @Transactional
    public GenerateCourseResponse generate(GenerateCourseCommand command) {
        DistrictCenter center = DistrictCenter.of(command.signgu())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.UNSUPPORTED_DISTRICT));

        // PostGIS — 지역 중심에서 가까운 순으로 정렬된 풀을 DB에서 직접 반환
        List<MapPlace> sortedPool = mapPlaceRepository.findNearbyByDistrict(
                center.getDistrict(), center.getLatitude(), center.getLongitude());
        if (sortedPool.isEmpty()) {
            throw new BusinessException(CourseErrorCode.NO_PLACES_IN_DISTRICT);
        }

        UUID pairId = UUID.randomUUID();
        CourseInput courseInput = CourseInput.of(
                command.signgu(), command.budget(), command.duration(), command.companion(), null);

        Set<Long> usedIds = new HashSet<>();

        // ACTIVE / CULTURE / FOOD_TOUR 3개 코스 생성
        List<CourseType> types = List.of(CourseType.ACTIVE, CourseType.CULTURE, CourseType.FOOD_TOUR);
        List<GeneratedCourse> savedCourses = new ArrayList<>();

        for (CourseType type : types) {
            List<MapPlace> items = CourseComposer.compose(type, command.duration(), sortedPool, usedIds);
            if (items.isEmpty()) continue;

            GeneratedCourse course = GeneratedCourse.create(
                    command.userId(), pairId, type,
                    buildTitle(command.signgu(), type, command.duration()),
                    buildDescription(type, command.companion()),
                    courseInput, "default", "form",
                    String.valueOf(command.userId()));

            // PostGIS — 선택된 장소들을 중심에서 가까운 순으로 정렬 + 거리값 일괄 조회 (쿼리 1회)
            List<Long> itemIds = items.stream().map(MapPlace::getId).toList();
            List<MapPlace> route = mapPlaceRepository.findByIdsOrderByDistance(
                    itemIds, center.getLatitude(), center.getLongitude());
            Map<Long, Integer> distanceMap = mapPlaceRepository.findDistancesFromPoint(
                    itemIds, center.getLatitude(), center.getLongitude());

            for (MapPlace place : route) {
                short duration = CourseComposer.durationOf(place.getCategory());
                short distance = distanceMap.getOrDefault(place.getId(), 0).shortValue();
                course.addItem(place.getId(), duration, distance);
            }

            savedCourses.add(courseRepository.save(course));
        }

        return new GenerateCourseResponse(pairId);
    }

    @Transactional(readOnly = true)
    public PairCourseResponse getByPairId(UUID pairId, CourseType type) {
        List<GeneratedCourse> courses = courseRepository.findByPairId(pairId);
        if (courses.isEmpty()) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }

        List<GeneratedCourse> filtered = (type != null)
                ? courses.stream().filter(c -> type == c.getCourseType()).toList()
                : courses;

        // 코스 아이템의 mapPlaceId 목록으로 장소 정보 일괄 조회
        Set<Long> mapPlaceIds = new HashSet<>();
        filtered.forEach(c -> c.getItems().forEach(item -> {
            if (item.getMapPlaceId() != null) mapPlaceIds.add(item.getMapPlaceId());
        }));

        Map<Long, MapPlace> placeById = mapPlaceRepository.findAllByIds(mapPlaceIds);

        List<CourseResponse> courseResponses = filtered.stream()
                .map(c -> CourseResponse.from(c, placeById))
                .toList();

        return new PairCourseResponse(pairId, courseResponses);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private static String buildTitle(String signgu, CourseType type, String duration) {
        String durationLabel = "FULL_DAY".equalsIgnoreCase(duration) ? "하루" : "반나절";
        String typeLabel = switch (type) {
            case ACTIVE    -> "액티브";
            case CULTURE   -> "문화 탐방";
            case FOOD_TOUR -> "맛집 투어";
        };
        return signgu + " " + typeLabel + " " + durationLabel + " 코스";
    }

    private static String buildDescription(CourseType type, String companion) {
        return switch (type) {
            case ACTIVE    -> companion + "과(와) 함께 체험·레저를 즐기는 액티브 코스입니다.";
            case CULTURE   -> companion + "과(와) 함께 자연, 전시, 역사를 둘러보는 문화 탐방 코스입니다.";
            case FOOD_TOUR -> companion + "과(와) 함께 맛집과 카페를 중심으로 즐기는 식도락 코스입니다.";
        };
    }
}
