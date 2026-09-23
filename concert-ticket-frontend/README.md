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

範本已集中至根目錄 [`.env.example`](../.env.example)，前端只需將其中的 `VITE_*` 放入 `concert-ticket-frontend/.env` 或 `.env.local`。根目錄 `.env` 不會自動套用到 Vue；同名設定以 `.env.local` 優先於 `.env`。

```properties
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080/api/ws
```

API base URL 包含 `/api`，不需附加 `/v1`。修改後重啟開發伺服器；建置版本需重新 build。後端 REST CORS 目前只允許 `http://localhost:5173`，更換前端來源時需同步調整後端 `SecurityConfig`。

Axios 使用 `withCredentials` 並附帶 `Authorization: Bearer <accessToken>`。Refresh Token 由後端儲存於 HttpOnly Cookie；遇到 401 時呼叫 `/v1/login/validate` 更新 Access Token 並重試一次，更新失敗則返回會員頁。

WebSocket 透過 STOMP `CONNECT` 的 Authorization 標頭驗證，訂閱 `/user/queue/notifications` 接收個人通知。

## 功能

- 會員註冊、登入、修改會員資料與登出。
- 瀏覽、搜尋與篩選活動；活動列表與詳情可查看圖片並點擊放大。
- 選擇活動日期、場次與座位後建立訂單；不可用座位依 `session_id` 查詢。座位圖逐排顯示排號，可快速跳至指定排，座位區可捲動，並顯示可選／已選／不可選狀態。
- 查看票券、付款與取消訂單，接收個人訂單通知。
- 管理員可管理活動與場次、上傳活動 JPG 圖片，並查看售票資料。
- 管理員先選活動類型，再選 A 排起的最後一排：音樂演唱會最多 50 排（AX）、舞台劇 100 排（CV）、展覽特展 150 排（ET）；每排座位數為 1～10。
- 加入／取消活動收藏及只看收藏。

同次訂位重試保留 `Idempotency-Key`，成功後清除；重整頁面不保留 key。付款目前為訂單狀態操作，尚未串接外部金流。

Python 銷售分析由 Java 後端自動排程，輸出 CSV。

新版座位圖已通過前端建置；尚未以瀏覽器自動化驗證快速跳排、捲動與不同排數的畫面。

## 活動圖片

- 管理員在新增或修改活動時可選擇 JPG／JPEG，表單提供固定 320 × 180 px 預覽框；點「×」只關閉目前預覽，不會刪除已儲存圖片。
- 上傳檔案上限為 10 MB。後端保留圖片比例，將最長邊縮至不超過 1280 px，並將儲存的 JPEG 控制在 1 MB 以下。
- 圖片是選填的；編輯活動時若未選新圖片，原圖會保留。管理員活動表格及公開活動列表顯示 180 × 102 px 縮圖，點擊可放大；活動詳情也會顯示圖片，無圖時列表顯示「無圖片」。
- 圖片透過 API 讀取，不需要將本機 JPG 複製到前端 `public/`。

## 建置與預覽

```powershell
npm run build
npm run preview
```

產物位於 `dist/`；preview 用於本機檢查建置結果。

## Playwright 網頁自動化測試（Microsoft Edge）

Playwright 會自動操作 Microsoft Edge，測試活動搜尋、收藏、登入、票券查詢與訂票等網頁流程。測試程式放在 [tests/e2e](tests/e2e)。

[playwright.config.js](playwright.config.js) 是測試設定檔，執行下列指令時會自動讀取，不需要直接執行它。

執行前請安裝 Microsoft Edge，並先啟動前後端服務（方式見[根目錄 README](../README.md#本機開發)）。前端預設網址為 `http://localhost:5173`；測試指令不會自動啟動服務。

另開終端機，切換至 `concert-ticket-frontend` 目錄後執行：

```powershell
# 首次使用時安裝依賴；已安裝可略過
npm ci

# 執行測試，不顯示瀏覽器視窗
npm run test:e2e

# 想看到 Edge 自動操作畫面時，改用這個指令
npm run test:e2e:headed
```

完成後可查看 `playwright-report/index.html` 測試報告。`headed` 只是顯示瀏覽器視窗，執行的測試相同。

## Docker

由專案根目錄執行下列指令，即可連同後端、PostgreSQL 與 Redis 一起啟動：

```powershell
docker compose up --build
```

先在根目錄依 `.env.example` 準備 `.env`。前端容器掛載原始碼並執行 Vite 開發伺服器，網址為 http://localhost:5173。

詳見[專案說明](../README.md)及[後端說明](../concert-ticket-backend/README.md)。
