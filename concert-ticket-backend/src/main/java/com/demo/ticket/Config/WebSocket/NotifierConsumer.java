package com.demo.ticket.Config.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotifierConsumer {

    private final Logger logger = LoggerFactory.getLogger(NotifierConsumer.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    public NotifierConsumer(
            SimpMessagingTemplate messagingTemplate,
            SimpUserRegistry simpUserRegistry
    ) {
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
    }

    public void sendNotification(NotificationMessage message) {
        logger.info(
                "推播通知 Username={}, Title={}, Content={}",
                message.getUsername(),
                message.getTitle(),
                message.getContent()
        );
        messagingTemplate.convertAndSendToUser(
                message.getUsername(),
                "/queue/notifications",
                message
        );

        logger.info(
                "推播完成給 Username={}",
                message.getUsername()
        );

        logger.info(
                "WebSocket users={}",
                simpUserRegistry.getUsers()
                        .stream()
                        .map(user -> user.getName())
                        .toList()
        );

    }

}
