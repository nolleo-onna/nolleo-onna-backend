package com.nolleo.onna.domain.comment.presentation.controller;

import com.nolleo.onna.common.response.ApiResponseDto;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.domain.comment.application.service.CommentCommandService;
import com.nolleo.onna.domain.comment.application.service.CommentQueryService;
import com.nolleo.onna.domain.comment.presentation.dto.request.CreateCommentRequest;
import com.nolleo.onna.domain.comment.presentation.dto.response.CommentResponse;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 API")
public class CommentController {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;

    @PostMapping("/comments")
    @Operation(summary = "댓글 작성")
    public ResponseEntity<ApiResponseDto<CommentResponse>> createComment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody @Valid CreateCommentRequest request
    ) {
        CommentResponse response = commentCommandService.createComment(principal.userId(), request);
        return ApiResponseDto.success(201, "댓글 작성 성공", response);
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제")
    public ResponseEntity<ApiResponseDto<Void>> deleteComment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long commentId
    ) {
        commentCommandService.deleteComment(principal.userId(), commentId);
        return ApiResponseDto.success(200, "댓글 삭제 성공", null);
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "댓글 목록 조회")
    public ResponseEntity<ApiResponseDto<Page<CommentResponse>>> getComments(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createAudit.createdAt"));
        Page<CommentResponse> response = commentQueryService.getComments(postId, pageable);
        return ApiResponseDto.success(200, "댓글 목록 조회 성공", response);
    }
}
