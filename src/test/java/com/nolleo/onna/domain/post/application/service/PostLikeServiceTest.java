package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.PostLike;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.domain.repository.PostLikeRepository;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.presentation.dto.response.PostLikeToggleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostRepository postRepository;

    @InjectMocks PostLikeService postLikeService;

    private Post post;

    @BeforeEach
    void setUp() {
        post = Post.restore(
                1L, 2L, "제목", "내용",
                List.of(), List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                3, 0, 0,
                OffsetDateTime.now(), null
        );
    }

    @Test
    @DisplayName("좋아요하지 않은 게시글에 좋아요를 등록하면 isLiked: true를 반환한다")
    void toggleLike_like_whenNotLiked() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.empty());
        given(postLikeRepository.save(any(PostLike.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        PostLikeToggleResponse response = postLikeService.toggleLike(1L, 1L);

        // then
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(4);
        verify(postLikeRepository, times(1)).save(any(PostLike.class));
        verify(postRepository, times(1)).incrementLikeCount(1L);
    }

    @Test
    @DisplayName("이미 좋아요한 게시글에 다시 요청하면 좋아요가 취소되고 isLiked: false를 반환한다")
    void toggleLike_unlike_whenAlreadyLiked() {
        // given
        PostLike existingLike = new PostLike(10L, 1L, 1L, OffsetDateTime.now());
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.of(existingLike));

        // when
        PostLikeToggleResponse response = postLikeService.toggleLike(1L, 1L);

        // then
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(2);
        verify(postLikeRepository, times(1)).delete(existingLike);
        verify(postRepository, times(1)).decrementLikeCount(1L);
    }

    @Test
    @DisplayName("좋아요 취소 시 likeCount가 0 미만으로 내려가지 않는다")
    void toggleLike_likeCount_neverBelowZero() {
        // given — likeCount가 0인 게시글
        Post zeroLikePost = Post.restore(
                1L, 2L, "제목", "내용",
                List.of(), List.of(), null,
                0, 0, 0,
                OffsetDateTime.now(), null
        );
        PostLike existingLike = new PostLike(10L, 1L, 1L, OffsetDateTime.now());

        given(postRepository.findById(1L)).willReturn(Optional.of(zeroLikePost));
        given(postLikeRepository.findByPostIdAndUserId(1L, 1L)).willReturn(Optional.of(existingLike));

        // when
        PostLikeToggleResponse response = postLikeService.toggleLike(1L, 1L);

        // then
        assertThat(response.likeCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 시 POST_NOT_FOUND 예외를 던진다")
    void toggleLike_throwsException_whenPostNotFound() {
        // given
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postLikeService.toggleLike(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_NOT_FOUND));

        verify(postLikeRepository, never()).save(any());
        verify(postLikeRepository, never()).delete(any());
    }
}
