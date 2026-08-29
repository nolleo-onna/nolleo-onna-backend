package com.nolleo.onna.domain.comment.presentation.controller;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.comment.application.service.CommentCommandService;
import com.nolleo.onna.domain.comment.application.service.CommentQueryService;
import com.nolleo.onna.domain.comment.domain.exception.CommentErrorCode;
import com.nolleo.onna.domain.comment.presentation.dto.response.CommentResponse;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@WithMockUser
class CommentControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean CommentCommandService commentCommandService;
    @MockBean CommentQueryService commentQueryService;
    @MockBean JwtProvider jwtProvider;

    private Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private CommentResponse sampleComment(Long id, Long parentId) {
        return new CommentResponse(
                id,
                new CommentResponse.AuthorInfo("테스터", null),
                "댓글 내용",
                false,
                parentId,
                List.of(),
                OffsetDateTime.now(),
                null
        );
    }

    @Test
    @DisplayName("POST /api/v1/comments - 최상위 댓글 작성 시 201을 반환한다")
    void createComment_returns201_whenTopLevel() throws Exception {
        // given
        given(commentCommandService.createComment(anyLong(), any())).willReturn(sampleComment(1L, null));

        // when & then
        mockMvc.perform(post("/api/v1/comments")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 1,
                                  "content": "댓글 내용"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.content").value("댓글 내용"))
                .andExpect(jsonPath("$.data.parentCommentId").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/comments - 대댓글 작성 시 201을 반환한다")
    void createComment_returns201_whenReply() throws Exception {
        // given
        given(commentCommandService.createComment(anyLong(), any())).willReturn(sampleComment(2L, 1L));

        // when & then
        mockMvc.perform(post("/api/v1/comments")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 1,
                                  "parentCommentId": 1,
                                  "content": "대댓글 내용"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.parentCommentId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/comments - content가 없으면 400을 반환한다")
    void createComment_returns400_whenContentMissing() throws Exception {
        mockMvc.perform(post("/api/v1/comments")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/comments - 존재하지 않는 게시글에 댓글 작성 시 404를 반환한다")
    void createComment_returns404_whenPostNotFound() throws Exception {
        // given
        given(commentCommandService.createComment(anyLong(), any()))
                .willThrow(new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/comments")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 999,
                                  "content": "댓글"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PT001"));
    }

    @Test
    @DisplayName("POST /api/v1/comments - 대댓글에 또 대댓글 시도 시 400을 반환한다")
    void createComment_returns400_whenReplyToReply() throws Exception {
        // given
        given(commentCommandService.createComment(anyLong(), any()))
                .willThrow(new BusinessException(CommentErrorCode.INVALID_PARENT_COMMENT));

        // when & then
        mockMvc.perform(post("/api/v1/comments")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postId": 1,
                                  "parentCommentId": 99,
                                  "content": "대대댓글"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CM003"));
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{commentId} - 본인 댓글 삭제 시 200을 반환한다")
    void deleteComment_returns200() throws Exception {
        // given
        willDoNothing().given(commentCommandService).deleteComment(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/comments/1")
                        .with(authentication(authAs(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("댓글 삭제 성공"));
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{commentId} - 다른 사람 댓글 삭제 시 403을 반환한다")
    void deleteComment_returns403_whenNotOwner() throws Exception {
        // given
        willThrow(new BusinessException(CommentErrorCode.COMMENT_ACCESS_DENIED))
                .given(commentCommandService).deleteComment(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/comments/1")
                        .with(authentication(authAs(2L)))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("CM002"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId}/comments - 비로그인으로 댓글 목록 조회 시 200을 반환한다")
    void getComments_returns200_withoutAuth() throws Exception {
        // given
        given(commentQueryService.getComments(anyLong(), any())).willReturn(Page.empty());

        // when & then
        mockMvc.perform(get("/api/v1/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("댓글 목록 조회 성공"));
    }
}
