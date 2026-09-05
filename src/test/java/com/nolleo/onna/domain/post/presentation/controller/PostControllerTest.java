package com.nolleo.onna.domain.post.presentation.controller;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.security.AuthPrincipal;
import com.nolleo.onna.common.security.jwt.JwtProvider;
import com.nolleo.onna.domain.post.application.dto.PostDetailResult;
import com.nolleo.onna.domain.post.application.dto.PostPopularResult;
import com.nolleo.onna.domain.post.application.dto.PostSummaryResult;
import com.nolleo.onna.domain.post.application.service.PostCommandService;
import com.nolleo.onna.domain.post.application.service.PostLikeService;
import com.nolleo.onna.domain.post.application.service.PostQueryService;
import com.nolleo.onna.domain.post.application.service.PostReportService;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.presentation.dto.response.PostLikeToggleResponse;
import com.nolleo.onna.domain.user.domain.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

@WebMvcTest(PostController.class)
@WithMockUser
class PostControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PostCommandService postCommandService;
    @MockBean PostQueryService postQueryService;
    @MockBean PostLikeService postLikeService;
    @MockBean PostReportService postReportService;
    @MockBean JwtProvider jwtProvider;

    private Authentication authAs(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, UserRole.USER);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private PostDetailResult sampleDetailResult() {
        Post post = Post.restore(
                1L, 1L, "제목", "내용",
                List.of("https://s3.example.com/img1.jpg"),
                List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                0, 0, 0,
                OffsetDateTime.now(), null
        );
        return new PostDetailResult(post, "테스터", null, false);
    }

    @Test
    @DisplayName("POST /api/v1/posts - 유효한 요청으로 게시글 작성 시 201을 반환한다")
    void createPost_returns201_whenValidRequest() throws Exception {
        // given
        given(postCommandService.createPost(anyLong(), any())).willReturn(sampleDetailResult());

        // when & then
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제목",
                                  "content": "내용",
                                  "categoryTags": ["CAFE"],
                                  "districtTag": "HAEUNDAE_GU",
                                  "imageUrls": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("게시글 작성 성공"))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    @DisplayName("POST /api/v1/posts - title이 없으면 400을 반환한다")
    void createPost_returns400_whenTitleMissing() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "내용",
                                  "categoryTags": ["CAFE"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 게시글 단건 조회 시 200을 반환한다")
    void getPost_returns200() throws Exception {
        // given
        given(postQueryService.getPost(anyLong(), any())).willReturn(sampleDetailResult());

        // when & then
        mockMvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId} - 존재하지 않는 게시글 조회 시 404를 반환한다")
    void getPost_returns404_whenPostNotFound() throws Exception {
        // given
        given(postQueryService.getPost(anyLong(), any()))
                .willThrow(new BusinessException(PostErrorCode.POST_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errorCode").value("PT001"));
    }

    @Test
    @DisplayName("GET /api/v1/posts - 비로그인으로 목록 조회 시 200을 반환한다")
    void getPosts_returns200_withoutAuth() throws Exception {
        // given
        given(postQueryService.getPosts(any(), any(), isNull())).willReturn(Page.empty());

        // when & then
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/posts/popular - 인기 게시글 조회 시 200을 반환한다")
    void getPopularPosts_returns200() throws Exception {
        // given
        PostPopularResult popular = new PostPopularResult(
                1L, "제목", "https://s3.example.com/img1.jpg", 10, false
        );
        given(postQueryService.getPopularPosts(any())).willReturn(List.of(popular));

        // when & then
        mockMvc.perform(get("/api/v1/posts/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].likeCount").value(10));
    }

    @Test
    @DisplayName("PATCH /api/v1/posts/{postId} - 본인 게시글 수정 시 200을 반환한다")
    void updatePost_returns200() throws Exception {
        // given
        given(postCommandService.updatePost(anyLong(), anyLong(), any())).willReturn(sampleDetailResult());

        // when & then
        mockMvc.perform(patch("/api/v1/posts/1")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정 제목",
                                  "content": "수정 내용",
                                  "categoryTags": ["CAFE"],
                                  "imageUrls": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("PATCH /api/v1/posts/{postId} - 다른 사람 게시글 수정 시 403을 반환한다")
    void updatePost_returns403_whenNotOwner() throws Exception {
        // given
        given(postCommandService.updatePost(anyLong(), anyLong(), any()))
                .willThrow(new BusinessException(PostErrorCode.POST_ACCESS_DENIED));

        // when & then
        mockMvc.perform(patch("/api/v1/posts/1")
                        .with(authentication(authAs(2L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정 제목",
                                  "content": "수정 내용",
                                  "categoryTags": ["CAFE"],
                                  "imageUrls": []
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PT002"));
    }

    @Test
    @DisplayName("DELETE /api/v1/posts/{postId} - 본인 게시글 삭제 시 200을 반환한다")
    void deletePost_returns200() throws Exception {
        // given
        willDoNothing().given(postCommandService).deletePost(anyLong(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/v1/posts/1")
                        .with(authentication(authAs(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("POST /api/v1/posts/{postId}/likes/toggle - 좋아요 토글 시 200을 반환한다")
    void toggleLike_returns200() throws Exception {
        // given
        PostLikeToggleResponse response = new PostLikeToggleResponse(1L, true, 6);
        given(postLikeService.toggleLike(anyLong(), anyLong())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/posts/1/likes/toggle")
                        .with(authentication(authAs(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(6));
    }

    @Test
    @DisplayName("POST /api/v1/posts/{postId}/reports - 게시글 신고 시 200을 반환한다")
    void reportPost_returns200() throws Exception {
        // given
        willDoNothing().given(postReportService).report(anyLong(), anyLong(), any());

        // when & then
        mockMvc.perform(post("/api/v1/posts/1/reports")
                        .with(authentication(authAs(2L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "스팸입니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("신고 접수 성공"));
    }

    @Test
    @DisplayName("POST /api/v1/posts/{postId}/reports - 본인 게시글 신고 시 400을 반환한다")
    void reportPost_returns400_whenReportingOwnPost() throws Exception {
        // given
        willThrow(new BusinessException(PostErrorCode.CANNOT_REPORT_OWN_POST))
                .given(postReportService).report(anyLong(), anyLong(), any());

        // when & then
        mockMvc.perform(post("/api/v1/posts/1/reports")
                        .with(authentication(authAs(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "테스트"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("PT005"));
    }
}
