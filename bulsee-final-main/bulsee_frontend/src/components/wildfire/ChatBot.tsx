import { useState, useRef, useEffect } from "react";
import { Send, X } from "lucide-react";
import { getBotResponse, resetChatSession } from "../../lib/chat";
import type { QuickReply } from "../../lib/chat";

interface ChatBotProps {
  allRegions?: any[]; 
  onSelectRegion?: (region: any) => void;// 선택적 prop (향후 확장용)
}

interface Message {
  id: number;
  text: string;
  sender: "user" | "bot";
  type?: string;
  data?: any;
  quickReplies?: QuickReply[];
}

export function ChatBot({ allRegions, onSelectRegion }: ChatBotProps) {
  // ========== 상태 관리 ==========
  const [isOpen, setIsOpen] = useState(false); // 챗봇 열림/닫힘
  const [input, setInput] = useState(""); // 입력 필드
  const [isLoading, setIsLoading] = useState(false); // 로딩 상태

  // 메시지 히스토리
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 1,
      text: "안녕하세요!\n🔥 산불 예측 AI 챗봇 '불씨'입니다.\n무엇을 도와드릴까요?\n채팅으로 물어보시거나 아래 버튼을 선택해주세요!",
      sender: "bot",
      type: "text",
      quickReplies: [
        { label: "🔥 산불 예측", value: "산불 예측" },
        { label: "📊 통계", value: "산불 통계" },
        { label: "❓ 도움말", value: "도움말" },
      ],
    },
  ]);

  // 자동 스크롤 참조
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // ========== 드래그 이동 상태 ==========
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const dragStartPos = useRef({ x: 0, y: 0 });
  const startOffset = useRef({ x: 0, y: 0 });

  /**
   * 드래그 시작
   */
  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    dragStartPos.current = { x: e.clientX, y: e.clientY };
    startOffset.current = { ...offset };
  };

  /**
   * 드래그 이동 처리
   */
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging) return;
      e.preventDefault();

      const deltaX = e.clientX - dragStartPos.current.x;
      const deltaY = e.clientY - dragStartPos.current.y;

      setOffset({
        x: startOffset.current.x + deltaX,
        y: startOffset.current.y + deltaY,
      });
    };

    const handleMouseUp = () => setIsDragging(false);

    if (isDragging) {
      window.addEventListener("mousemove", handleMouseMove);
      window.addEventListener("mouseup", handleMouseUp);
    }

    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [isDragging, offset]);

  /**
   * 메시지 전송 (텍스트를 직접 받아서 보내는 형태로 안정화)
   */
  const sendMessage = async (text: string) => {
    const userMsg = text.trim();
    if (!userMsg || isLoading) return;

    // 사용자 메시지 추가
    setMessages((prev) => [
      ...prev,
      { id: Date.now(), text: userMsg, sender: "user" },
    ]);

    setIsLoading(true);

    try {
      // 백엔드 호출
      const response = await getBotResponse(userMsg);

      // 봇 응답 추가
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          text: response.message,
          sender: "bot",
          type: response.type,
          data: response.data,
          quickReplies: response.quickReplies,
        },
      ]);
      //예측 결과인 경우 지도를 해당 지역으로 이동시킴
      if (response.type === "prediction" && response.data && onSelectRegion) {
        console.log("📍 지도 연동 데이터:", response.data);
        onSelectRegion(response.data); // 부모로부터 받은 함수 실행
      }
    } catch (error) {
      console.error("메시지 전송 실패:", error);

      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          text: "죄송합니다. 오류가 발생했습니다. 😥",
          sender: "bot",
          type: "text",
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * 입력창에서 전송
   */
  const handleSend = async () => {
    const text = input;
    setInput("");
    await sendMessage(text);
  };

  /**
   * 빠른 답변 버튼 클릭 (setTimeout/상태경합 제거)
   */
  const handleQuickReply = async (value: string) => {
    await sendMessage(value);
  };

  /**
   * 대화 초기화
   */
  const handleResetChat = async () => {
    if (!confirm("대화 내역을 모두 지우시겠습니까?")) return;

    await resetChatSession();

    setMessages([
      {
        id: Date.now(),
        text: "안녕하세요! '불씨'입니다. 무엇을 도와드릴까요? 🔥",
        sender: "bot",
        type: "text",
        quickReplies: [
          { label: "🔥 산불 예측", value: "산불 예측" },
          { label: "📊 통계", value: "산불 통계" },
          { label: "❓ 도움말", value: "도움말" },
        ],
      },
    ]);
  };

  /**
   * 자동 스크롤 (새 메시지 시)
   */
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  /**
   * 챗봇 닫을 때 위치 초기화
   */
  useEffect(() => {
    if (!isOpen) setOffset({ x: 0, y: 0 });
  }, [isOpen]);

  // ========== UI 렌더링 ==========
  return (
    <div
      id="bulsee-chatbot"
      style={{
        position: "fixed",
        bottom: "1.5rem",
        left: "1.5rem",
        zIndex: 999999,
        transform: `translate(${offset.x}px, ${offset.y}px)`,
        transition: isDragging ? "none" : "transform 0.1s ease-out",
        pointerEvents: "auto",
      }}
    >
      {/* ===== 채팅창 ===== */}
      {isOpen && (
        <div className="bg-white w-[380px] h-[550px] rounded-xl shadow-2xl border border-gray-200 flex flex-col mb-4 overflow-hidden">
          {/* 헤더 */}
          <div
            onMouseDown={handleMouseDown}
            className="bg-[#EB5310] p-3 flex justify-between items-center text-white cursor-move select-none"
          >
            <div className="flex items-center gap-2 pointer-events-none">
              <span className="text-xl">🔥</span>
              <span className="font-bold">불씨 AI</span>
            </div>
            <div className="flex gap-1">
              <button
                onMouseDown={(e) => e.stopPropagation()}
                onClick={handleResetChat}
                className="hover:bg-white/20 rounded p-1 transition text-sm"
                title="대화 초기화"
              >
                🔄
              </button>
              <button
                onMouseDown={(e) => e.stopPropagation()}
                onClick={() => setIsOpen(false)}
                className="hover:bg-white/20 rounded p-1 transition"
              >
                <X size={18} />
              </button>
            </div>
          </div>

          {/* 대화 내용 */}
          <div className="flex-1 overflow-y-auto p-3 bg-gray-50 space-y-3">
            {messages.map((msg) => (
              <div key={msg.id}>
                {/* 메시지 말풍선 */}
                <div
                  className={`flex ${
                    msg.sender === "user" ? "justify-end" : "justify-start"
                  }`}
                >
                  {msg.sender === "bot" && (
                    <div className="w-7 h-7 rounded-full bg-[#EB5310] flex items-center justify-center mr-2 flex-shrink-0 text-sm">
                      🔥
                    </div>
                  )}
                  <div
                    className={`max-w-[75%] px-3 py-2 rounded-lg text-sm ${
                      msg.sender === "user"
                        ? "bg-[#EB5310] text-white"
                        : "bg-white text-gray-800 border border-gray-200"
                    }`}
                    style={{ whiteSpace: "pre-wrap", lineHeight: "1.5" }}
                  >
                    {msg.text}
                  </div>
                </div>

                {/* 빠른 답변 버튼 */}
                {msg.quickReplies && msg.quickReplies.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2 ml-9">
                    {msg.quickReplies.map((reply, idx) => (
                      <button
                        key={idx}
                        onClick={() => handleQuickReply(reply.value)}
                        className="px-2.5 py-1.5 bg-white border border-gray-300 rounded-md text-xs hover:bg-[#EB5310] hover:text-white hover:border-[#EB5310] transition-colors"
                      >
                        {reply.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {/* 로딩 애니메이션 */}
            {isLoading && (
              <div className="flex justify-start items-end gap-2">
                <div className="w-7 h-7 rounded-full bg-[#EB5310] flex items-center justify-center text-sm">
                  🔥
                </div>
                <div className="bg-white px-3 py-2 rounded-lg border border-gray-200 flex gap-1">
                  <span
                    className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce"
                    style={{ animationDelay: "0s" }}
                  />
                  <span
                    className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce"
                    style={{ animationDelay: "0.2s" }}
                  />
                  <span
                    className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce"
                    style={{ animationDelay: "0.4s" }}
                  />
                </div>
              </div>
            )}

            {/* 자동 스크롤 앵커 */}
            <div ref={messagesEndRef} />
          </div>

          {/* 입력창 */}
          <div className="p-2 bg-white border-t border-gray-200 flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSend()}
              placeholder="메시지를 입력하세요..."
              disabled={isLoading}
              className="flex-1 bg-gray-100 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#EB5310] transition placeholder-gray-400 disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={isLoading || !input.trim()}
              className="bg-[#EB5310] text-white p-2 rounded-lg hover:bg-[#d44a0e] disabled:opacity-50 transition-colors flex items-center justify-center"
            >
              <Send size={16} />
            </button>
          </div>
        </div>
      )}

      {/* ===== 닫혀있을 때 버튼 ===== */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="relative flex items-center justify-center w-14 h-14 bg-[#EB5310] rounded-full shadow-lg hover:bg-[#d44a0e] hover:scale-105 transition-all duration-200"
        >
          <span className="text-2xl">🔥</span>
          <span className="absolute top-0 right-0 w-3 h-3 bg-red-600 rounded-full border-2 border-white animate-pulse" />
        </button>
      )}
    </div>
  );
}
