# 演唱會訂票系統

本專案提供演唱會活動瀏覽、會員管理、訂票與管理員後台功能。系統由 Vue 3 前端、Spring Boot 後端、PostgreSQL 與 Redis 組成，可透過 Docker Compose 一次啟動。

## 功能

- 會員：註冊、登入、修改會員資料、登出。
- 活動：查看活動列表與活動詳情。
- 訂票：查詢活動與場次、建立訂單、付款、查看票券、取消訂單。
- 管理後台：查看活動／場次／售票資料，並可新增、修改、刪除活動及建立場次。

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

停止服務 + 清掉容器（加上 --rmi all 就連 image 也刪）
- docker compose down -v --rmi all
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
