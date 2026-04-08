import { FormEvent, useState } from "react";
import styles from "./InputBox.module.css";

interface Props {
  disabled: boolean;
  isStreaming: boolean;
  onSend: (message: string) => Promise<void> | void;
  onStop: () => void;
}

export default function InputBox({ disabled, isStreaming, onSend, onStop }: Props) {
  const [value, setValue] = useState("");

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!value.trim() || disabled) {
      return;
    }
    const text = value;
    setValue("");
    await onSend(text);
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <textarea
        className={styles.input}
        placeholder="Type your message..."
        value={value}
        onChange={(event) => setValue(event.target.value)}
        rows={2}
        disabled={disabled}
      />
      <div className={styles.actions}>
        {isStreaming ? (
          <button type="button" className={styles.stopButton} onClick={onStop}>
            Stop
          </button>
        ) : null}
        <button type="submit" className={styles.sendButton} disabled={disabled || !value.trim()}>
          Send
        </button>
      </div>
    </form>
  );
}
