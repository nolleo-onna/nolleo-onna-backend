package com.nolleo.onna.domain.post.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.post.application.dto.CreatePostCommand;
import com.nolleo.onna.domain.post.application.dto.PostDetailResult;
import com.nolleo.onna.domain.post.application.dto.PostPopularResult;
import com.nolleo.onna.domain.post.application.dto.PostSummaryResult;
import com.nolleo.onna.domain.post.application.dto.UpdatePostCommand;
import com.nolleo.onna.domain.post.application.service.PostCommandService;
import com.nolleo.onna.domain.post.application.service.PostLikeService;
import com.nolleo.onna.domain.post.application.service.PostQueryService;
import com.nolleo.onna.domain.post.application.service.PostReportService;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.domain.repository.PostSearchCondition;
import com.nolleo.onna.domain.post.presentation.dto.request.CreatePostRequest;
import com.nolleo.onna.domain.post.presentation.dto.request.ReportPostRequest;
import com.nolleo.onna.domain.post.presentation.dto.request.UpdatePostRequest;
import com.nolleo.onna.domain.post.presentation.dto.response.PostDetailResponse;
import com.nolleo.onna.domain.post.presentation.dto.response.PostLikeToggleResponse;
import com.nolleo.onna.domain.post.presentation.dto.response.PostPopularResponse;
import com.nolleo.onna.domain.post.presentation.dto.response.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "Post", description = "한끝 게시판 API")
public class PostController {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostLikeService postLikeService;
    private final PostReportService postReportService;

    @PostMapping
    @Operation(summary = "게시글 작성")
    public ResponseEntity<ApiResponseDto<PostDetailResponse>> createPost(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody @Valid CreatePostRequest request
    ) {
        CreatePostCommand command = new CreatePostCommand(
                request.title(), request.content(), request.categoryTags(), request.districtTag(), request.imageUrls());
        PostDetailResult result = postCommandService.createPost(principal.userId(), command);
        return ApiResponseDto.success(201, "게시글 작성 성공", toDetailResponse(result));
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회")
    public ResponseEntity<ApiResponseDto<Page<PostSummaryResponse>>> getPosts(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) PostCategoryTag category,
            @RequestParam(required = false) PostDistrictTag district,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int clampedSize = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(page, clampedSize, Sort.by(Sort.Direction.DESC, "createAudit.createdAt"));
        PostSearchCondition condition = new PostSearchCondition(category, district);
        Long userId = principal != null ? principal.userId() : null;
        Page<PostSummaryResponse> response = postQueryService.getPosts(condition, pageable, userId)
                .map(this::toSummaryResponse);
        return ApiResponseDto.success(200, "게시글 목록 조회 성공", response);
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 게시글 조회")
    public ResponseEntity<ApiResponseDto<List<PostPopularResponse>>> getPopularPosts(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long userId = principal != null ? principal.userId() : null;
        List<PostPopularResponse> response = postQueryService.getPopularPosts(userId).stream()
                .map(r -> new PostPopularResponse(r.postId(), r.title(), r.thumbnail(), r.likeCount(), r.isLiked()))
                .toList();
        return ApiResponseDto.success(200, "인기 게시글 조회 성공", response);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 단건 조회")
    public ResponseEntity<ApiResponseDto<PostDetailResponse>> getPost(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long postId
    ) {
        Long userId = principal != null ? principal.userId() : null;
        PostDetailResult result = postQueryService.getPost(postId, userId);
        return ApiResponseDto.success(200, "게시글 조회 성공", toDetailResponse(result));
    }

    @PatchMapping("/{postId}")
    @Operation(summary = "게시글 수정")
    public ResponseEntity<ApiResponseDto<PostDetailResponse>> updatePost(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long postId,
            @RequestBody @Valid UpdatePostRequest request
    ) {
        UpdatePostCommand command = new UpdatePostCommand(
                request.title(), request.content(), request.categoryTags(), request.districtTag(), request.imageUrls());
        PostDetailResult result = postCommandService.updatePost(principal.userId(), postId, command);
        return ApiResponseDto.success(200, "게시글 수정 성공", toDetailResponse(result));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제")
    public ResponseEntity<ApiResponseDto<Void>> deletePost(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long postId
    ) {
        postCommandService.deletePost(principal.userId(), postId);
        return ApiResponseDto.success(200, "게시글 삭제 성공", null);
    }

    @PostMapping("/{postId}/likes/toggle")
    @Operation(summary = "좋아요 토글")
    public ResponseEntity<ApiResponseDto<PostLikeToggleResponse>> toggleLike(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long postId
    ) {
        PostLikeToggleResponse response = postLikeService.toggleLike(principal.userId(), postId);
        return ApiResponseDto.success(200, "좋아요 토글 성공", response);
    }

    @PostMapping("/{postId}/reports")
    @Operation(summary = "게시글 신고")
    public ResponseEntity<ApiResponseDto<Void>> reportPost(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long postId,
            @RequestBody @Valid ReportPostRequest request
    ) {
        postReportService.report(principal.userId(), postId, request);
        return ApiResponseDto.success(200, "신고 접수 성공", null);
    }

    private PostDetailResponse toDetailResponse(PostDetailResult result) {
        return new PostDetailResponse(
                result.post().getId(),
                result.post().getTitle(),
                result.post().getContent(),
                new PostDetailResponse.AuthorInfo(result.authorNickname(), result.authorProfileImageUrl()),
                result.post().getCategoryTags(),
                result.post().getDistrictTag(),
                result.post().getImageUrls(),
                result.post().getLikeCount(),
                result.isLiked(),
                result.post().getViewCount(),
                result.post().getCommentCount(),
                result.post().getCreatedAt(),
                result.post().getUpdatedAt()
        );
    }

    private PostSummaryResponse toSummaryResponse(PostSummaryResult result) {
        return new PostSummaryResponse(
                result.post().getId(),
                result.post().getTitle(),
                new PostSummaryResponse.AuthorInfo(result.authorNickname(), result.authorProfileImageUrl()),
                result.post().getCategoryTags(),
                result.post().getDistrictTag(),
                result.post().getImageUrls() != null && !result.post().getImageUrls().isEmpty(),
                result.post().getLikeCount(),
                result.isLiked(),
                result.post().getViewCount(),
                result.post().getCommentCount(),
                result.post().getCreatedAt()
        );
    }
}
