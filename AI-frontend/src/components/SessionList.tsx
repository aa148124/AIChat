import type { Session } from "../types";
import styles from "./SessionList.module.css";

interface Props {
  sessions: Session[];
  activeMemoryId: string;
  isStreaming: boolean;
  onCreateSession: () => void;
  onSelectSession: (memoryId: string) => void;
}

export default function SessionList({
  sessions,
  activeMemoryId,
  isStreaming,
  onCreateSession,
  onSelectSession
}: Props) {
  return (
    <aside className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>Conversations</h2>
        <button className={styles.newButton} onClick={onCreateSession}>
          + New
        </button>
      </div>

      <div className={styles.list}>
        {sessions.map((session) => {
          const isActive = session.memoryId === activeMemoryId;
          return (
            <button
              key={session.memoryId}
              className={`${styles.item} ${isActive ? styles.active : ""}`}
              onClick={() => onSelectSession(session.memoryId)}
              disabled={isStreaming}
            >
              <div className={styles.itemTitle}>{session.name || "Untitled"}</div>
              <div className={styles.itemMeta}>{session.lastMessage || "No messages yet"}</div>
            </button>
          );
        })}
      </div>
    </aside>
  );
}
