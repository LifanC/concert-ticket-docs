# 資料庫初始化與本批升級

沿用原本 Docker Compose 的 `db-init` 掛載方式，沒有引入 Flyway，也沒有更換 Spring Boot／MyBatis 版本。

## 新資料庫

PostgreSQL 第一次建立資料目錄時執行 `init.sql`，在單一交易中建立所有資料表、場次座位、有效訂單座位唯一索引、庫存約束及訂單冪等資料表。

金額欄位直接使用 `numeric(12,2)`；`expires_at`、`paid_at`、`cancelled_at` 直接使用 `timestamptz`。活動與場次日期仍為字串。

## 既有資料庫

Docker 不會在已有資料的 volume 上重跑初始化 SQL。`init.sql` 用於新資料庫初始化，不是既有資料庫的升級腳本；既有環境需另行準備對應目前 schema 的遷移，包含欄位轉型、約束、索引及既有座位資料回填。原先無時區的訂單時間應按 `Asia/Taipei` 解讀。

## 本批行為

- 座位以 `(session_id, seat_id)` 唯一識別，保留、售出與釋放和訂單／庫存在同一交易。
- 場次在訂位時依現有座位配置建立指定座位。
- 同場次交易先鎖場次，再處理訂單與座位，避免不同操作產生反向鎖定。不同座位可接受並發請求，但同場次的庫存異動仍短暫序列化。
- 每 30 秒掃描最多 100 筆逾期待付款訂單；每筆獨立交易，多實例由條件更新防止重複釋放。不依賴記憶體排程；補償掃描目前只恢復狀態與庫存，不補送 WebSocket 通知。
- `POST /api/v1/booking/saveTicket` 必須帶 `Idempotency-Key`（1～128 個英數字、`_` 或 `-`）。同會員、同 key、同場次／活動／座位回傳首次成功的 JSON；不同訂位內容回傳 409。名稱、日期、時間及價格由資料庫決定，前端快照不參與請求雜湊。
- 前端在同次訂位重試時保留 key，成功後清除；重整頁面不保留 key。後端仍以座位唯一索引防止重複占用。
- 不可用座位查詢改傳 `session_id`，前端已同步調整。
- 付款、取消、逾期集中在 `BookingOrderService`；退款實際流程仍未實作。

## 重跑驗證

在專案根目錄建立獨立測試資料庫：

```powershell
docker run --rm -d --name booking-core-test -e POSTGRES_USER=booking_test -e POSTGRES_PASSWORD=booking_test -e POSTGRES_DB=booking_transaction_test -p 127.0.0.1::5432 postgres:16-alpine -c max_connections=200
docker port booking-core-test 5432
```

等待 PostgreSQL 就緒，在 `concert-ticket-backend` 目錄執行（將 `<port>` 換成輸出的連接埠）：

```powershell
$env:BOOKING_TEST_JDBC_URL = 'jdbc:postgresql://127.0.0.1:<port>/booking_transaction_test'
.\mvnw.cmd '-Dtest=BookingTransactionTest' test
docker stop booking-core-test
Remove-Item Env:BOOKING_TEST_JDBC_URL
```

測試程式目前已整段註解，上述指令需恢復啟用後才會執行測試。測試只載入本目錄的 `init.sql`，另建立測試訂單與對應保留座位，使用正式 MyBatis 與 Spring 交易代理。涵蓋回滾、所有權、100 請求同座位只有一筆成功、不同座位並發、重送與並發相同 key、付款／逾期競爭、無記憶體排程補償，以及資料庫拒絕非法庫存。測試會重建指定測試資料庫中的 schema；未提供環境變數時跳過，不能視為通過。Redis、通知與即時排程以 mock 取代，尚未涵蓋完整 HTTP／Security／E2E 流程或真正重啟 JVM。
