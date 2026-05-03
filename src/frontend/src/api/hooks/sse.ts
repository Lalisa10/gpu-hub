import { useEffect, useState } from 'react';

function parseFrame(frame: string): string | null {
  const lines = frame.split('\n');
  const dataLines: string[] = [];
  for (const line of lines) {
    if (line.startsWith('data:')) {
      dataLines.push(line.startsWith('data: ') ? line.slice(6) : line.slice(5));
    }
  }
  return dataLines.length ? dataLines.join('\n') : null;
}

interface OpenStreamOptions {
  onMessage: (data: string) => void;
  onError?: () => void;
}

/**
 * Opens an SSE stream and invokes onMessage with the joined `data:` payload of each frame.
 * Returns a teardown that aborts the stream. Use this for raw text streams (e.g. logs)
 * — for JSON streams, prefer makeSseHook which parses each frame.
 */
export function openSseTextStream(url: string, opts: OpenStreamOptions): () => void {
  const token = localStorage.getItem('access_token');
  const ctrl = new AbortController();

  (async () => {
    try {
      const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
        signal: ctrl.signal,
      });
      if (!res.ok || !res.body) throw new Error('SSE connect failed');
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buf = '';
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buf += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
        let idx;
        while ((idx = buf.indexOf('\n\n')) >= 0) {
          const frame = buf.slice(0, idx);
          buf = buf.slice(idx + 2);
          const payload = parseFrame(frame);
          if (payload !== null) opts.onMessage(payload);
        }
      }
    } catch (e) {
      if ((e as Error).name !== 'AbortError') opts.onError?.();
    }
  })();

  return () => ctrl.abort();
}

interface SseHookOptions<T> {
  parse?: (data: string) => T;
}

export function makeSseHook<T>(
  buildUrl: (id: string) => string,
  options: SseHookOptions<T> = {},
) {
  const parse = options.parse ?? ((s: string) => JSON.parse(s) as T);

  return function useSseStream(id: string | null, enabled: boolean) {
    const [data, setData] = useState<T | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isError, setIsError] = useState(false);

    useEffect(() => {
      if (!enabled || !id) {
        setData(null);
        return;
      }
      setIsLoading(true);
      setIsError(false);

      return openSseTextStream(buildUrl(id), {
        onMessage: (raw) => {
          setIsLoading(false);
          try {
            setData(parse(raw));
          } catch {
            /* skip malformed */
          }
        },
        onError: () => {
          setIsLoading(false);
          setIsError(true);
        },
      });
    }, [id, enabled]);

    return { data, isLoading, isError };
  };
}
