import { useEffect, useMemo, useRef, useState } from "react";
import type { Message, MessageMap, Session } from "../types";
import { streamChatMessage } from "../utils/sse";

const STORAGE_KEYS = {
  sessions: "chat:sessions",
  messages: "chat:messages",
  activeMemoryId: "chat:activeMemoryId"
};

function uuid(): string {
  return crypto.randomUUID();
}

function now(): number {
  return Date.now();
}

function createDefaultSession(): Session {
  return {
    memoryId: uuid(),
    name: "New Chat",
    lastMessage: "",
    timestamp: now()
  };
}

function parseLocalStorage<T>(key: string, fallback: T): T {
  const raw = localStorage.getItem(key);
  if (!raw) {
    return fallback;
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function useChat() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [messagesBySession, setMessagesBySession] = useState<MessageMap>({});
  const [activeMemoryId, setActiveMemoryId] = useState<string>("");
  const [isStreaming, setIsStreaming] = useState(false);
  const [error, setError] = useState<string>("");
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const savedSessions = parseLocalStorage<Session[]>(STORAGE_KEYS.sessions, []);
    const savedMessages = parseLocalStorage<MessageMap>(STORAGE_KEYS.messages, {});
    const savedActiveId = localStorage.getItem(STORAGE_KEYS.activeMemoryId) ?? "";

    if (savedSessions.length === 0) {
      const initial = createDefaultSession();
      setSessions([initial]);
      setMessagesBySession({ [initial.memoryId]: [] });
      setActiveMemoryId(initial.memoryId);
      return;
    }

    setSessions(savedSessions.sort((a, b) => b.timestamp - a.timestamp));
    setMessagesBySession(savedMessages);
    setActiveMemoryId(savedActiveId || savedSessions[0].memoryId);
  }, []);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.sessions, JSON.stringify(sessions));
  }, [sessions]);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.messages, JSON.stringify(messagesBySession));
  }, [messagesBySession]);

  useEffect(() => {
    if (activeMemoryId) {
      localStorage.setItem(STORAGE_KEYS.activeMemoryId, activeMemoryId);
    }
  }, [activeMemoryId]);

  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort();
    };
  }, []);

  const activeMessages = useMemo(
    () => messagesBySession[activeMemoryId] ?? [],
    [activeMemoryId, messagesBySession]
  );

  const createSession = () => {
    const session = createDefaultSession();
    setSessions((prev) => [session, ...prev]);
    setMessagesBySession((prev) => ({ ...prev, [session.memoryId]: [] }));
    setActiveMemoryId(session.memoryId);
    setError("");
  };

  const switchSession = (memoryId: string) => {
    if (isStreaming) {
      return;
    }
    setActiveMemoryId(memoryId);
    setError("");
  };

  const stopStreaming = () => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    setIsStreaming(false);
  };

  const retryLast = async () => {
    const list = activeMessages;
    const latestUser = [...list].reverse().find((item) => item.role === "user");
    if (!latestUser || isStreaming) {
      return;
    }
    await sendMessage(latestUser.content);
  };

  const sendMessage = async (content: string) => {
    const trimmed = content.trim();
    if (!trimmed || !activeMemoryId || isStreaming) {
      return;
    }
    const memoryId = activeMemoryId;

    setError("");
    setIsStreaming(true);

    const userMessage: Message = {
      id: uuid(),
      role: "user",
      content: trimmed,
      timestamp: now()
    };

    const assistantMessage: Message = {
      id: uuid(),
      role: "assistant",
      content: "",
      timestamp: now()
    };

    setMessagesBySession((prev) => {
      const list = prev[memoryId] ?? [];
      return {
        ...prev,
        [memoryId]: [...list, userMessage, assistantMessage]
      };
    });

    setSessions((prev) =>
      prev
        .map((session) =>
          session.memoryId === memoryId
            ? {
                ...session,
                name: session.name === "New Chat" ? trimmed.slice(0, 20) : session.name,
                lastMessage: trimmed,
                timestamp: now()
              }
            : session
        )
        .sort((a, b) => b.timestamp - a.timestamp)
    );

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      await streamChatMessage(
        memoryId,
        trimmed,
        {
          onData: (chunk) => {
            setMessagesBySession((prev) => {
              const list = prev[memoryId] ?? [];
              const next = [...list];
              const idx = next.findIndex((item) => item.id === assistantMessage.id);
              if (idx === -1) {
                return prev;
              }
              next[idx] = { ...next[idx], content: next[idx].content + chunk };
              return { ...prev, [memoryId]: next };
            });
          },
          onComplete: () => {
            setIsStreaming(false);
            setMessagesBySession((prev) => {
              const list = prev[memoryId] ?? [];
              const last = list[list.length - 1];
              setSessions((sessionPrev) =>
                sessionPrev
                  .map((session) =>
                    session.memoryId === memoryId
                      ? { ...session, lastMessage: last?.content || session.lastMessage, timestamp: now() }
                      : session
                  )
                  .sort((a, b) => b.timestamp - a.timestamp)
              );
              return prev;
            });
          },
          onError: (streamError) => {
            setIsStreaming(false);
            setError(streamError.message || "SSE stream failed.");
          }
        },
        controller.signal
      );
    } catch (requestError) {
      setIsStreaming(false);
      setError(requestError instanceof Error ? requestError.message : "Request failed.");
    } finally {
      abortControllerRef.current = null;
    }
  };

  return {
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
  };
}
