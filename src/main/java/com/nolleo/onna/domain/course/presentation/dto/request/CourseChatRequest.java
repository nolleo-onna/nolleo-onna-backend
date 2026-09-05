package com.nolleo.onna.domain.course.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "자연어 코스 대화 요청")
public record CourseChatRequest(

        @Schema(description = "사용자 자연어 메시지", example = "광안리 근처로 먹거리 여행 가려고, 관광지도 한 군데")
        @NotBlank(message = "메시지는 비어 있을 수 없습니다.")
        String message,

        @Schema(description = "대화 ID — 첫 턴은 null, 되묻기 응답의 conversationId를 이어서 전달", example = "null")
        String conversationId
) {}
