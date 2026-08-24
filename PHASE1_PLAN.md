# 第一階段（一）：庫存鎖定與防止超賣

## 文件用途

這份文件用來記錄售票系統「庫存鎖定與防止超賣」的設計與執行順序，方便下次接續討論或實作。

目前僅完成規劃，尚未依本文件修改程式。

## 目前狀況

目前系統的訂票流程為：

1. 前端產生訂單編號。
2. 後端新增狀態為「待付款」的訂單。
3. 建立訂單時不會保留庫存。
4. 使用者付款時才將場次 `capacity` 減 1，並將 `sold` 加 1。
5. 使用者取消訂單時只修改訂單狀態，沒有庫存保留或釋放流程。
6. 待付款訂單沒有自動過期機制。

相關程式位置：

- `concert-ticket-backend/src/main/java/com/demo/ticket/Service/BookingServiceImpl.java`
- `concert-ticket-backend/src/main/resources/Mapper/BookingMapper.xml`
- `concert-ticket-frontend/src/components/Booking.vue`
- `db-init/init.sql`

## 目前可能發生的問題

- 多位使用者可以同時建立超過剩餘票數的待付款訂單。
- 使用者建立訂單後，無法確定該張票是否真的被保留。
- 最後付款的人才可能發現庫存不足。
- 待付款訂單會永久留在系統中。
- 若直接加入庫存歸還，但沒有條件式更新，可能重複歸還庫存。
- 若 Redis 和 PostgreSQL 都各自保存一份庫存，容易發生資料不一致。

## 目標流程

```text
建立訂單
   ↓
原子保留庫存
   ↓
建立 PENDING_PAYMENT 訂單
   ↓
設定 10 分鐘付款期限
   ↓
├─ 期限內付款 → PAID
├─ 使用者取消 → CANCELLED，釋放庫存
└─ 超過期限 → EXPIRED，釋放庫存
```

基本原則：

> 庫存只在建立訂單時保留一次，也只在取消或過期時釋放一次。

## 核心設計決策

### PostgreSQL 是庫存的唯一真實來源

庫存資料應以 PostgreSQL 為準。建立訂單時使用條件式 `UPDATE` 原子保留庫存，不需要先使用 Redis 分散式鎖。

Redis 在此階段只負責：

- 保存待付款訂單的到期索引。
- 協助排程快速找到需要過期的訂單。
- 視需要發布即時庫存通知。

即使 Redis 資料遺失，仍能從 PostgreSQL 找回過期訂單。

### 庫存欄位定義

建議將場次庫存拆分為：

| 欄位 | 說明 |
| --- | --- |
| `capacity` | 場次總票數 |
| `reserved` | 已建立訂單但尚未付款的保留數量 |
| `sold` | 已付款數量 |

可售數量的計算公式：

```text
available = capacity - reserved - sold
```

任何時候都必須符合：

```text
reserved >= 0
sold >= 0
reserved + sold <= capacity
```

## 建議資料庫調整

### `session` 資料表

建議增加：

```text
reserved bigint NOT NULL DEFAULT 0
```

並考慮加入資料庫約束：

```text
capacity >= 0
reserved >= 0
sold >= 0
reserved + sold <= capacity
```

### `ticket` 資料表

建議增加：

| 欄位 | 說明 |
| --- | --- |
| `session_id` | 對應 `session.id` 的外鍵 |
| `quantity` | 購票張數，初期可固定為 1 |
| `expires_at` | 付款到期時間 |
| `paid_at` | 實際付款時間 |
| `cancelled_at` | 取消時間 |

應使用 `session_id` 識別場次，不應繼續使用活動名稱、日期和時間三個字串拼湊關聯。

### 訂單狀態

資料庫建議保存固定狀態代碼：

| 狀態 | 說明 |
| --- | --- |
| `PENDING_PAYMENT` | 等待付款 |
| `PAID` | 已付款 |
| `CANCELLED` | 已取消 |
| `EXPIRED` | 付款逾期 |
| `REFUNDED` | 已退款 |

前端再將狀態代碼轉換成中文顯示文字。

## 實作順序

### 步驟 1：定義狀態與庫存規則

- 建立後端訂單狀態 Enum。
- 決定付款期限，初期建議 10 分鐘。
- 確認一張訂單是否支援多張票；若暫時不支援，`quantity` 固定為 1。
- 定義哪些狀態可以付款、取消及退款。

狀態轉換規則：

```text
PENDING_PAYMENT → PAID
PENDING_PAYMENT → CANCELLED
PENDING_PAYMENT → EXPIRED
PAID → REFUNDED（未來功能）
```

其他狀態轉換一律拒絕。

### 步驟 2：建立資料庫 Migration

- 為 `session` 增加 `reserved`。
- 為 `ticket` 增加 `session_id`、`quantity`、`expires_at` 等欄位。
- 建立 `ticket.session_id → session.id` 外鍵。
- 為過期訂單查詢建立索引，例如 `(status, expires_at)`。
- 為場次關聯與訂單查詢建立必要索引。
- 建議引入 Flyway，避免只修改 `db-init/init.sql`。

### 步驟 3：建立原子庫存保留 SQL

概念 SQL：

```sql
UPDATE interviewworks_ticket.session
SET reserved = reserved + :quantity
WHERE id = :sessionId
  AND capacity - reserved - sold >= :quantity;
```

判斷受影響筆數：

- `1`：庫存保留成功。
- `0`：場次不存在或庫存不足。

不得採用「先 SELECT 剩餘數量，再 UPDATE」的方式，因為兩個併發請求可能同時讀到相同庫存。

### 步驟 4：修改建立訂單流程

建立訂單 Service 應在同一個 `@Transactional` 交易中：

1. 從 JWT 取得使用者身分。
2. 驗證場次存在且已開賣。
3. 執行原子庫存保留 SQL。
4. 庫存不足時回傳 `409 Conflict`。
5. 由後端產生訂單編號。
6. 建立 `PENDING_PAYMENT` 訂單。
7. 設定 `expires_at = 現在時間 + 10 分鐘`。
8. 交易提交成功後，將到期資料寫入 Redis。

若新增訂單失敗，庫存保留必須跟著回滾。

### 步驟 5：修改付款流程

付款時先條件式更新訂單：

```sql
UPDATE interviewworks_ticket.ticket
SET status = 'PAID',
    paid_at = CURRENT_TIMESTAMP
WHERE orderno = :orderNo
  AND email = :email
  AND status = 'PENDING_PAYMENT'
  AND expires_at > CURRENT_TIMESTAMP
RETURNING session_id, quantity;
```

有成功更新訂單時，才執行：

```sql
UPDATE interviewworks_ticket.session
SET reserved = reserved - :quantity,
    sold = sold + :quantity
WHERE id = :sessionId
  AND reserved >= :quantity;
```

兩個動作必須在同一個交易中。

付款時不能再次減少總容量，因為建立訂單時已保留庫存。

### 步驟 6：修改取消訂單流程

只有 `PENDING_PAYMENT` 可以直接取消並釋放庫存：

```sql
UPDATE interviewworks_ticket.ticket
SET status = 'CANCELLED',
    cancelled_at = CURRENT_TIMESTAMP
WHERE orderno = :orderNo
  AND email = :email
  AND status = 'PENDING_PAYMENT'
RETURNING session_id, quantity;
```

有回傳資料時才執行：

```sql
UPDATE interviewworks_ticket.session
SET reserved = reserved - :quantity
WHERE id = :sessionId
  AND reserved >= :quantity;
```

條件式狀態更新可以避免重複取消時重複歸還庫存。

已付款訂單未來應走退款流程，不可直接套用待付款取消邏輯。

### 步驟 7：建立訂單過期機制

Redis 建議使用 Sorted Set：

```text
Key: ticket:payment:deadlines
Member: 訂單編號
Score: expires_at 的 Unix timestamp
```

建立訂單成功後執行概念操作：

```text
ZADD ticket:payment:deadlines <expiresTimestamp> <orderNo>
```

Spring 排程每隔 5 至 10 秒查找已到期訂單：

```text
ZRANGEBYSCORE ticket:payment:deadlines 0 <currentTimestamp>
```

過期處理必須使用條件式更新：

```sql
UPDATE interviewworks_ticket.ticket
SET status = 'EXPIRED'
WHERE orderno = :orderNo
  AND status = 'PENDING_PAYMENT'
  AND expires_at <= CURRENT_TIMESTAMP
RETURNING session_id, quantity;
```

只有成功改成 `EXPIRED` 時才能釋放 `reserved`，最後從 Redis Sorted Set 移除該訂單。

另外應定期掃描 PostgreSQL：

```sql
SELECT orderno
FROM interviewworks_ticket.ticket
WHERE status = 'PENDING_PAYMENT'
  AND expires_at <= CURRENT_TIMESTAMP;
```

這可以補償 Redis 重啟、資料遺失或排程中斷。

### 步驟 8：調整 API

建立訂單 Request 建議改成：

```json
{
  "sessionId": "S-001",
  "quantity": 1
}
```

以下資料應由後端決定，不接受前端提供：

- 訂單編號
- 使用者 Email
- 活動名稱
- 價格
- 訂單狀態
- 付款期限

成功 Response 範例：

```json
{
  "orderNo": "後端產生的訂單編號",
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-08-24T15:10:00+08:00"
}
```

建議錯誤狀態：

| 情況 | HTTP Status |
| --- | --- |
| 庫存不足 | `409 Conflict` |
| 訂單已過期 | `409 Conflict` |
| 場次或訂單不存在 | `404 Not Found` |
| 無權操作該訂單 | `403 Forbidden` |
| Request 格式錯誤 | `400 Bad Request` |

### 步驟 9：調整前端

- 不再由前端產生訂單編號。
- 建立訂單只傳 `sessionId` 和 `quantity`。
- 顯示後端回傳的 `expiresAt` 付款倒數。
- 倒數歸零後重新查詢訂單狀態。
- 收到 `409 Conflict` 時顯示庫存不足或訂單過期訊息。
- 收到 WebSocket 庫存事件時重新查詢場次資料。

前端倒數只能作為畫面提示，真正是否過期必須由後端判斷。

### 步驟 10：加入 WebSocket 庫存通知

以下事件發生後可廣播庫存異動：

- 訂單建立並保留庫存。
- 訂單取消並釋放庫存。
- 訂單過期並釋放庫存。
- 訂單付款成功，保留數轉為已售數。

訊息範例：

```json
{
  "type": "SESSION_INVENTORY_CHANGED",
  "sessionId": "S-001",
  "available": 18
}
```

WebSocket 只負責通知，不能作為庫存的真實來源。前端收到事件後應重新呼叫 API 取得最新資料。

## 交易邊界

以下操作各自必須放在同一個資料庫交易內：

### 建立訂單

```text
保留庫存 + 新增訂單
```

### 付款

```text
訂單改為 PAID + reserved 減少 + sold 增加
```

### 取消

```text
訂單改為 CANCELLED + reserved 減少
```

### 過期

```text
訂單改為 EXPIRED + reserved 減少
```

Redis 與 WebSocket 操作應在資料庫交易成功提交後執行，避免資料庫回滾但通知已送出。

## 冪等性要求

所有狀態修改都必須以目前狀態作為 SQL 條件：

- 只有 `PENDING_PAYMENT` 能付款。
- 只有 `PENDING_PAYMENT` 能直接取消。
- 只有已到期的 `PENDING_PAYMENT` 能改為 `EXPIRED`。
- 同一付款、取消或過期請求執行多次，結果必須與執行一次相同。

不能只在 Java 中先讀取狀態再決定是否更新，因為併發請求可能同時通過檢查。

## 測試與驗收

### 基本流程測試

- 有 10 張票，建立 1 張訂單後 `reserved = 1`。
- 付款後 `reserved = 0`、`sold = 1`。
- 待付款訂單取消後釋放庫存。
- 待付款訂單過期後自動釋放庫存。
- 過期訂單不能付款。
- 已取消訂單不能付款。
- 同一訂單重複付款不會重複增加 `sold`。
- 同一訂單重複取消不會重複減少 `reserved`。
- Redis 不可用時，資料庫掃描仍能處理過期訂單。

### 併發測試

測試資料：場次總容量為 10，同時送出 100 個建立訂單請求。

預期結果：

```text
成功建立：10
庫存不足：90
reserved：10
sold：0
available：0
```

接著讓 6 筆付款、2 筆取消、2 筆過期。

最終結果應為：

```text
capacity：10
reserved：0
sold：6
available：4
```

測試過程中任何時間都必須符合：

```text
reserved >= 0
sold >= 0
reserved + sold <= capacity
```

## 完成條件

符合以下條件才算完成第一階段第一項：

- [ ] 建立訂單時會立即保留庫存。
- [ ] 庫存不足時不能建立訂單。
- [ ] 訂單具有明確付款期限。
- [ ] 付款不會再次扣除可售庫存。
- [ ] 取消待付款訂單會釋放庫存。
- [ ] 過期訂單會自動釋放庫存。
- [ ] 重複付款、取消或過期不會重複異動庫存。
- [ ] Redis 故障後仍能透過 PostgreSQL 恢復過期處理。
- [ ] 100 個請求搶 10 張票時只會成功 10 筆。
- [ ] 自動化測試驗證所有庫存不變量。

## 下次接續方式

下次可以直接提出：

> 繼續處理 `PHASE1_INVENTORY_RESERVATION_PLAN.md`，從步驟 1 開始。

若步驟 1 已完成，也可以指定：

> 繼續處理 `PHASE1_INVENTORY_RESERVATION_PLAN.md`，從步驟 2 開始。

每完成一個步驟，應更新本文件的進度紀錄，再進入下一步。

## 進度紀錄

| 步驟 | 項目 | 狀態 |
| --- | --- | --- |
| 1 | 定義狀態與庫存規則 | 尚未開始 |
| 2 | 建立資料庫 Migration | 尚未開始 |
| 3 | 建立原子庫存保留 SQL | 尚未開始 |
| 4 | 修改建立訂單流程 | 尚未開始 |
| 5 | 修改付款流程 | 尚未開始 |
| 6 | 修改取消訂單流程 | 尚未開始 |
| 7 | 建立訂單過期機制 | 尚未開始 |
| 8 | 調整 API | 尚未開始 |
| 9 | 調整前端 | 尚未開始 |
| 10 | 加入 WebSocket 庫存通知 | 尚未開始 |
| 11 | 完成基本與併發測試 | 尚未開始 |

## 後續功能順序

完成本項目後，依原定路線接續：

1. 第一階段（二）：後端產生訂單編號。
2. 第一階段（三）：完整訂單狀態流程。
3. 第一階段（四）：第三方付款整合。

其中「後端產生訂單編號」會被建立訂單流程使用，因此可以在本文件步驟 4 先完成必要的基礎版本，之後再補強編號格式、唯一性與壓力測試。
