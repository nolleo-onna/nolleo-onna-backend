package com.nolleo.onna.domain.post.domain.repository;

import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;

public record PostSearchCondition(PostCategoryTag categoryTag, PostDistrictTag districtTag) {}
