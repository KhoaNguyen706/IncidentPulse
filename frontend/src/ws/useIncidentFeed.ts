import { useEffect } from 'react';
import type { IncidentEvent } from '../types';
import { useLiveFeed } from './LiveFeedContext';

export function useIncidentFeed(onEvent?: (event: IncidentEvent) => void) {
  const { connected, subscribe } = useLiveFeed();

  useEffect(() => {
    if (!onEvent) return;
    return subscribe(onEvent);
  }, [subscribe, onEvent]);

  return { connected };
}
