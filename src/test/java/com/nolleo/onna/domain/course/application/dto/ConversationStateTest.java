package com.nolleo.onna.domain.course.application.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolleo.onna.domain.course.domain.model.vo.CourseIntent;
import com.nolleo.onna.domain.course.domain.model.vo.SpotPin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationStateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("카운터가 없던 시절에 Redis에 저장된 JSON도 turnCount·offTopicStreak 0으로 역직렬화된다")
    void deserializes_legacyJsonWithoutCounters() throws Exception {
        String legacy = """
                {"intent":{"startArea":"광안리","nearbyAllowed":false,"budget":null,"companion":null,
                           "mood":[],"slotHints":{"foodCount":null,"cafeCount":null,"attractionCount":null,"activityCount":null},
                           "clarifiedOnce":true},
                 "awaitingConfirmation":true}
                """;

        ConversationState state = objectMapper.readValue(legacy, ConversationState.class);

        assertThat(state.intent().startArea()).isEqualTo("광안리");
        assertThat(state.awaitingConfirmation()).isTrue();
        assertThat(state.turnCount()).isZero();
        assertThat(state.offTopicStreak()).isZero();
    }

    @Test
    @DisplayName("직렬화 → 역직렬화 왕복에서 카운터가 보존된다")
    void roundTrip_keepsCounters() throws Exception {
        // 장소 지정 목록까지 포함해 정규 생성자(9개 인자)로 역직렬화되는지 함께 확인한다
        CourseIntent intent = new CourseIntent("해운대", true, 50000, "친구", List.of("활기찬"), null, false,
                List.of(new SpotPin("해운대 바다", "c1", "해운대해수욕장")), List.of(SpotPin.of("동백섬")));
        ConversationState original = new ConversationState(intent, false, 4, 2);

        ConversationState restored = objectMapper.readValue(objectMapper.writeValueAsString(original), ConversationState.class);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("afterOffTopicTurn은 intent·확인 대기 여부를 그대로 두고 턴과 연속 횟수만 1씩 올린다")
    void afterOffTopicTurn_incrementsCountersOnly() {
        CourseIntent intent = new CourseIntent("서면", false, null, null, List.of(), null, false);
        ConversationState state = ConversationState.of(intent, true, 3);

        ConversationState next = state.afterOffTopicTurn();

        assertThat(next.intent()).isEqualTo(intent);
        assertThat(next.awaitingConfirmation()).isTrue();
        assertThat(next.turnCount()).isEqualTo(4);
        assertThat(next.offTopicStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("of는 여행 관련 턴을 반영하므로 연속 횟수를 0으로 되돌린다")
    void of_resetsOffTopicStreak() {
        CourseIntent intent = new CourseIntent("서면", false, null, null, List.of(), null, false);

        ConversationState state = ConversationState.of(intent, false, 5);

        assertThat(state.offTopicStreak()).isZero();
        assertThat(state.turnCount()).isEqualTo(5);
    }
}
