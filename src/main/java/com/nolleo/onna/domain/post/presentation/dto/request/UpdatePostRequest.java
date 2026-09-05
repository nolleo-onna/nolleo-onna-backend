package com.nolleo.onna.domain.post.presentation.dto.request;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull @Size(min = 1, max = 10) List<PostCategoryTag> categoryTags,
        PostDistrictTag districtTag,
        @Size(max = 5) List<String> imageUrls
) {}
