# 演唱會訂票系統

本專案提供演唱會活動瀏覽、會員管理、訂票與管理員後台功能。系統由 Vue 3 前端、Spring Boot 後端、PostgreSQL 與 Redis 組成，可透過 Docker Compose 一次啟動。

**線上演唱會售票系統｜全端個人專案**
1. 使用 Vue 3、Spring Boot、PostgreSQL 與 Redis 建置前後端分離售票平台，完成活動瀏覽、會員註冊登入、場次選擇、訂票、付款、取消訂單及後台活動管理流程。
2. 以 Spring Security 搭配 JWT Access Token／Refresh Token 實作身分驗證與角色授權，區分一般會員及管理員 API 權限，並透過 Redis 管理 Token 狀態與登出失效機制。
3. 設計訂單付款期限排程：訂票後建立到期任務，逾時自動將待付款訂單更新為 `EXPIRED`，付款或取消時同步撤銷排程，維持票券狀態一致性。
4. 導入 STOMP WebSocket 個人訊息佇列，將付款逾時、付款完成與取消等訂單事件即時推送給指定使用者。
5. 使用 MyBatis 處理 PostgreSQL 資料存取與交易流程，並以 Docker Compose 整合前端、後端、PostgreSQL、Redis 四項服務，降低本機建置成本。
6. 使用 OpenAI Codex 協助需求拆解、程式碼重構、問題排查與文件整理，並由本人負責架構設計、功能實作及成果驗證。

## 功能概覽

- 會員：註冊、登入、修改會員資料、登出。
- 活動：查看活動列表與活動詳情。
- 訂票：查詢活動與場次、建立訂單、付款、查看票券、取消訂單。
- 管理後台：查看活動／場次／售票資料，並可新增、修改、刪除活動及建立場次。

## 功能解析

### 前台使用者

- 公開瀏覽全部活動。
- 會員註冊、登入、Token 驗證、個人資料修改及登出。
- 依活動查詢場次、票券、價格及開賣日期。
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
- 前端 Axios 統一附帶 Token，遇到未授權狀態會導回會員頁。

### 訂單狀態與逾時處理

- 建立訂單時產生訂單編號、付款期限與 `PENDING_PAYMENT` 狀態。
- 使用 `TaskScheduler` 依每筆訂單到期時間建立排程。
- 到期時再次確認狀態；仍未付款才更新為 `EXPIRED`，避免覆蓋已付款或已取消訂單。
- 付款或取消後撤銷記憶體中的到期任務。
- 主要票券狀態包含 `PENDING_PAYMENT`、`PAID`、`CANCELLED`、`EXPIRED`。

### 即時通知

- 後端採用 Spring WebSocket 與 STOMP。
- 透過 `convertAndSendToUser` 發送至 `/user/queue/notifications` 類型的個人佇列。
- 前端使用 `@stomp/stompjs` 與 SockJS 建立連線並訂閱個人通知。

### 資料與部署

- PostgreSQL 儲存權限、會員、活動、場次及票券資料。
- MyBatis Mapper／XML 負責 SQL 與物件映射。
- Redis 用於 Token 狀態管理。
- Docker Compose 編排 Vue、Spring Boot、PostgreSQL、Redis 四項服務；資料庫設有健康檢查與持久化 volume。
- Swagger／OpenAPI 提供 API 文件與測試介面。

### 主要 API 分組

| 模組 | 代表 API | 用途 |
| --- | --- | --- |
| Activity | `GET /v1/activity/selectAllActivities` | 公開活動列表 |
| Login | `/v1/login/register`、`login`、`validate`、`saveProfile`、`logout` | 帳號與 Token 流程 |
| Booking | 查詢活動／場次／票券、`saveTicket`、`cancelOrder`、`dopayprice` | 會員訂票流程 |
| Admin | 查詢活動／場次／票券、`saveActivity`、`deleteActivity`、`createSession` | 後台管理 |

### 架構流程

```text
Vue 3 / Element Plus
        │ Axios REST + STOMP WebSocket
        ▼
Spring Boot Controller
        │
JWT Filter ── Spring Security 角色授權
        │
Service（交易、狀態判斷、排程、通知）
   ┌────┴────────┐
   ▼             ▼
MyBatis       Redis
   │          Token 狀態
   ▼
PostgreSQL
```

## 技術棧

| 類別 | 技術 |
| --- | --- |
| 前端 | Vue 3、Vite、Element Plus 2.14、Axios |
| 後端 | Java 21、Spring Boot、Spring Security、MyBatis |
| 資料儲存 | PostgreSQL 16、Redis 7 |
| 驗證與文件 | JWT、Springdoc OpenAPI / Swagger UI |
| 容器化 | Docker、Docker Compose |

## 專案結構

```text
concert-ticket-docs/
├── concert-ticket-backend/    # Spring Boot API
├── concert-ticket-frontend/   # Vue 3 網站
├── db-init/                   # PostgreSQL 初始化 SQL
├── .env.example               # 環境變數範本
└── docker-compose.yml
```

## 快速啟動

1. 複製環境變數範本為 `.env`，並設定安全的密碼。

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

- [後端說明](concert-ticket-backend/README.md)
- [前端說明](concert-ticket-frontend/README.md)
