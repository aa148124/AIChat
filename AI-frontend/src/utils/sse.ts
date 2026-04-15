export interface StreamHandlers {
  onData: (chunk: string) => void;
  onComplete: () => void;
  onError: (error: Error) => void;
}

const CHAT_PATH = "/ai/chat";
const AGENT_CHAT_PATH = "/ai/agent/chat";
const OLLAMA_CHAT_PATH = "/ai/ollama/chat";

async function streamSseGet(url: string, handlers: StreamHandlers, signal?: AbortSignal): Promise<void> {
  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "text/event-stream"
    },
    signal
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status} ${response.statusText}`);
  }

  if (!response.body) {
    throw new Error("SSE response body is empty");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split("\n\n");
      buffer = blocks.pop() ?? "";

      for (const block of blocks) {
        const lines = block.split("\n");
        const dataLines = lines
          .filter((line) => line.startsWith("data:"))
          .map((line) => line.slice(5).trimStart());

        if (dataLines.length > 0) {
          handlers.onData(dataLines.join("\n"));
        }
      }
    }

    if (buffer.trim()) {
      const lines = buffer.split("\n");
      const dataLines = lines
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.slice(5).trimStart());
      if (dataLines.length > 0) {
        handlers.onData(dataLines.join("\n"));
      }
    }

    handlers.onComplete();
  } catch (error) {
    if (signal?.aborted) {
      return;
    }
    handlers.onError(error instanceof Error ? error : new Error("Unknown SSE error"));
  } finally {
    reader.releaseLock();
  }
}

export async function streamChatMessage(
  memoryId: string,
  message: string,
  handlers: StreamHandlers,
  signal?: AbortSignal
): Promise<void> {
  const url = `${CHAT_PATH}?memoryId=${encodeURIComponent(memoryId)}&message=${encodeURIComponent(message)}`;
  return streamSseGet(url, handlers, signal);
}

export async function streamAgentChat(
  userId: string,
  message: string,
  handlers: StreamHandlers,
  signal?: AbortSignal
): Promise<void> {
  const url = `${AGENT_CHAT_PATH}?userId=${encodeURIComponent(userId)}&message=${encodeURIComponent(message)}`;
  return streamSseGet(url, handlers, signal);
}

export async function streamOllamaChat(
  memoryId: string,
  message: string,
  handlers: StreamHandlers,
  signal?: AbortSignal
): Promise<void> {
  const url = `${OLLAMA_CHAT_PATH}?memoryId=${encodeURIComponent(memoryId)}&message=${encodeURIComponent(message)}`;
  return streamSseGet(url, handlers, signal);
}
