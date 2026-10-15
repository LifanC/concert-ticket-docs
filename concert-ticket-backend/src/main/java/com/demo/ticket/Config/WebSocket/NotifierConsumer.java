// 閱讀導引：將通知傳送至指定會員的個人訊息佇列；接收端訂閱 /user/queue/notifications。
package com.demo.ticket.Config.WebSocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class NotifierConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;

    /**
     * 注入訊息傳送器與線上使用者登錄表，供推播及連線診斷使用。
     */
    public NotifierConsumer(
            SimpMessagingTemplate messagingTemplate,
            SimpUserRegistry simpUserRegistry
    ) {
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
    }

    /**
     * 將通知傳到指定會員的個人 WebSocket 佇列；此呼叫不保證離線會員已讀取訊息。
     */
    public void sendNotification(NotificationMessage message) {

        List<String> users = simpUserRegistry.getUsers()
                .stream()
                .map(SimpUser::getName)
                .toList();

        log.info(
                "準備推播 Email={}, WebSocket users={}",
                message.getEmail(),
                users
        );

        boolean online = users.contains(message.getEmail());

        log.info(
                "目標使用者是否在線 Email={}, online={}",
                message.getEmail(),
                online
        );

        messagingTemplate.convertAndSendToUser(
                message.getEmail(),
                "/queue/notifications",
                message
        );

        log.info(
                "已呼叫 WebSocket 推播 Email={}",
                message.getEmail()
        );
    }

}
