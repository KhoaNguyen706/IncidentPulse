import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getAccessToken, getSockJsUrl } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { IncidentEvent } from '../types';

type EventHandler = (event: IncidentEvent) => void;

interface LiveFeedContextValue {
  connected: boolean;
  subscribe: (handler: EventHandler) => () => void;
}

const LiveFeedContext = createContext<LiveFeedContextValue | null>(null);

export function LiveFeedProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [connected, setConnected] = useState(false);
  const handlersRef = useRef<Set<EventHandler>>(new Set());

  useEffect(() => {
    if (!user) {
      setConnected(false);
      return;
    }

    const token = getAccessToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(getSockJsUrl()) as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/incidents', (msg: IMessage) => {
          const event = JSON.parse(msg.body) as IncidentEvent;
          handlersRef.current.forEach((h) => h(event));
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [user]);

  const subscribe = (handler: EventHandler) => {
    handlersRef.current.add(handler);
    return () => handlersRef.current.delete(handler);
  };

  return (
    <LiveFeedContext.Provider value={{ connected, subscribe }}>
      {children}
    </LiveFeedContext.Provider>
  );
}

export function useLiveFeed() {
  const ctx = useContext(LiveFeedContext);
  if (!ctx) throw new Error('useLiveFeed must be used within LiveFeedProvider');
  return ctx;
}

/** Per-incident topic subscription (detail page). */
export function useIncidentTopicFeed(
  incidentId: number | undefined,
  onEvent: (event: IncidentEvent) => void,
) {
  const { user } = useAuth();
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    if (!user || incidentId == null) return;

    const token = getAccessToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(getSockJsUrl()) as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/incidents/${incidentId}`, (msg: IMessage) => {
          onEventRef.current(JSON.parse(msg.body) as IncidentEvent);
        });
      },
    });

    client.activate();
    return () => client.deactivate();
  }, [user, incidentId]);
}
