package com.bulsee.controller;

import com.bulsee.service.ChatbotService;
import com.bulsee.vo.ChatRequest;
import com.bulsee.vo.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(originPatterns = "*")
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * ✅ 메인 엔드포인트: 챗봇 메시지 처리
     * POST /api/chatbot/message
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("📨 챗봇 메시지 요청: sessionId={}, message={}",
                request.getSessionId(), request.getMessage());

        // 입력 검증
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder()
                            .message("세션 ID가 필요합니다.")
                            .type("error")
                            .build()
            );
        }

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder()
                            .message("메시지를 입력해주세요.")
                            .type("error")
                            .build()
            );
        }

        try {
            ChatResponse response = chatbotService.processMessage(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 챗봇 메시지 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    ChatResponse.builder()
                            .message("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                            .type("error")
                            .build()
            );
        }
    }

    /**
     * ✅ (호환용) 예전 프론트가 /chat 을 호출해도 동작하도록 alias 제공
     * POST /api/chatbot/chat  ->  POST /api/chatbot/message 와 동일 처리
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatAlias(@RequestBody ChatRequest request) {
        return sendMessage(request);
    }

    /**
     * ✅ 대화 히스토리 초기화
     * DELETE /api/chatbot/history/{sessionId}
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<String> clearHistory(@PathVariable String sessionId) {
        log.info("🗑️ 히스토리 삭제 요청: sessionId={}", sessionId);

        try {
            chatbotService.clearHistory(sessionId);
            return ResponseEntity.ok("히스토리가 삭제되었습니다.");

        } catch (Exception e) {
            log.error("❌ 히스토리 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("히스토리 삭제 실패");
        }
    }

    /**
     * ✅ 헬스체크 (테스트용)
     * GET /api/chatbot/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chatbot service is running! 🤖");
    }
}
