import { useEffect, useRef } from "react";
import type { ChatMode, Message } from "../types";
import MessageBubble from "./MessageBubble";
import InputBox from "./InputBox";
import styles from "./ChatWindow.module.css";

interface Props {
  messages: Message[];
  isStreaming: boolean;
  error: string;
  chatMode: ChatMode;
  onChatModeChange: (mode: ChatMode) => void;
  onSend: (message: string) => Promise<void>;
  onStop: () => void;
  onRetry: () => Promise<void>;
}

export default function ChatWindow({
  messages,
  isStreaming,
  error,
  chatMode,
  onChatModeChange,
  onSend,
  onStop,
  onRetry
}: Props) {
  const listRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    listRef.current?.scrollTo({
      top: listRef.current.scrollHeight,
      behavior: "smooth"
    });
  }, [messages, isStreaming]);

  const title = chatMode === "agent" ? "Chained Agent" : chatMode === "ollama" ? "Ollama 本地模型" : "AI Assistant";

  return (
    <section className={styles.container}>
      <header className={styles.header}>
        <h2 className={styles.title}>{title}</h2>
        <div className={styles.headerRight}>
          <label className={styles.modeField}>
            <span className={styles.modeLabel}>模式</span>
            <select
              className={styles.modeSelect}
              value={chatMode}
              disabled={isStreaming}
              onChange={(event) => onChatModeChange(event.target.value as ChatMode)}
            >
              <option value="standard">普通对话</option>
              <option value="agent">链式 Agent</option>
              <option value="ollama">Ollama 本地模型</option>
            </select>
          </label>
          {isStreaming ? <span className={styles.loading}>AI is typing...</span> : null}
        </div>
      </header>

      <div className={styles.messageList} ref={listRef}>
        {messages.length === 0 ? <div className={styles.empty}>Start a new conversation.</div> : null}
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
      </div>

      {error ? (
        <div className={styles.errorBox}>
          <span>{error}</span>
          <button className={styles.retryButton} onClick={onRetry}>
            Retry
          </button>
        </div>
      ) : null}

      <InputBox disabled={isStreaming} isStreaming={isStreaming} onSend={onSend} onStop={onStop} />
    </section>
  );
}
