package com.demo.ticket.Config.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

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

        List<String> users = simpUserRegistry.getUsers()
                .stream()
                .map(SimpUser::getName)
                .toList();

        logger.info(
                "準備推播 Email={}, WebSocket users={}",
                message.getEmail(),
                users
        );

        boolean online = users.contains(message.getEmail());

        logger.info(
                "目標使用者是否在線 Email={}, online={}",
                message.getEmail(),
                online
        );

        messagingTemplate.convertAndSendToUser(
                message.getEmail(),
                "/queue/notifications",
                message
        );

        logger.info(
                "已呼叫 WebSocket 推播 Email={}",
                message.getEmail()
        );
    }

}
