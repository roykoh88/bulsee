package com.bulsee.vo;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatContext {

    private String currentIntent;  // 현재 진행 중인 의도
    private String step;           // 현재 단계 (ask_region, ask_date 등)
    private String region;         // 수집된 지역
    private String date;           // 수집된 날짜 또는 연도

    /**
     * 컨텍스트가 비어있는지 확인
     */
    public boolean isEmpty() {
        return currentIntent == null || currentIntent.isEmpty();
    }

    /**
     * 특정 의도인지 확인
     */
    public boolean isIntent(String intent) {
        return intent.equals(currentIntent);
    }
}