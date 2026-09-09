package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.application.port.UserLookupPort;
import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.post.application.dto.PostDetailResult;
import com.nolleo.onna.domain.post.application.dto.PostPopularResult;
import com.nolleo.onna.domain.post.application.dto.PostSummaryResult;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.domain.repository.PostLikeRepository;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.domain.repository.PostSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostQueryServiceTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock UserLookupPort userLookupPort;

    @InjectMocks PostQueryService postQueryService;

    private Post post;
    private UserLookupPort.UserProfile mockProfile;

    @BeforeEach
    void setUp() {
        post = Post.restore(
                1L, 1L, "제목", "내용",
                List.of("https://s3.example.com/img1.jpg"),
                List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                5, 2, 10,
                OffsetDateTime.now(), null
        );

        mockProfile = new UserLookupPort.UserProfile("테스터", "https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("게시글 단건 조회 시 조회수가 증가하고 상세 정보를 반환한다")
    void getPost_success_andIncrementsViewCount() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(false);
        given(userLookupPort.findById(1L)).willReturn(Optional.of(mockProfile));

        // when
        PostDetailResult result = postQueryService.getPost(1L, 1L);

        // then
        assertThat(result.post().getId()).isEqualTo(1L);
        assertThat(result.post().getTitle()).isEqualTo("제목");
        assertThat(result.isLiked()).isFalse();
        assertThat(result.post().getImageUrls()).hasSize(1);
        verify(postRepository, times(1)).incrementViewCount(1L);
    }

    @Test
    @DisplayName("좋아요한 게시글 조회 시 isLiked: true를 반환한다")
    void getPost_returnsIsLikedTrue_whenUserLiked() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.existsByPostIdAndUserId(1L, 1L)).willReturn(true);
        given(userLookupPort.findById(1L)).willReturn(Optional.of(mockProfile));

        // when
        PostDetailResult result = postQueryService.getPost(1L, 1L);

        // then
        assertThat(result.isLiked()).isTrue();
    }

    @Test
    @DisplayName("비로그인 사용자 조회 시 isLiked: false를 반환한다")
    void getPost_returnsIsLikedFalse_whenUserIsNull() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(userLookupPort.findById(1L)).willReturn(Optional.of(mockProfile));

        // when
        PostDetailResult result = postQueryService.getPost(1L, null);

        // then
        assertThat(result.isLiked()).isFalse();
        verify(postLikeRepository, never()).existsByPostIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 시 POST_NOT_FOUND 예외를 던진다")
    void getPost_throwsException_whenPostNotFound() {
        // given
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postQueryService.getPost(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_NOT_FOUND));
    }

    @Test
    @DisplayName("게시글 목록 조회 시 이미지가 있는 게시글은 hasImage: true를 반환한다")
    void getPosts_hasImageTrue_whenImagesExist() {
        // given
        Page<Post> postPage = new PageImpl<>(List.of(post));
        given(postRepository.findAll(any(PostSearchCondition.class), any())).willReturn(postPage);
        given(postLikeRepository.findLikedPostIds(anyLong(), anyList())).willReturn(Set.of());
        given(userLookupPort.findById(1L)).willReturn(Optional.of(mockProfile));

        // when
        Page<PostSummaryResult> result = postQueryService.getPosts(
                new PostSearchCondition(null, null),
                PageRequest.of(0, 10),
                1L
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        PostSummaryResult summary = result.getContent().get(0);
        assertThat(summary.post().getImageUrls()).isNotEmpty();
    }

    @Test
    @DisplayName("게시글 목록 조회 시 이미지가 없는 게시글은 imageUrls가 비어있다")
    void getPosts_hasImageFalse_whenNoImages() {
        // given
        Post noImagePost = Post.restore(
                2L, 1L, "이미지없는 게시글", "내용",
                List.of(), List.of(PostCategoryTag.FESTIVAL), null,
                0, 0, 0, OffsetDateTime.now(), null
        );
        Page<Post> postPage = new PageImpl<>(List.of(noImagePost));
        given(postRepository.findAll(any(PostSearchCondition.class), any())).willReturn(postPage);
        given(postLikeRepository.findLikedPostIds(anyLong(), anyList())).willReturn(Set.of());
        given(userLookupPort.findById(1L)).willReturn(Optional.of(mockProfile));

        // when
        Page<PostSummaryResult> result = postQueryService.getPosts(
                new PostSearchCondition(null, null),
                PageRequest.of(0, 10),
                1L
        );

        // then
        assertThat(result.getContent().get(0).post().getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("비로그인 목록 조회 시 모든 게시글의 isLiked가 false다")
    void getPosts_allIsLikedFalse_whenUserIsNull() {
        // given
        Page<Post> postPage = new PageImpl<>(List.of(post));
        given(postRepository.findAll(any(PostSearchCondition.class), any())).willReturn(postPage);
        given(userLookupPort.findById(1L)).willReturn(Optional.of(mockProfile));

        // when
        Page<PostSummaryResult> result = postQueryService.getPosts(
                new PostSearchCondition(null, null),
                PageRequest.of(0, 10),
                null
        );

        // then
        assertThat(result.getContent().get(0).isLiked()).isFalse();
        verify(postLikeRepository, never()).findLikedPostIds(anyLong(), anyList());
    }

    @Test
    @DisplayName("인기 게시글 조회 시 최대 5개를 반환하고 대표 이미지를 포함한다")
    void getPopularPosts_returnsUpTo5_withThumbnail() {
        // given
        given(postRepository.findPopular(5)).willReturn(List.of(post));
        given(postLikeRepository.findLikedPostIds(anyLong(), anyList())).willReturn(Set.of(1L));

        // when
        List<PostPopularResult> result = postQueryService.getPopularPosts(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).thumbnail()).isEqualTo("https://s3.example.com/img1.jpg");
        assertThat(result.get(0).isLiked()).isTrue();
    }

    @Test
    @DisplayName("인기 게시글 비로그인 조회 시 isLiked가 모두 false다")
    void getPopularPosts_allIsLikedFalse_whenUserIsNull() {
        // given
        given(postRepository.findPopular(5)).willReturn(List.of(post));

        // when
        List<PostPopularResult> result = postQueryService.getPopularPosts(null);

        // then
        assertThat(result.get(0).isLiked()).isFalse();
        verify(postLikeRepository, never()).findLikedPostIds(anyLong(), anyList());
    }
}
