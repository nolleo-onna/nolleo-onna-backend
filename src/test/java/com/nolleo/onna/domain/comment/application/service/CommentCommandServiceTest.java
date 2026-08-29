package com.nolleo.onna.domain.comment.application.service;

import com.nolleo.onna.common.exception.BusinessException;
import com.nolleo.onna.domain.comment.domain.exception.CommentErrorCode;
import com.nolleo.onna.domain.comment.domain.model.Comment;
import com.nolleo.onna.domain.comment.domain.repository.CommentRepository;
import com.nolleo.onna.domain.comment.presentation.dto.request.CreateCommentRequest;
import com.nolleo.onna.domain.comment.presentation.dto.response.CommentResponse;
import com.nolleo.onna.domain.post.domain.exception.PostErrorCode;
import com.nolleo.onna.domain.post.domain.model.Post;
import com.nolleo.onna.domain.post.domain.model.vo.PostCategoryTag;
import com.nolleo.onna.domain.post.domain.model.vo.PostDistrictTag;
import com.nolleo.onna.domain.post.domain.repository.PostRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentCommandServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserJpaRepository userJpaRepository;

    @InjectMocks CommentCommandService commentCommandService;

    private Post post;
    private Comment topLevelComment;
    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        post = Post.restore(
                1L, 2L, "제목", "내용",
                List.of(), List.of(PostCategoryTag.CAFE),
                PostDistrictTag.HAEUNDAE_GU,
                0, 0, 0, OffsetDateTime.now(), null
        );

        topLevelComment = Comment.restore(
                10L, 1L, 1L, null, "최상위 댓글", false,
                OffsetDateTime.now(), null
        );

        mockUser = mock(UserEntity.class);
        given(mockUser.getNickname()).willReturn("테스터");
        given(mockUser.getProfileImageUrl()).willReturn(null);
    }

    @Test
    @DisplayName("최상위 댓글을 정상적으로 작성한다")
    void createComment_topLevel_success() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(1L, null, "댓글 내용");
        Comment saved = Comment.restore(
                20L, 1L, 1L, null, "댓글 내용", false, OffsetDateTime.now(), null
        );

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        CommentResponse response = commentCommandService.createComment(1L, request);

        // then
        assertThat(response.content()).isEqualTo("댓글 내용");
        assertThat(response.parentCommentId()).isNull();
        verify(postRepository, times(1)).incrementCommentCount(1L);
    }

    @Test
    @DisplayName("대댓글을 정상적으로 작성한다")
    void createComment_reply_success() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(1L, 10L, "대댓글 내용");
        Comment saved = Comment.restore(
                21L, 1L, 1L, 10L, "대댓글 내용", false, OffsetDateTime.now(), null
        );

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findById(10L)).willReturn(Optional.of(topLevelComment));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);
        given(userJpaRepository.findById(1L)).willReturn(Optional.of(mockUser));

        // when
        CommentResponse response = commentCommandService.createComment(1L, request);

        // then
        assertThat(response.content()).isEqualTo("대댓글 내용");
        assertThat(response.parentCommentId()).isEqualTo(10L);
        verify(postRepository, times(1)).incrementCommentCount(1L);
    }

    @Test
    @DisplayName("대댓글에 또 대댓글 작성 시 INVALID_PARENT_COMMENT 예외를 던진다")
    void createComment_throwsException_whenReplyToReply() {
        // given — parentCommentId가 있는 댓글(대댓글)에 또 대댓글 시도
        Comment replyComment = Comment.restore(
                21L, 1L, 1L, 10L, "대댓글", false, OffsetDateTime.now(), null
        );
        CreateCommentRequest request = new CreateCommentRequest(1L, 21L, "대대댓글 시도");

        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(commentRepository.findById(21L)).willReturn(Optional.of(replyComment));

        // when & then
        assertThatThrownBy(() -> commentCommandService.createComment(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.INVALID_PARENT_COMMENT));

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 POST_NOT_FOUND 예외를 던진다")
    void createComment_throwsException_whenPostNotFound() {
        // given
        CreateCommentRequest request = new CreateCommentRequest(999L, null, "댓글");
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentCommandService.createComment(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(PostErrorCode.POST_NOT_FOUND));
    }

    @Test
    @DisplayName("본인 댓글을 soft delete 처리하고 게시글 댓글 수를 감소시킨다")
    void deleteComment_success() {
        // given
        Comment comment = Comment.restore(
                10L, 1L, 1L, null, "댓글", false, OffsetDateTime.now(), null
        );
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        // when
        commentCommandService.deleteComment(1L, 10L);

        // then
        verify(commentRepository, times(1)).update(any(Comment.class));
        verify(postRepository, times(1)).decrementCommentCount(1L);
    }

    @Test
    @DisplayName("삭제된 댓글의 content가 '삭제된 댓글입니다.'로 변경된다")
    void deleteComment_contentChangedToDeletedMessage() {
        // given
        Comment comment = Comment.restore(
                10L, 1L, 1L, null, "원본 댓글", false, OffsetDateTime.now(), null
        );
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        // when
        commentCommandService.deleteComment(1L, 10L);

        // then
        assertThat(comment.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 댓글 삭제 시 COMMENT_NOT_FOUND 예외를 던진다")
    void deleteComment_throwsException_whenCommentNotFound() {
        // given
        given(commentRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentCommandService.deleteComment(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("다른 사람의 댓글 삭제 시 COMMENT_ACCESS_DENIED 예외를 던진다")
    void deleteComment_throwsException_whenNotOwner() {
        // given
        Comment comment = Comment.restore(
                10L, 1L, 1L, null, "댓글", false, OffsetDateTime.now(), null
        );
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        // when & then — userId=2가 시도
        assertThatThrownBy(() -> commentCommandService.deleteComment(2L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(CommentErrorCode.COMMENT_ACCESS_DENIED));

        verify(commentRepository, never()).update(any());
        verify(postRepository, never()).decrementCommentCount(any());
    }
}
