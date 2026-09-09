# 演唱會訂票系統前端

前端為以 Vue 3 建置，提供會員、活動、訂票與管理員操作介面。

## 技術

- Vue 3
- Vue Router
- Element Plus 2.14.3
- Axios
- SockJS 與 `@stomp/stompjs`

## 需求

- Node.js 20.19.0 以上（依 package.json 設定）
- npm
- 可連線的後端 API（預設為 `http://localhost:8080/api`）

## 本機執行

在本目錄依 `package-lock.json` 安裝相依套件並啟動開發伺服器：

```powershell
npm ci
npm run dev
```

若要新增或更新相依套件，請使用 `npm install <package>`，並一併提交更新後的 `package.json` 與 `package-lock.json`。

開發伺服器預設網址為 http://localhost:5173。

## 後端連線設定

REST API 由 `src/services/api.js` 統一管理，WebSocket 由 `src/services/websocket.js` 管理。在本目錄建立 `.env.local` 可覆寫預設值：

```properties
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080/api/ws
```

API base URL 包含 `/api`，不需附加 `/v1`。修改後重啟開發伺服器；建置版本需重新 build。後端 REST CORS 目前只允許 `http://localhost:5173`，更換前端來源時需同步調整後端 `SecurityConfig`。

Axios 使用 `withCredentials` 並附帶 `Authorization: Bearer <accessToken>`。Refresh Token 由後端儲存於 HttpOnly Cookie；遇到 401 時呼叫 `/v1/login/validate` 更新 Access Token 並重試一次，更新失敗則返回會員頁。

WebSocket 透過 STOMP `CONNECT` 的 Authorization 標頭驗證，訂閱 `/user/queue/notifications` 接收個人通知。

## 功能

- 會員註冊、登入、修改會員資料與登出。
- 瀏覽、搜尋與篩選活動；可查看活動詳情。
- 選擇活動日期、場次與座位後建立訂單；不可用座位依 `session_id` 查詢。
- 查看票券、付款與取消訂單，接收個人訂單通知。
- 管理員可管理活動與場次，並查看售票資料。

- 加入／取消活動收藏及只看收藏。

同次訂位重試保留 `Idempotency-Key`，成功後清除；重整頁面不保留 key。付款目前為訂單狀態操作，尚未串接外部金流。

## 建置與預覽

```powershell
npm run build
npm run preview
```

產物位於 `dist/`；preview 用於本機檢查建置結果。目前 npm scripts 未設定自動化測試。

## Docker

由專案根目錄執行下列指令，即可連同後端、PostgreSQL 與 Redis 一起啟動：

```powershell
docker compose up --build
```

先在根目錄依 `.env.example` 準備 `.env`。前端容器掛載原始碼並執行 Vite 開發伺服器，網址為 http://localhost:5173。

詳見[專案說明](../README.md)及[後端說明](../concert-ticket-backend/README.md)。
