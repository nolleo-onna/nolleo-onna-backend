package com.nolleo.onna.domain.post.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.common.infrastructure.s3.ImageStoragePort;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
import com.nolleo.onna.domain.post.presentation.dto.request.CreatePostRequest;
import com.nolleo.onna.domain.post.presentation.dto.request.UpdatePostRequest;
import com.nolleo.onna.domain.post.presentation.dto.response.PostDetailResponse;
import com.nolleo.onna.domain.user.domain.entity.UserEntity;
import com.nolleo.onna.domain.user.domain.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostCommandServiceTest {

    @Mock PostRepository postRepository;
    @Mock ImageStoragePort imageStoragePort;
    @Mock UserJpaRepository userJpaRepository;

    @InjectMocks PostCommandService postCommandService;

    private UserEntity mockUser;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        mockUser = mock(UserEntity.class);
        given(mockUser.getNickname()).willReturn("테스터");
        given(mockUser.getProfileImageUrl()).willReturn("https://example.com/profile.jpg");

        savedPost = Post.restore(
                1L, 1L, "제목", "내용",
                List.of("https://s3.example.com/image1.jpg"),
                List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                0, 0, 0,
                OffsetDateTime.now(), null
        );
    }

    @Test
    @DisplayName("유효한 요청으로 게시글을 정상적으로 작성한다")
    void createPost_success() {
        // given
        CreatePostRequest request = new CreatePostRequest(
                "제목", "내용",
                List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                List.of("https://s3.example.com/image1.jpg")
        );

        given(postRepository.save(any(Post.class))).willReturn(savedPost);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        PostDetailResponse response = postCommandService.createPost(1L, request);

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.author().nickname()).isEqualTo("테스터");
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("이미지를 6장 이상 첨부하면 TOO_MANY_IMAGES 예외를 던진다")
    void createPost_throwsException_whenTooManyImages() {
        // given
        List<String> sixImages = List.of("u1", "u2", "u3", "u4", "u5", "u6");
        CreatePostRequest request = new CreatePostRequest(
                "제목", "내용",
                List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                sixImages
        );

        // when & then
        assertThatThrownBy(() -> postCommandService.createPost(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.TOO_MANY_IMAGES));

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인 게시글을 정상적으로 수정한다")
    void updatePost_success() {
        // given
        UpdatePostRequest request = new UpdatePostRequest(
                "수정 제목", "수정 내용",
                List.of(PostCategoryTag.RESTAURANT),
                PostDistrictTag.JUNG_GU,
                List.of("https://s3.example.com/image1.jpg")
        );

        Post updatedPost = Post.restore(
                1L, 1L, "수정 제목", "수정 내용",
                List.of("https://s3.example.com/image1.jpg"),
                List.of(PostCategoryTag.RESTAURANT),
                PostDistrictTag.JUNG_GU,
                0, 0, 0,
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        given(postRepository.findById(1L)).willReturn(Optional.of(savedPost));
        given(postRepository.update(eq(1L), any(Post.class))).willReturn(updatedPost);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        PostDetailResponse response = postCommandService.updatePost(1L, 1L, request);

        // then
        assertThat(response.title()).isEqualTo("수정 제목");
        verify(postRepository, times(1)).update(eq(1L), any(Post.class));
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 시 POST_NOT_FOUND 예외를 던진다")
    void updatePost_throwsException_whenPostNotFound() {
        // given
        UpdatePostRequest request = new UpdatePostRequest(
                "수정 제목", "수정 내용", List.of(), null, List.of()
        );
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postCommandService.updatePost(1L, 999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_NOT_FOUND));
    }

    @Test
    @DisplayName("다른 사람의 게시글 수정 시 POST_ACCESS_DENIED 예외를 던진다")
    void updatePost_throwsException_whenNotOwner() {
        // given
        UpdatePostRequest request = new UpdatePostRequest(
                "수정 제목", "수정 내용", List.of(), null, List.of()
        );
        given(postRepository.findById(1L)).willReturn(Optional.of(savedPost)); // userId=1

        // when & then — userId=2가 시도
        assertThatThrownBy(() -> postCommandService.updatePost(2L, 1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_ACCESS_DENIED));
    }

    @Test
    @DisplayName("게시글 수정 시 삭제된 이미지는 S3에서도 삭제된다")
    void updatePost_deletesRemovedImagesFromS3() {
        // given — 기존 이미지: image1, image2 / 수정 후: image1만
        Post postWithTwoImages = Post.restore(
                1L, 1L, "제목", "내용",
                List.of("https://s3.example.com/image1.jpg", "https://s3.example.com/image2.jpg"),
                List.of(PostCategoryTag.CAFE), PostDistrictTag.HAEUNDAE_GU,
                0, 0, 0, OffsetDateTime.now(), null
        );

        UpdatePostRequest request = new UpdatePostRequest(
                "수정 제목", "수정 내용", List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                List.of("https://s3.example.com/image1.jpg")  // image2 제거
        );

        Post updatedPost = Post.restore(
                1L, 1L, "수정 제목", "수정 내용",
                List.of("https://s3.example.com/image1.jpg"),
                List.of(PostCategoryTag.CAFE), PostDistrictTag.HAEUNDAE_GU,
                0, 0, 0, OffsetDateTime.now(), OffsetDateTime.now()
        );

        given(postRepository.findById(1L)).willReturn(Optional.of(postWithTwoImages));
        given(postRepository.update(eq(1L), any(Post.class))).willReturn(updatedPost);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        postCommandService.updatePost(1L, 1L, request);

        // then — image2만 S3에서 삭제
        verify(imageStoragePort, times(1)).delete("https://s3.example.com/image2.jpg");
        verify(imageStoragePort, never()).delete("https://s3.example.com/image1.jpg");
    }

    @Test
    @DisplayName("본인 게시글을 정상적으로 삭제하고 S3 이미지도 삭제한다")
    void deletePost_success() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(savedPost));
        willDoNothing().given(imageStoragePort).delete(any());
        willDoNothing().given(postRepository).softDelete(1L, "1");

        // when
        postCommandService.deletePost(1L, 1L);

        // then
        verify(imageStoragePort, times(1)).delete("https://s3.example.com/image1.jpg");
        verify(postRepository, times(1)).softDelete(1L, "1");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 시 POST_NOT_FOUND 예외를 던진다")
    void deletePost_throwsException_whenPostNotFound() {
        // given
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postCommandService.deletePost(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_NOT_FOUND));

        verify(postRepository, never()).softDelete(any(), any());
    }

    @Test
    @DisplayName("다른 사람의 게시글 삭제 시 POST_ACCESS_DENIED 예외를 던진다")
    void deletePost_throwsException_whenNotOwner() {
        // given
        given(postRepository.findById(1L)).willReturn(Optional.of(savedPost)); // userId=1

        // when & then — userId=2가 시도
        assertThatThrownBy(() -> postCommandService.deletePost(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_ACCESS_DENIED));
    }
}
