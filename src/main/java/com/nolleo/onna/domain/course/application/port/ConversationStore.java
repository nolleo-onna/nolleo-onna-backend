package com.nolleo.onna.domain.course.application.port;

import com.nolleo.onna.domain.course.application.dto.ConversationState;

import java.util.Optional;

/**
 * [아웃바운드 포트] 진행 중인 대화의 부분 CourseIntent + 턴 수 보관.
 * 되묻기 후 후속 입력을 기존 intent와 병합하고, 최대 턴 제한을 판단하기 위한 임시 저장소.
 * 구현은 infrastructure/conversation에 위치한다.
 */
public interface ConversationStore {

    /** 저장 실패는 로그만 남기고 삼킨다 — 대화 상태는 되묻기 편의를 위한 임시 데이터라 저장소 장애가 요청을 실패시키면 안 된다 */
    void save(String conversationId, ConversationState state);

    /** 없거나 만료됐거나 저장소에 접근할 수 없으면 empty — 호출자는 새 대화로 취급한다 */
    Optional<ConversationState> find(String conversationId);

    /**
     * 대화 상태를 원자적으로 꺼내면서 지운다 — 코스 생성 직전에 호출한다.
     * 같은 conversationId로 "코스 생성 시작"이 동시에 들어와도 한 요청만 상태를 가져가므로 생성은 한 번만 일어난다.
     * 이미 다른 요청이 가져갔으면 empty.
     */
    Optional<ConversationState> take(String conversationId);

    /** 삭제 실패는 로그만 남기고 삼킨다 — TTL이 있어 결국 정리된다 */
    void delete(String conversationId);
}
