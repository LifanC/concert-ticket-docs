import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { toFindCookie } from '@/components/componentsJs/cookie'

const webSocketUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/api/ws'

let client = null;

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

                    ElMessage({
                        type: "success",
                        dangerouslyUseHTMLString: true,
                        message:
                            notification.title +
                            "<br>" +
                            notification.content,
                        duration: 600000,
                        showClose: true
                    });
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
    connectWebSocket, disconnectWebSocket
}
