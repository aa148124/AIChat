import { useEffect, useRef } from "react";
import type { Message } from "../types";
import MessageBubble from "./MessageBubble";
import InputBox from "./InputBox";
import styles from "./ChatWindow.module.css";

interface Props {
  messages: Message[];
  isStreaming: boolean;
  error: string;
  onSend: (message: string) => Promise<void>;
  onStop: () => void;
  onRetry: () => Promise<void>;
}

export default function ChatWindow({ messages, isStreaming, error, onSend, onStop, onRetry }: Props) {
  const listRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    listRef.current?.scrollTo({
      top: listRef.current.scrollHeight,
      behavior: "smooth"
    });
  }, [messages, isStreaming]);

  return (
    <section className={styles.container}>
      <header className={styles.header}>
        <h2 className={styles.title}>AI Assistant</h2>
        {isStreaming ? <span className={styles.loading}>AI is typing...</span> : null}
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
