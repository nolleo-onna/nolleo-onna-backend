package com.nolleo.onna.domain.post.application.dto;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;

import java.util.List;

public record UpdatePostCommand(
        String title,
        String content,
        List<PostCategoryTag> categoryTags,
        PostDistrictTag districtTag,
        List<String> imageUrls
) {}
