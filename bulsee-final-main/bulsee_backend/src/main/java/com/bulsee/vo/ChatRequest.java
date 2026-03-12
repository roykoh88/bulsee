package com.bulsee.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String sessionId;  // 세션 ID (프론트에서 생성)
    private String message;    // 사용자 메시지
}