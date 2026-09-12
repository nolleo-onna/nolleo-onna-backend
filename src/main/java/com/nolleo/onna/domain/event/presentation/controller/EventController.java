package com.nolleo.onna.domain.event.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.domain.event.application.service.EventQueryService;
import com.nolleo.onna.domain.event.application.dto.EventDetailResult;
import com.nolleo.onna.domain.event.presentation.dto.response.EventDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "행사 API")
public class EventController {

    private final EventQueryService eventQueryService;

    @GetMapping
    @Operation(summary = "행사 전체 목록 조회", description = "활성 상태인 행사 전체 목록을 반환합니다.")
    public ResponseEntity<ApiResponseDto<List<EventDetailResponse>>> getEvents() {
        List<EventDetailResponse> data = eventQueryService.getEvents().stream()
                .map(EventDetailResponse::from)
                .toList();
        return ApiResponseDto.success(200, "행사 목록 조회 성공", data);
    }

    @GetMapping("/{contentId}")
    @Operation(summary = "행사 상세 조회", description = "contentId로 행사 상세 정보를 반환합니다.")
    public ResponseEntity<ApiResponseDto<EventDetailResponse>> getEventDetail(
            @PathVariable String contentId) {
        EventDetailResponse data = EventDetailResponse.from(eventQueryService.getEventDetail(contentId));
        return ApiResponseDto.success(200, "행사 상세 조회 성공", data);
    }
}
