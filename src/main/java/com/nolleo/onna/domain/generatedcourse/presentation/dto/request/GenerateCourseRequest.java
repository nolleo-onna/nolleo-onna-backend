package com.nolleo.onna.domain.generatedcourse.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// TODO: 현재 signgu·duration·companion·budget 모두 자유 입력을 허용하고 있으나,
//       각 필드는 서버에서 지원하는 고정 값 목록이 존재함.
//       추후 각 필드에 허용 값 기반 검증(@Pattern 또는 커스텀 validator)을 적용하여
//       지원하지 않는 값이 입력될 경우 400 에러를 반환하도록 개선 예정.
@Schema(description = "코스 생성 요청")
public record GenerateCourseRequest(

        @Schema(description = "지역명 (예: 광안리, 해운대, 서면)", example = "광안리")
        @NotBlank(message = "지역명은 필수입니다.")
        String signgu,

        @Schema(description = "예산 (원 단위) — 저장만, 가격 데이터 확보 후 필터 적용 예정", example = "50000")
        Integer budget,

        @Schema(description = "여행 시간", example = "HALF_DAY", allowableValues = {"HALF_DAY", "FULL_DAY"})
        @NotBlank(message = "여행 시간은 필수입니다.")
        String duration,

        @Schema(description = "동행 유형", example = "COUPLE", allowableValues = {"COUPLE", "FRIENDS", "FAMILY", "SOLO"})
        @NotNull(message = "동행 유형은 필수입니다.")
        String companion
) {}
