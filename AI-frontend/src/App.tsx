import ChatWindow from "./components/ChatWindow";
import SessionList from "./components/SessionList";
import { useChat } from "./hooks/useChat";
import styles from "./App.module.css";

export default function App() {
  const {
    sessions,
    activeMemoryId,
    activeMessages,
    isStreaming,
    error,
    createSession,
    switchSession,
    sendMessage,
    stopStreaming,
    retryLast
  } = useChat();

  return (
    <div className={styles.page}>
      <div className={styles.layout}>
        <SessionList
          sessions={sessions}
          activeMemoryId={activeMemoryId}
          isStreaming={isStreaming}
          onCreateSession={createSession}
          onSelectSession={switchSession}
        />
        <ChatWindow
          messages={activeMessages}
          isStreaming={isStreaming}
          error={error}
          onSend={sendMessage}
          onStop={stopStreaming}
          onRetry={retryLast}
        />
      </div>
    </div>
  );
}
