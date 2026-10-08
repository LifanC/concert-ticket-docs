# 演唱會訂票系統前端

前端為以 Vue 3 建置，提供會員、活動、訂票與管理員操作介面。

## 技術

- Vue 3
- Vue Router
- Element Plus 2.14.3
- Chart.js 4.5.1
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

銷售儀表板使用 Chart.js 顯示各場次票況的水平堆疊長條圖，以及訂單狀態圓環圖。滑鼠提示顯示票數或訂單筆數與比例，點擊圖例可切換資料顯示；重新整理 API 資料後，圖表同步更新。

Python 銷售分析由 Java 後端自動排程，輸出 CSV；儀表板直接使用 Java API 資料，未串接 Python 報表。

## 手機排版與共用列表

活動列表、管理後台的活動／場次／訂單列表，以及銷售儀表板的庫存列表，統一使用 `src/components/ResponsiveRecordList.vue`。螢幕寬度不超過 767px 時顯示直式卡片，其餘顯示表格，視窗尺寸改變時自動切換。

每個列表只定義一份 `fields`：`key` 對應資料欄位、`label` 為欄位名稱、`format` 處理顯示格式，`role` 指定標題、圖片、狀態或操作區。欄位設定與具名插槽同時供桌面與手機使用，新增欄位、修改按鈕或圖片內容只需改一次。

- `Activity.vue`：維護公開活動列表的 `activityFields` 與收藏、查看詳情按鈕。
- `Admin.vue`：維護 `activityFields`、`sessionFields`、`orderFields` 與管理操作按鈕。
- `AdminDashboard.vue`：維護 `inventoryFields`、圖表與統計資料。
- `ResponsiveRecordList.vue`：統一管理表格／卡片結構、空資料提示及手機卡片間距。

會員中心標題區與銷售概況工具列在手機保留左右 16px 內距；表單、購票摘要與導覽列支援換行。分區設定與購票座位區保留區域內的橫向捲動。

`booking/SeatMap.vue` 在寬度不超過 600px 時將座位加大為 48 × 48px，座位間距為 12px，並增加排間距；快速找排獨立排列，座位圖可橫向捲動。座位大小由 `--seat-size` 統一控制按鈕寬度與網格欄寬。

## WebSocket 通知顯示時間

個人通知由 `src/services/websocket.js` 的 `showNotification()` 顯示，全部提供手動關閉按鈕。

| 通知 | 顯示時間 |
| --- | --- |
| `saveTicket` 建立訂單後的付款期限提示（標題以「新通知：請在 」開頭） | 30 秒 |
| `BookingPaymentScheduler` 的「付款即將到期」 | 30 秒 |
| `BookingPaymentScheduler` 的「付款期限已到」 | 30 秒 |
| 其他 `warning` 通知 | 8 秒 |
| 其他一般通知 | 5 秒 |

前端以通知標題辨識付款提醒；後端變更上述標題時，需同步更新前端判斷。這些時間只控制畫面提示的停留時間，不改變訂單付款期限或後端排程。帶有相同通知編號的訊息會去重，避免重複顯示。

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

## Docker

由專案根目錄執行下列指令，即可連同後端、PostgreSQL 與 Redis 一起啟動：

```powershell
docker compose up --build
```

先在根目錄依 `.env.example` 準備 `.env`。前端容器掛載原始碼並執行 Vite 開發伺服器，網址為 http://localhost:5173。

詳見[專案說明](../README.md)及[後端說明](../concert-ticket-backend/README.md)。

## 銷售儀表板與分區限購

後台頁籤依序為銷售儀表板、活動管理、建立場次、分區與限購、查看訂單。儀表板彙整已付款實付營收、售票率、可售庫存及訂單狀態，支援重新整理。

「分區與限購」選擇場次後，可編輯分區名稱、顏色、排別範圍、基準票價、票種價格比例、資格說明與每會員限購張數。畫面提供座位分區及各票種價格預覽，儲存後由 API 載入最新版本；已有保留或售出座位時禁止修改。可匯出目前設定 JSON。

會員訂票時可選票種、篩選分區、查看實際票價與剩餘可購額度，並更新座位狀態。建立訂單不傳入客戶端票價，最終計價與限購以後端為準。API 回傳 400 時顯示表單錯誤，不登出使用者。

正式版建置可執行：

```powershell
npm run build
```

2026-10-06 正式版建置通過。測試已還原為原有版本，保留 `activity.spec.js`、`member.spec.js`、`booking.spec.js` 與 `admin.spec.js`；後來新增的銷售設定／儀表板測試已移除，之後再補。新版功能的完整瀏覽器流程與真實資料庫併發仍待驗證。

## 活動刪除與場次建立

收藏以活動編號為單位，同一活動的不同售票狀態共用收藏結果。會員與管理員都可新增或取消自己的收藏；按鈕送出時顯示載入狀態，失敗時顯示原因，重新整理後從 API 還原已收藏狀態。「只看收藏」使用相同的活動編號篩選。

- 在「建立場次」選擇活動後填寫場次與開賣時間；不同活動可分別建立多個場次。新增送出的 `id` 固定為空字串，避免沿用先前編輯的場次編號；活動延期或狀態修改會保留原編號。
- 刪除尚無場次的活動成功後，更新活動列表並顯示「活動已刪除」。已有場次時，後端回傳 HTTP 409，保留資料並提示無法刪除的原因。
- 進入管理後台時建立或沿用 WebSocket 連線，刪除受阻的通知只傳給操作管理員。HTTP 回應提供相同通知作為備援，依通知編號避免重複顯示；通知文字以純文字呈現。
