package com.nolleo.onna.domain.generatedcourse.application.dto;

import com.nolleo.onna.domain.generatedcourse.presentation.dto.request.GenerateCourseRequest;

public record GenerateCourseCommand(
        Long userId,
        String signgu,
        Integer budget,
        String duration,
        String companion
) {
    public static GenerateCourseCommand of(Long userId, GenerateCourseRequest request) {
        return new GenerateCourseCommand(
                userId,
                request.signgu(),
                request.budget(),
                request.duration(),
                request.companion()
        );
    }
}
