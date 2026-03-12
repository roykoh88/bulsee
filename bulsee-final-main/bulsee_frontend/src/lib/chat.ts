/**
 * 챗봇 백엔드 통신 라이브러리
 * 
 * 역할:
 * 1. Spring Boot API 호출
 * 2. 세션 ID 관리 (localStorage)
 * 3. 요청/응답 타입 정의
 * 
 * 사용법:
 * import { getBotResponse } from "./lib/chat";
 * const response = await getBotResponse("안녕하세요");
 */

// ========== API 설정 ==========
const API_BASE_URL = "http://localhost:8080/api/chatbot";

// ========== 타입 정의 ==========

/**
 * 채팅 컨텍스트 (대화 상태)
 */
interface ChatContext {
  currentIntent?: string;  // 현재 의도
  step?: string;          // 현재 단계
  region?: string;        // 수집된 지역
  date?: string;          // 수집된 날짜/연도
}

/**
 * 빠른 답변 버튼
 */
interface QuickReply {
  label: string;   // 버튼 텍스트
  value: string;   // 클릭 시 전송될 메시지
  action?: string; // 특수 액션
}

/**
 * 채팅 요청 (프론트 → 백엔드)
 */
interface ChatRequest {
  sessionId: string;
  message: string;
  context?: ChatContext | null;
}

/**
 * 채팅 응답 (백엔드 → 프론트)
 */
interface ChatResponse {
  message: string;              // 봇 메시지
  type: string;                 // 메시지 타입
  data?: any;                   // 추가 데이터
  quickReplies?: QuickReply[];  // 빠른 답변 버튼
  context?: ChatContext;        // 업데이트된 컨텍스트
}

// ========== 세션 관리 ==========

/**
 * 세션 ID 가져오기 (없으면 생성)
 * 
 * localStorage에 저장하여 브라우저 새로고침 시에도 유지
 */
function getSessionId(): string {
  let sessionId = localStorage.getItem("chatbot_session_id");
  
  if (!sessionId) {
    // UUID 형식의 세션 ID 생성
    sessionId = `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem("chatbot_session_id", sessionId);
  }
  
  return sessionId;
}

// ========== API 호출 ==========

/**
 * 봇 응답 가져오기
 * 
 * @param message 사용자 메시지
 * @returns 봇 응답
 */
export async function getBotResponse(message: string): Promise<ChatResponse> {
  const sessionId = getSessionId();
  
  const request: ChatRequest = {
    sessionId,
    message,
    context: null
  };
  
  try {
    const response = await fetch(`${API_BASE_URL}/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const data: ChatResponse = await response.json();
    return data;
    
  } catch (error) {
    console.error("API 호출 오류:", error);
    
    // 에러 시 기본 응답 반환
    return {
      message: "서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요.",
      type: "error"
    };
  }
}

/**
 * 채팅 세션 초기화
 * 
 * localStorage와 백엔드 세션 모두 초기화
 */
export async function resetChatSession(): Promise<void> {
  const sessionId = getSessionId();
  
  try {
    // 백엔드 세션 초기화
    await fetch(`${API_BASE_URL}/reset?sessionId=${sessionId}`, {
      method: "POST",
    });
    
    // localStorage 세션 삭제
    localStorage.removeItem("chatbot_session_id");
    
    console.log("✅ 세션 초기화 완료");
    
  } catch (error) {
    console.error("세션 초기화 오류:", error);
  }
}

/**
 * 챗봇 서버 상태 확인
 * 
 * @returns 서버 상태 메시지
 */
export async function checkChatbotHealth(): Promise<string> {
  try {
    const response = await fetch(`${API_BASE_URL}/health`);
    const text = await response.text();
    return text;
    
  } catch (error) {
    console.error("헬스 체크 오류:", error);
    return "서버 연결 실패";
  }
}

/**
 * 타입 export (다른 파일에서 사용 가능)
 */
export type { ChatContext, QuickReply, ChatRequest, ChatResponse };
