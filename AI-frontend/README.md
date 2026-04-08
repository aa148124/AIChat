# AI Chat Frontend

React 18 + TypeScript + Vite chat UI for SSE backend.

## Run

```bash
npm install
npm run dev
```

Dev server: `http://localhost:5173`

Backend endpoint expected: `http://localhost:8081/ai/chat`

## Notes

- Vite proxy is configured in `vite.config.ts`, so frontend calls `/ai/chat`.
- Session list and message history are persisted in `localStorage`.
- Supports stop generation, retry on errors, and markdown rendering.
