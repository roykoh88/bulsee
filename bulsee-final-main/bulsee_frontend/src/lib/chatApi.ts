/**
 * 챗봇 백엔드 통신 라이브러리 (최종형)
 */

const API_BASE_URL = "http://localhost:8080/api/chatbot";

// ========== 타입 정의 ==========

/**
 * 지도시각화 및 상세 정보 연동 데이터 (백엔드 RegionDataDTO와 일치)
 */
export interface RegionData {
  sigCd?: string;
  name: string;
  stnId: number;
  lat: number;
  lng: number;
  riskLevel?: string;
  fireProbability?: number;
  typicalArea?: number;
  worstArea?: number;
  spreadDir?: string;
  temperature?: number;
  humidity?: number;
  windSpeed?: number;
  precipitation?: number;
}

export interface ChatContext {
  currentIntent?: string;
  step?: string;
  region?: string;
  date?: string;
}

export interface QuickReply {
  label: string;
  value: string;
}

export interface ChatRequest {
  sessionId: string;
  message: string;
  context?: ChatContext | null;
}

export interface ChatResponse {
  message: string;
  type: "text" | "prediction" | "statistics" | "faq" | "error"; // 타입 고정으로 안정성 향상
  data?: RegionData; // any 대신 명확한 지역 데이터 타입 사용
  quickReplies?: QuickReply[];
  context?: ChatContext;
}

// ========== 세션 관리 ==========

function getSessionId(): string {
  let sessionId = localStorage.getItem("chatbot_session_id");
  if (!sessionId) {
    sessionId = `session_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
    localStorage.setItem("chatbot_session_id", sessionId);
  }
  return sessionId;
}

// ========== API 호출 ==========

/**
 * 챗봇 응답 가져오기
 */
export async function getBotResponse(message: string): Promise<ChatResponse> {
  const sessionId = getSessionId();
  const request: ChatRequest = { sessionId, message, context: null };

  try {
    const response = await fetch(`${API_BASE_URL}/message`, { // ✅ 최신 엔드포인트
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`HTTP Error: ${response.status}`);
    }

    const data = (await response.json()) as ChatResponse;

    return {
      message: data?.message || "응답을 받지 못했습니다.",
      type: data?.type || "text",
      data: data?.data,
      quickReplies: data?.quickReplies || [],
      context: data?.context,
    };
  } catch (error) {
    console.error("API 호출 오류:", error);
    return {
      message: "서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요. 🔥",
      type: "error",
    };
  }
}

/**
 * 채팅 히스토리 초기화
 */
export async function resetChatSession(): Promise<void> {
  const sessionId = getSessionId();
  try {
    await fetch(`${API_BASE_URL}/history/${sessionId}`, { // ✅ DELETE 요청으로 수정
      method: "DELETE",
    });
    localStorage.removeItem("chatbot_session_id");
    console.log("✅ 세션 및 히스토리 초기화 완료");
  } catch (error) {
    console.error("세션 초기화 오류:", error);
  }
}

/**
 * 서버 헬스체크
 */
export async function checkChatbotHealth(): Promise<string> {
  try {
    const response = await fetch(`${API_BASE_URL}/health`);
    return response.ok ? await response.text() : "서버 상태 불안정";
  } catch (error) {
    return "서버 연결 실패";
  }
}