package com.nolleo.onna.domain.post.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReportPostRequest(@NotBlank String reason) {}
