package com.nolleo.onna;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 풀 컨텍스트 로드 테스트 — PostgreSQL + Redis가 실행 중인 환경에서만 통과.
// 로컬 인프라 없이 실행 시 DataSource/UserJpaRepository 빈 생성 실패로 실패함.
// Docker Compose 또는 개발 환경 구성 후 @Disabled 제거하여 실행할 것.
@Disabled("인프라(DB, Redis) 없이는 실행 불가 — 개발 환경 구성 후 활성화")
@SpringBootTest
class OnnaApplicationTests {

    @Test
    void contextLoads() {
    }

}