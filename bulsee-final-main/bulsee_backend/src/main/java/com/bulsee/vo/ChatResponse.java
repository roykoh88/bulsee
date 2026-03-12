package com.bulsee.vo;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;              // 챗봇 응답 메시지
    private String type;                 // "text", "prediction", "error"
    private Object data;                 // 예측 결과 등 추가 데이터
    private List<QuickReply> quickReplies; // 빠른 답변 버튼 (선택)
}