# 演唱會訂票系統

本專案提供演唱會活動瀏覽、會員管理、訂票與管理員後台功能。系統由 Vue 3 前端、Spring Boot 後端、PostgreSQL 與 Redis 組成，並由 Java 定時呼叫 Python 產生銷售報表，可透過 Docker Compose 一次啟動。

**線上演唱會售票系統｜全端個人專案**

使用 Vue 3 與 Spring Boot 開發演唱會售票系統，整合會員驗證、活動瀏覽、場次選座、訂單管理與管理員後台，並提供自動化測試及 Python 銷售報表。

1. 使用 MyBatis 與 PostgreSQL 交易、列鎖及唯一約束保護座位占用，同步處理訂單、座位與庫存異動；建立訂單支援 `Idempotency-Key`，讓相同訂位請求可安全重試。
2. 實作 10 分鐘付款期限、訂單到期排程與定期補償掃描，處理付款、取消及逾期狀態轉換，並補償服務重啟後遺留的逾期訂單。
3. 整合 Spring Security、JWT Access Token／Refresh Token 與 Redis，實作會員及管理員權限、Token 更新與登出失效機制；使用 STOMP WebSocket 推送個人訂單通知。
4. 以 Docker Compose 整合前端、後端、PostgreSQL 與 Redis，並由 Java 定時執行 Python，匯出各場次已付款訂單數與金額的 CSV 報表。
5. 建立訂單狀態轉換、Spring 啟動與角色權限、Playwright 瀏覽器流程及 Python 報表測試，於測試紀錄列出結果與驗證限制。

目前付款為系統內訂單狀態操作；外部金流、退款及完整高併發驗收仍待完成，詳見[測試紀錄](TEST_REPORT.md)與[功能規劃](FEATURES_ROADMAP.md)。

本專案採用 OpenAI Codex 協作開發，協助需求拆解、架構討論、程式碼撰寫與重構、問題排查及文件整理；由本人進行需求確認、技術方案取捨、程式碼檢視、系統整合與測試驗證。

## 功能概覽

- 會員：註冊、登入、修改會員資料、登出。
- 活動：查看活動列表與活動詳情、搜尋篩選、加入／取消收藏及只看收藏。
- 訂票：查詢活動與場次、選擇可用座位、建立訂單、付款、查看票券、取消訂單。
- 管理後台：查看活動／場次／售票資料，並可新增、修改、刪除活動及建立場次。
- 銷售分析：Java 啟動後自動執行 Python，預設每次完成後等待 30 分鐘，產生各場次已付款訂單數與金額的 CSV 報表。

## 功能解析

### 前台使用者

- 公開瀏覽全部活動。
- 會員註冊、登入、Token 驗證、個人資料修改及登出。
- 依活動查詢場次、票券、價格及開賣日期。
- 選擇日期、場次與座位；不可用座位依場次查詢，避免不同場次互相影響。
- 建立票券訂單、查詢個人票券、付款及取消訂單。
- 接收個人 WebSocket 訂單通知。

### 管理後台

- 查詢活動、場次及票券。
- 新增、修改或刪除活動。
- 建立活動場次。
- 管理 API 僅允許具 `ADMIN_ITEM_IMPLEMENT` 權限的使用者存取。

### 驗證與授權

- Spring Security 在 Controller 前透過自訂 JWT Filter 驗證請求。
- 登入與活動查詢為公開 API；訂票 API 需要一般會員權限；管理 API 需要管理員權限。
- 使用 Access Token 與 Refresh Token，並以 Redis 保存及檢查 Token 狀態，支援登出失效。
- 前端 Axios 統一附帶 Bearer Access Token；收到 401 時透過 `POST /api/v1/login/validate` 與 HttpOnly Refresh Token Cookie 更新 Token，失敗後導回會員頁。

### 訂單狀態與逾時處理

- 建立訂單時產生訂單編號、10 分鐘付款期限與 `PENDING_PAYMENT` 狀態。
- 使用 `TaskScheduler` 依每筆訂單到期時間建立排程。
- 到期時再次確認狀態；仍未付款才更新為 `EXPIRED`，避免覆蓋已付款或已取消訂單。
- 付款或取消後撤銷記憶體中的到期任務。
- 主要票券狀態包含 `PENDING_PAYMENT`、`PAID`、`CANCELLED`、`EXPIRED`。
- 另以固定延遲掃描逾期訂單（預設 30 秒，每批最多 100 筆），補償重啟或排程遺漏；補償掃描不補送 WebSocket 通知。
- 座位保留、訂單及庫存異動在同一資料庫交易完成，並以有效訂單座位唯一索引避免重複占用。
- 建立訂單必須帶 `Idempotency-Key`；同會員以相同 key 重試相同訂位會取得首次成功結果，不同訂位內容則回傳 409「衝突」（Conflict）。
- 付款目前為系統內訂單狀態操作，尚未串接外部金流或實作退款流程。

### 即時通知

- 後端採用 Spring WebSocket 與 STOMP。
- 透過 `convertAndSendToUser` 發送至 `/user/queue/notifications` 類型的個人佇列。
- 前端使用 `@stomp/stompjs` 與 SockJS 建立連線並訂閱個人通知。
- STOMP `CONNECT` 標頭攜帶 `Authorization: Bearer <accessToken>`，後端驗證後設定使用者身分。

### 資料與部署

- PostgreSQL 儲存權限、會員、活動、場次及票券資料。
- MyBatis Mapper／XML 負責 SQL 與物件映射。
- Redis 用於 Token 狀態管理。
- Docker Compose 編排 Vue、Spring Boot、PostgreSQL、Redis 四項服務；資料庫設有健康檢查與持久化 volume。
- Swagger／OpenAPI 提供 API 文件與測試介面。

### 主要 API 分組

以下路徑皆相對於後端 `/api`；例如活動列表的完整路徑為 `/api/v1/activity/selectAllActivities`。

| 模組 | 代表 API | 用途 |
| --- | --- | --- |
| Activity | `GET /v1/activity/selectAllActivities` | 公開活動列表 |
| 收藏 | `/v1/activity/selectOnlyFavoriteActivities`、`saveFavoriteActivity`、`deleteFavoriteActivity` | 查詢、新增與刪除收藏 |
| Login | `/v1/login/register`、`login`、`validate`、`saveProfile`、`logout` | 帳號與 Token 流程 |
| Booking | 查詢活動／場次／票券、`saveTicket`、`cancelOrder`、`dopayprice` | 會員訂票流程 |
| 座位 | `GET /v1/booking/selectOnlySeats`、`GET /v1/booking/selectOnlyUnavailableSeats` | 場次座位與不可用座位查詢 |
| Admin | 查詢活動／場次／票券、`saveActivity`、`deleteActivity`、`createSession` | 後台管理 |

### 架構流程

```text
Vue 3 / Element Plus
        │ Axios REST + STOMP WebSocket
        ▼
JWT Filter ── Spring Security 角色授權
        │
Spring Boot Controller
        │
Service（交易、狀態判斷、排程、通知）
   ┌────┴────────┐
   ▼             ▼
MyBatis       Redis
   │          Token 狀態
   ▼
PostgreSQL
```

銷售分析由後端的獨立排程執行：

```text
Java SalesAnalyticsScheduler → Python analyze.py → 唯讀查詢 PostgreSQL
                                      │
                                      ▼
                              reports/ 銷售 CSV
```

Python 與 Java 共用資料庫連線設定；Docker 中的 Python 位於後端容器，不另建服務。報表目前以 CSV 提供，尚未顯示於 Vue 後台。

## 技術棧

| 類別 | 技術 |
| --- | --- |
| 前端 | Vue 3、Vite、Element Plus 2.14、Axios |
| 後端 | Java 21、Spring Boot、Spring Security、MyBatis |
| 銷售分析 | Python、pg8000、python-dotenv |
| 資料儲存 | PostgreSQL 16、Redis 7 |
| 驗證與文件 | JWT、Springdoc OpenAPI / Swagger UI |
| 容器化 | Docker、Docker Compose |
| 測試 | JUnit、Mockito、Playwright（Microsoft Edge）、Python unittest |

## 專案結構

```text
concert-ticket-docs/
├── concert-ticket-backend/    # Spring Boot API
├── concert-ticket-frontend/   # Vue 3 網站
├── concert-ticket-analytics/  # Python 銷售分析與 CSV 報表
├── db-init/                   # PostgreSQL 初始化 SQL
├── .env.example               # 前端、後端與分析共用範本
├── TEST_REPORT.md             # 測試結果、重跑方式與驗證限制
├── FEATURES_ROADMAP.md        # 功能規劃與待完成項目
└── docker-compose.yml
```

## 快速啟動

環境變數範本統一放在根目錄 `.env.example`，各程式仍依自己的位置讀取設定。Docker Compose 使用根目錄 `.env`；本機 Java 預設讀取啟動工作目錄的 `.env`；Vue 使用前端資料夾的 `.env` 或 `.env.local`，僅需其中的 `VITE_*`。本機 Python 環境準備與排程設定見[後端說明](concert-ticket-backend/README.md#python-自動銷售分析)。

先安裝並啟動 Docker Engine／Docker Desktop 與 Docker Compose，以下指令在專案根目錄執行。前端容器使用 Vite 開發伺服器。

1. 複製環境變數範本為 `.env`，並設定安全的密碼。已有 `.env` 時只合併缺少的參數，避免覆蓋原設定。

   ```powershell
   Copy-Item .env.example .env
   ```

2. 啟動所有服務。

   ```powershell
   docker compose up --build
   ```

3. 以背景模式執行時，改用：

   ```powershell
   docker compose up --build -d
   ```

首次啟動時，PostgreSQL 會執行 `db-init/` 下的初始化 SQL。若曾以相同資料卷啟動過，初始化 SQL 不會再次自動執行；需要重新初始化資料時，請先確認不再需要現有資料，再執行 `docker compose down -v`。

服務啟動後可使用：

| 服務 | 網址／連線資訊 |
| --- | --- |
| 前端 | http://localhost:5173 |
| 後端 API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/v3/api-docs |
| PostgreSQL | `localhost:5432`，資料庫 `interviewworks` |
| Redis | `localhost:6379` |

## 本機開發

需要 JDK 21、Node.js 20.19.0 以上與 npm；啟用 Python 分析時另需 Python 3。以下方式使用 Docker 提供 PostgreSQL 與 Redis，前後端則在本機執行。

1. 依快速啟動準備根目錄 `.env`，從專案根目錄啟動資料庫與快取：

   ```powershell
   docker compose up -d db redis
   ```

2. 在 `concert-ticket-backend/.env` 設定與根目錄相同的 `POSTGRES_PASSWORD`、`REDIS_PASSWORD`，以及 `REFRESH_COOKIE_SECURE=false`。自動分析預設開啟，請依[後端 Python 環境說明](concert-ticket-backend/README.md#python-自動銷售分析)安裝依賴；暫時不使用分析時設定 `ANALYTICS_ENABLED=false`。在後端目錄執行：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. 在 `concert-ticket-frontend/.env.local` 放入根目錄範本中的兩個 `VITE_*` 參數，另開終端機於前端目錄執行：

   ```powershell
   npm ci
   npm run dev
   ```

開啟 http://localhost:5173 即可使用。根目錄 `.env` 不會自動套用至 Vue；前端設定修改後需重啟 Vite。後端 CORS 目前允許 `http://localhost:5173`，更換前端網域或埠號時需同步調整 `SecurityConfig.java`。

## 銷售報表與執行紀錄

Docker Compose 將報表掛載至本機 `concert-ticket-analytics/reports/`，可直接開啟 CSV；最新一次 Python 執行輸出位於該目錄的 `analytics-latest.log`。CSV 檔名含 UTC 時間，採 UTF-8 BOM 編碼供 Excel 開啟，每次產生新檔，未設定自動清理。

報表依場次統計目前 `PAID` 訂單數與 `payprice` 加總，無已付款訂單的場次顯示 0。排程開關、間隔與逾時分別由 `ANALYTICS_ENABLED`、`ANALYTICS_DELAY_MS`、`ANALYTICS_TIMEOUT_SECONDS` 控制，預設為 `true`、`1800000` 毫秒與 `120` 秒；修改後需重新啟動後端，Docker 環境可執行 `docker compose up -d app` 套用設定。

## 測試與建置

後端測試在 `concert-ticket-backend` 執行，需要 Java 21 與後端環境設定：

```powershell
.\mvnw.cmd clean test
```

前端先啟動前後端服務並安裝 Microsoft Edge，再於 `concert-ticket-frontend` 執行：

```powershell
npm ci
npm run test:e2e
# 以可見瀏覽器視窗執行
npm run test:e2e:headed
npm run build
```

前端建置產物位於 `dist/`，Playwright HTML 報告位於 `playwright-report/index.html`。真實帳號測試需提供 `E2E_ADMIN_ACCOUNT`／`E2E_ADMIN_PASSWORD` 或 `E2E_MEMBER_ACCOUNT`／`E2E_MEMBER_PASSWORD`，未提供時跳過對應案例。訂票案例預設停在確認視窗；設定 `E2E_CREATE_ORDER=1` 才會送出並保留真實待付款訂單，詳細條件見[測試紀錄](TEST_REPORT.md#重跑)。

Python 測試需先完成分析虛擬環境安裝，再於 `concert-ticket-analytics` 執行：

```powershell
.\.venv\Scripts\python.exe -m unittest -v test_analyze
```

2026-09-10 的既有紀錄包含 Java 18 項、Edge 9 項與 Python 6 項測試通過，以及前端建置成功。Java 訂單測試使用 mock Mapper，Python 聚合測試使用 SQLite；這些結果不代表已驗證真實 PostgreSQL 併發、交易回滾或完整付款流程。實際驗證範圍與限制以 [TEST_REPORT.md](TEST_REPORT.md) 為準。

## 後續規劃

待完成項目包含完整購票與付款 E2E、真實資料庫併發與重啟補償驗證、CI、後台銷售儀表板、主辦方權限、QR Code 驗票、外部金流與退款，以及負載／壓力測試。詳細規劃見 [FEATURES_ROADMAP.md](FEATURES_ROADMAP.md)；其中歷史完成標記需搭配測試紀錄判讀。

## 常用 Docker 指令

```powershell
# 查看服務狀態
docker compose ps

# 查看並持續追蹤日誌
docker compose logs -f

# 停止服務（保留資料庫資料卷）
docker compose down

# 停止服務並移除資料庫資料卷；此操作會刪除本機資料
docker compose down -v
```

若要同時移除容器、資料卷與映像檔，可使用：

```powershell
docker compose down -v --rmi all
```

## 資料庫與快取檢查

```powershell
# 進入 PostgreSQL（容器名稱預設為 postgres-container）
docker exec -it postgres-container psql -U postgres -d interviewworks
```

進入 PostgreSQL 後可使用：

```sql
SET search_path TO interviewworks_ticket;
\dt
```

```powershell
# 進入 Redis；請依 .env 的 REDIS_PASSWORD 取代 <password>
docker exec -it redis-container redis-cli -a <password>
```

## 延伸文件

- [測試紀錄與重跑方式](TEST_REPORT.md)
- [功能規劃與待辦](FEATURES_ROADMAP.md)
- [後端說明](concert-ticket-backend/README.md)
- [前端說明](concert-ticket-frontend/README.md)
- [資料庫初始化與訂票一致性](db-init/README.md)
- [Python 銷售分析：Java 啟動後自動產生 CSV 報表](concert-ticket-analytics/README.md)
