package com.example.IncidentPulse.WebSocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Broadcasts incident events to STOMP subscribers on two topics:
 *  - /topic/incidents          (firehose of all incident events)
 *  - /topic/incidents/{id}     (a single incident's stream)
 */
@Component
public class IncidentEventPublisher {

    private static final String ALL_TOPIC = "/topic/incidents";

    private final SimpMessagingTemplate messagingTemplate;

    public IncidentEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(IncidentEvent event) {
        messagingTemplate.convertAndSend(ALL_TOPIC, event);
        if (event.getId() != null) {
            messagingTemplate.convertAndSend(ALL_TOPIC + "/" + event.getId(), event);
        }
    }
}
