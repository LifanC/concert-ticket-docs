# 演唱會訂票系統後端

後端為以 Java 21 與 Spring Boot 建置的 REST API，負責會員驗證、活動、訂票與管理員功能。

## 技術

- Java 21、Spring Boot 4.1.0、Spring Security、Spring Validation
- MyBatis 與 PostgreSQL
- Redis：Refresh Token 快取與 JWT 黑名單管理
- JWT：Access Token 與 Refresh Token 驗證機制
- Springdoc OpenAPI：API 文件與測試介面
- WebSocket、Log4j2

## API 與文件

本機預設服務埠為 `8080`，所有 API 皆以 `/api` 為 context path。

| 項目 | 路徑 |
| --- | --- |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api/v3/api-docs` |
| 會員 API | `/api/v1/login` |
| 活動 API | `/api/v1/activity` |
| 訂票 API | `/api/v1/booking` |
| 管理員 API | `/api/v1/admin` |

需登入的端點使用 `Authorization` request header 傳遞 JWT；管理員操作另需具備 `ADMIN_ITEM_IMPLEMENT` 權限。

### 主要端點

| 功能 | 方法與路徑 |
| --- | --- |
| 註冊／登入／驗證／登出 | `POST /api/v1/login/register`、`/login`、`/validate`、`/logout` |
| 修改會員資料 | `PUT /api/v1/login/saveProfile` |
| 取得活動 | `GET /api/v1/activity/selectAllActivities` |
| 建立訂單 | `POST /api/v1/booking/saveTicket` |
| 查詢我的票券 | `GET /api/v1/booking/selectOnlyTicket` |
| 付款／取消訂單 | `PUT /api/v1/booking/dopayprice`、`/cancelOrder` |
| 管理活動與場次 | `/api/v1/admin` 下的活動、場次與售票管理端點 |
| 收藏列表 | `GET /api/v1/activity/selectOnlyFavoriteActivities` |
| 新增／刪除收藏 | `POST /api/v1/activity/saveFavoriteActivity`、`DELETE /api/v1/activity/deleteFavoriteActivity` |
| 場次查詢 | `GET /api/v1/booking/selectOnlySession` |
| 座位／不可用座位 | `GET /api/v1/booking/selectOnlySeats`、`GET /api/v1/booking/selectOnlyUnavailableSeats` |

WebSocket 端點為 `/api/ws`，前端透過 STOMP `CONNECT` 的 `Authorization: Bearer <accessToken>` 標頭驗證，後端依 JWT 設定 Principal，並訂閱 `/user/queue/notifications` 個人通知佇列。

# JWT 登入驗證流程

## ① Login

使用者輸入帳號密碼，由 Frontend 傳送至 Backend。

```text
Frontend ── 帳號 / 密碼 ──> Backend
```

---

## ② Login 成功

Backend 驗證帳號密碼成功後，回傳：

- Access Token
- Refresh Token（透過 Set-Cookie 設定 HttpOnly、SameSite=Lax Cookie）

```text
Frontend <── Access Token ── Backend
         <── Refresh Token（HttpOnly Cookie） ─ Backend
```

---

## ③ 呼叫 API

Frontend 使用 **Access Token**，以 `Authorization: Bearer <accessToken>` 呼叫 Backend API。Redis 保存 Token 與相關狀態供驗證及失效處理。

```text
Frontend
    │
    │ Access Token
    ▼
Backend
    │
    │ 驗證 JWT
    ▼
驗證成功
    │
    ▼
回傳資料
```

---

## ④ Access Token 過期

當 Access Token 過期時，Frontend 攜帶 **Refresh Token Cookie** 向 `/api/v1/login/validate` 取得新的 Access Token。

```text
Frontend
    │
    │ Refresh Token Cookie
    ▼
POST /api/v1/login/validate
    │
    │ 驗證 Refresh Token
    ▼
Backend
    │
    ▼
產生 New Access Token
    │
    ▼
Frontend
```

---

## ⑤ Logout

使用者登出時，Refresh Token 被撤銷或刪除，並清除 Refresh Token Cookie 與相關 Redis 狀態。

```text
Frontend
    │
    │ Logout
    ▼
Backend
    │
    ▼
撤銷 / 刪除 Refresh Token
```

---

## 完整流程

```text
① Login
Frontend ── 帳號 / 密碼 ──> Backend

② Login 成功
Frontend <── Access Token
         <── Refresh Token（HttpOnly Cookie）

③ 呼叫 API
Frontend ── Access Token ──> Backend
                              │
                              ▼
                           驗證 JWT
                              │
                              ▼
                           回傳資料

④ Access Token 過期
Frontend ── Refresh Token Cookie ──> /api/v1/login/validate
                               │
                               ▼
                        驗證 Refresh Token
                               │
                               ▼
Frontend <── New Access Token

⑤ Logout
Frontend ── Logout ──> Backend
                        │
                        ▼
                撤銷 / 刪除 Refresh Token
```

## 功能範圍

- 會員：註冊、登入、驗證 Token、修改會員資料、登出。
- 活動：取得活動列表、查詢收藏、新增與刪除收藏。
- 訂票：查詢活動、場次、票券與票價；建立訂單、付款、取消訂單。
- 管理：查詢活動／場次／售票資料；儲存或刪除活動、建立場次。
- 自動分析：定時啟動 Python，匯出各場次已付款訂單數與金額 CSV。

## 本機執行

共用範本位於根目錄 [`.env.example`](../.env.example)。從後端目錄啟動時，可將範本複製至 `concert-ticket-backend/.env`，填入實際設定；若已有 `.env`，請只合併缺少的參數：

```properties
POSTGRES_PASSWORD=your_password_here
REDIS_PASSWORD=your_password_here
REFRESH_COOKIE_SECURE=false
```

需要 JDK 21、PostgreSQL 16 與 Redis 7；啟用自動分析另需下方的 Python 環境。若使用 Docker 提供資料庫與 Redis，需在根目錄 `.env` 設定相同密碼，並從根目錄執行 `docker compose up -d db redis`。接著於後端目錄執行：

```powershell
.\mvnw.cmd spring-boot:run
```

Spring 預設讀取啟動工作目錄的 `.env`，因此上述方式讀取後端 `.env`。若希望改讀根目錄 `.env`，可在同一終端機啟動前設定 `$env:SPRING_CONFIG_IMPORT = 'optional:file:../.env[.properties]'`；此覆寫仍存在時，修改後端 `.env` 不會生效。IDE 的工作目錄也會影響讀取位置。

預設本機連線設定：

- PostgreSQL：`jdbc:postgresql://localhost:5432/interviewworks?currentSchema=interviewworks_ticket`
- Redis：`localhost:6379`

PostgreSQL 帳號為 `postgres`；`PORT` 可覆寫預設 HTTP 埠號 8080。

## Docker

建議從專案根目錄使用 Docker Compose 啟動所有服務：

```powershell
docker compose up --build
```

Docker profile 會將 PostgreSQL 與 Redis 主機分別連至 Compose 服務 `db`、`redis`，由 Compose 注入根目錄 `.env`。

後端映像包含 Python 與分析依賴，Compose 將報表目錄掛載至本機 `concert-ticket-analytics/reports/`。單獨建置後端映像也須使用專案根目錄作為建置 context：`docker build -f concert-ticket-backend/Dockerfile .`。

## Python 自動銷售分析

`SalesAnalyticsScheduler` 在 Java 啟動完成後，透過 `ProcessBuilder` 呼叫 `analyze.py`。它將 datasource 的主機、連接埠、資料庫、帳號與密碼傳給 Python，自動執行時不必另設分析用 `.env`。每次分析結束後預設等待 30 分鐘，再執行下一次。

本機第一次使用需安裝 Python 3，並在專案根目錄建立分析虛擬環境及安裝依賴：

```powershell
py -m venv concert-ticket-analytics/.venv
.\concert-ticket-analytics\.venv\Scripts\python.exe -m pip install -r concert-ticket-analytics/requirements.txt
```

完成後正常啟動 Java 即可自動分析。Docker 建置會完成 Python 環境安裝，不需在主機另做這兩步。

| 參數 | 預設值 | 用途 |
| --- | --- | --- |
| ANALYTICS_ENABLED | true | 啟用自動分析；false 關閉 |
| ANALYTICS_DELAY_MS | 1800000 | 每次完成後等待 30 分鐘，單位為毫秒 |
| ANALYTICS_TIMEOUT_SECONDS | 120 | 單次執行上限，單位為秒 |
| ANALYTICS_DIRECTORY | 空字串 | 從根目錄或後端目錄自動尋找分析資料夾 |
| ANALYTICS_PYTHON | 空字串 | 使用分析資料夾 `.venv` 中的 Python |

上述參數可放在 Java 實際讀取的 `.env`，未填時使用 YAML 預設值，修改後重啟生效。從其他目錄啟動時，可指定分析資料夾與 Python 執行檔的絕對路徑；Windows 路徑使用 `/`。Docker Compose 會提供容器內的路徑，覆寫 `.env` 中這兩個路徑參數。

排程使用獨立執行緒，不佔用訂單到期排程。執行失敗時寫入 Java 日誌，下一次排程重試；超過預設 120 秒會終止 Python，Java 關閉時也會停止排程與執行中的程序。Python 最新輸出在 `concert-ticket-analytics/reports/analytics-latest.log`，CSV 每次產生新檔，未設定自動清理。詳細統計規則見[Python 分析功能](../concert-ticket-analytics/README.md)。

## 跨來源設定

目前 CORS 僅允許 `http://localhost:5173`，並允許 `GET`、`POST`、`PUT`、`DELETE` 與 `OPTIONS` 方法。若前端改以其他網域或埠號執行，需同步調整 `security/SecurityConfig.java`。

REST CORS 允許 credentials；Cookie 的 Secure 屬性由 `REFRESH_COOKIE_SECURE` 控制。訂票與新增／刪除收藏需要 `USER_ITEM_IMPLEMENT`。Security 放行登入路徑與活動／收藏列表，個別服務仍依使用者身分處理請求。Axios 收到 401 時會攜帶 Cookie 更新 Access Token 並重試一次，失敗後返回會員頁。

## 訂票一致性與逾期處理

- 建立訂單必須帶 `Idempotency-Key`（1～128 個英數字、`_` 或 `-`）。同會員、同 key、同訂位內容重試回傳首次成功 JSON；不同訂位內容回傳 409。
- 活動、日期、時間與價格由資料庫決定；不可用座位依 `session_id` 查詢。
- 訂單、場次庫存及座位保留／釋放在同一交易完成；同場次先鎖場次，再處理訂單與座位。
- 狀態包含 `PENDING_PAYMENT`、`PAID`、`CANCELLED`、`EXPIRED`，付款、取消與逾期由 `BookingOrderService` 集中處理。
- 除每筆訂單的記憶體到期任務外，`BookingExpirationRecovery` 預設以 30 秒固定延遲掃描，每批最多 100 筆，逐筆交易補償逾期訂單；可透過 `booking.expiration.scan-delay-ms` 調整延遲。
- 補償掃描只恢復狀態、座位與庫存，不補送 WebSocket 通知。付款尚未串接外部金流，退款尚未實作。

回到[專案說明](../README.md)。
