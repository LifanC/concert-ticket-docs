// 閱讀導引：建立 STOMP 即時通知連線，訂閱會員個人佇列並避免重複顯示相同通知。
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { toFindCookie } from '@/components/componentsJs/cookie.js'

const webSocketUrl = import.meta.env.VITE_WS_URL || '/api/ws';

let client = null;
const displayedNotifications = new Set();
const paymentDeadlineTitles = new Set(['付款即將到期', '付款期限已到']);

/** 有 id 的通知只顯示一次，最近紀錄最多保留 100 筆；付款期限通知顯示 30 秒，無 id 的通知不參與去重。 */
function showNotification(notification) {
    if (notification.id && displayedNotifications.has(notification.id)) return;
    if (notification.id) {
        displayedNotifications.add(notification.id);
        // 只保留最近 100 個通知識別碼，避免長時間連線讓去重集合無限增長。
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

/** 以呼叫當下的 access token 建立 STOMP CONNECT 標頭，啟用自動重連及心跳；已啟用的客戶端不重建。 */
function connectWebSocket() {

    if (client?.active) {
        console.log("WebSocket 已經連線");
        return;
    }

    console.log("開始建立 websocket");

    let accessToken = toFindCookie('accessToken')

    client = new Client({
        webSocketFactory: () =>
            new SockJS(new URL(webSocketUrl, window.location.origin).href),
        connectHeaders: {
            Authorization: `Bearer ${accessToken}`
        },

        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        debug: (msg) => {
            console.log(msg);
        },

        /** STOMP 連線成功後訂閱會員個人通知；重新連線時也會重新建立訂閱。 */
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

/** 停止目前 STOMP 客戶端並清空連線參照。 */
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
