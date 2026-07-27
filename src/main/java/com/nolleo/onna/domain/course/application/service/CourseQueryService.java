package com.nolleo.onna.domain.course.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.course.domain.exception.CourseErrorCode;
import com.nolleo.onna.domain.course.domain.model.Course;
import com.nolleo.onna.domain.course.domain.repository.CourseRepository;
import com.nolleo.onna.domain.course.presentation.dto.response.CourseResponse;
import com.nolleo.onna.domain.course.presentation.dto.response.CourseSummaryResponse;
import com.nolleo.onna.domain.spot.domain.model.Spot;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final SpotsRepository spotsRepository;

    /** pairId로 묶인 코스(들)을 방문 스팟 상세 정보와 함께 조회한다. */
    public List<CourseResponse> getByPairId(UUID pairId) {
        List<Course> courses = courseRepository.findByPairId(pairId);
        if (courses.isEmpty()) {
            throw new BusinessException(CourseErrorCode.COURSE_NOT_FOUND);
        }
        return toResponses(courses);
    }

    /** 사용자가 생성한 코스 목록을 요약 정보(제목/소개/총비용/스팟 이름 목록)로 조회한다. 상세는 pairId로 별도 조회. */
    public List<CourseSummaryResponse> getByUserId(Long userId) {
        List<Course> courses = courseRepository.findByUserId(userId);
        Map<String, Spot> spotByContentId = loadSpots(courses);

        return courses.stream()
                .map(course -> CourseSummaryResponse.of(course, spotByContentId))
                .toList();
    }

    private List<CourseResponse> toResponses(List<Course> courses) {
        Map<String, Spot> spotByContentId = loadSpots(courses);
        return courses.stream()
                .map(course -> CourseResponse.of(course, spotByContentId))
                .toList();
    }

    private Map<String, Spot> loadSpots(List<Course> courses) {
        List<String> spotContentIds = new ArrayList<>();
        courses.forEach(course -> spotContentIds.addAll(course.getSpotContentIds()));
        return spotsRepository.findByIds(spotContentIds).stream()
                .collect(Collectors.toMap(Spot::getContentId, Function.identity()));
    }
}
