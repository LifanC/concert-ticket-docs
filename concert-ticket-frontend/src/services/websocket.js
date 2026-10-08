import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { toFindCookie } from '@/components/componentsJs/cookie.js'

const webSocketUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/api/ws'

let client = null;
const displayedNotifications = new Set();
const paymentDeadlineTitles = new Set(['付款即將到期', '付款期限已到']);

function showNotification(notification) {
    if (notification.id && displayedNotifications.has(notification.id)) return;
    if (notification.id) {
        displayedNotifications.add(notification.id);
        if (displayedNotifications.size > 100) {
            displayedNotifications.delete(displayedNotifications.values().next().value);
        }
    }
    ElMessage({
        type: ['success', 'warning', 'error', 'info'].includes(notification.type)
            ? notification.type : 'success',
        message: `${notification.title}：${notification.content}`,
        duration: (paymentDeadlineTitles.has(notification.title)
            || notification.title?.startsWith('新通知：請在 '))
            ? 30000
            : notification.type === 'warning' ? 8000 : 5000,
        showClose: true
    });
}

function connectWebSocket() {

    if (client?.active) {
        console.log("WebSocket 已經連線");
        return;
    }

    console.log("開始建立 websocket");

    let accessToken = toFindCookie('accessToken')

    client = new Client({
        webSocketFactory: () =>
            new SockJS(webSocketUrl),
        connectHeaders: {
            Authorization: `Bearer ${accessToken}`
        },

        debug: (msg) => {
            console.log(msg);
        },

        onConnect() {
            console.log("WebSocket 連線成功");

            client.subscribe(
                "/user/queue/notifications",
                (msg) => {
                    const notification = JSON.parse(msg.body);

                    console.log("收到通知", notification);

                    showNotification(notification);
                }
            );
        },

        onStompError: (frame) => {
            console.error("STOMP ERROR", frame);
        },

        onWebSocketError: (error) => {
            console.error("WS ERROR", error);
        }
    });

    client.activate();
}

function disconnectWebSocket() {
    if (client) {
        client.deactivate();
        client = null;

        console.log("WebSocket 已斷線");
    }
}

export {
    connectWebSocket, disconnectWebSocket, showNotification
}
