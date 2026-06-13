package com.nolleo.onna.domain.food.domain.model.vo;

/** [값 객체] 금액 + 통화 코드 — 두 필드는 항상 한 쌍으로 의미를 가짐. */
public record Money(Integer price, String currency) {

    public static Money of(Integer price, String currency) {
        return new Money(price, currency != null ? currency : "KRW");
    }
}
