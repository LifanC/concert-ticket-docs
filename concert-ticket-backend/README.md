# 演唱會訂票系統後端

後端為以 Java 21 與 Spring Boot 建置的 REST API，負責會員驗證、活動、訂票與管理員功能。

## 技術

- Java 21、Spring Boot、Spring Security、Spring Validation
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

WebSocket 端點為 `/api/ws`，前端會以 `username` query parameter 建立連線，並訂閱個人通知佇列。

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
- Refresh Token

```text
Frontend <── Access Token ── Backend
         <── Refresh Token ─ Backend
```

---

## ③ 呼叫 API

Frontend 使用 **Access Token** 呼叫 Backend API。

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

當 Access Token 過期時，Frontend 使用 **Refresh Token** 向 `/auth/refresh` 取得新的 Access Token。

```text
Frontend
    │
    │ Refresh Token
    ▼
POST /auth/refresh
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

使用者登出時，Refresh Token 被撤銷或刪除。

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
         <── Refresh Token

③ 呼叫 API
Frontend ── Access Token ──> Backend
                              │
                              ▼
                           驗證 JWT
                              │
                              ▼
                           回傳資料

④ Access Token 過期
Frontend ── Refresh Token ──> /auth/refresh
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
- 活動：取得活動列表。
- 訂票：查詢活動、場次、票券與票價；建立訂單、付款、取消訂單。
- 管理：查詢活動／場次／售票資料；儲存或刪除活動、建立場次。

## 本機執行

請先在專案根目錄建立 `.env`，並設定：

```properties
POSTGRES_PASSWORD=your_password_here
REDIS_PASSWORD=your_password_here
```

啟動 PostgreSQL 與 Redis 後，在本目錄執行：

```powershell
mvn spring-boot:run
```

預設本機連線設定：

- PostgreSQL：`jdbc:postgresql://localhost:5432/interviewworks?currentSchema=interviewworks_ticket`
- Redis：`localhost:6379`

## Docker

建議從專案根目錄使用 Docker Compose 啟動所有服務：

```powershell
docker compose up --build
```

Docker profile 會將 PostgreSQL 與 Redis 主機分別連至 Compose 服務 `db`、`redis`。

## 跨來源設定

目前 CORS 僅允許 `http://localhost:5173`，並允許 `GET`、`POST`、`PUT`、`DELETE` 與 `OPTIONS` 方法。若前端改以其他網域或埠號執行，需同步調整 `security/SecurityConfig.java`。

## 延伸文件

- [後端說明](concert-ticket-backend/README.md)