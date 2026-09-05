package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.application.dto.ConversationState;

import java.util.Optional;

/**
 * [아웃바운드 포트] 진행 중인 대화의 부분 CourseIntent + 턴 수 보관.
 * 되묻기 후 후속 입력을 기존 intent와 병합하고, 최대 턴 제한을 판단하기 위한 임시 저장소.
 * 구현은 infrastructure/conversation에 위치한다.
 */
public interface ConversationStore {

    void save(String conversationId, ConversationState state);

    Optional<ConversationState> find(String conversationId);

    void delete(String conversationId);
}
