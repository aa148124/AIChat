export interface Session {
  memoryId: string;
  name: string;
  lastMessage: string;
  timestamp: number;
}

export type MessageRole = "user" | "assistant";

export interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: number;
}

export type MessageMap = Record<string, Message[]>;
