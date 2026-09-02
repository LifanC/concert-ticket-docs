# 售票系統程式修改優先順序

## 文件目的

本文件依據 [FEATURES_ROADMAP.md](FEATURES_ROADMAP.md)，整理目前程式應修改的順序、涉及檔案、實作原則及驗收條件。

本文件只提供修改指引，不代表相關程式已完成。

## 第一優先：修正訂單與庫存交易

### 目標

確保訂位、付款、取消及逾期流程中，訂單狀態與場次庫存永遠保持一致，並避免重複請求造成庫存重複異動。

### 主要修改範圍

- `BookingServiceImpl.java`
- `BookingPaymentScheduler.java`
- `BookingMapper.java`
- `BookingMapper.xml`
- Booking 相關 DTO
- 資料庫 Schema 或 migration

### 核心原則

`@Transactional` 只有在方法拋出例外時才會 rollback。Mapper 回傳更新筆數為 `0`，但方法正常 return，交易仍會 commit。因此每個重要更新都必須檢查 affected rows，不符合預期時拋出 `RuntimeException`。

`@PreAuthorize` 只負責權限驗證，不能保證訂單與庫存一致。

### 付款流程

建議執行順序：

1. 將指定會員的訂單由 `PENDING_PAYMENT` 更新為 `PAID`。
2. 確認訂單更新筆數等於 1。
3. 將場次 `reserved` 減少、`sold` 增加。
4. 確認場次更新筆數等於 1。
5. 任一步失敗時拋出例外，使整筆交易 rollback。
6. Transaction commit 後才取消逾期工作及發送通知。

訂單更新條件至少包含：

```sql
WHERE orderno = #{orderno}
  AND session_id = #{session_id}
  AND customer = #{customer}
  AND status = 'PENDING_PAYMENT'
  AND expires_at > CURRENT_TIMESTAMP
```

場次更新條件：

```sql
SET reserved = reserved - #{quantity},
    sold = sold + #{quantity}
WHERE id = #{session_id}
  AND reserved >= #{quantity}
```

`reserved > 0` 只能防止保留數變成負數，不能證明某一筆訂單具有付款資格。真正防止重複付款的是先以訂單編號、會員及 `PENDING_PAYMENT` 狀態更新特定訂單，並檢查更新筆數。

### 取消流程

建議執行順序：

1. 將指定訂單由 `PENDING_PAYMENT` 更新為 `CANCELLED`。
2. 確認訂單更新筆數等於 1。
3. 將場次 `reserved` 減少。
4. 確認場次更新筆數等於 1。
5. 任一步失敗時 rollback。
6. Commit 後取消逾期工作及發送通知。

若 `capacity` 代表場次固定總容量，取消時不可執行：

```sql
capacity = capacity + 1
```

取消只應釋放保留數：

```sql
SET reserved = reserved - #{quantity}
WHERE id = #{session_id}
  AND reserved >= #{quantity}
```

### 訂位流程

建議在同一交易內：

1. 原子式保留庫存或座位。
2. 確認庫存更新筆數等於 1。
3. 建立 `PENDING_PAYMENT` 訂單及付款期限。
4. 建立失敗時 rollback 已保留的庫存。
5. Commit 後才安排逾期處理及發送通知。

不應先查剩餘數量再更新，否則並發請求可能同時讀到相同庫存。

### 逾期流程

1. 將已到期訂單由 `PENDING_PAYMENT` 更新為 `EXPIRED`。
2. 只有訂單更新成功時才釋放 `reserved` 或座位。
3. 訂單狀態及庫存異動必須在同一交易內。
4. 重複執行同一逾期工作不得重複釋放庫存。
5. 付款與逾期同時執行時，只允許其中一方成功改變 `PENDING_PAYMENT`。

### 庫存異動對照表

假設欄位定義如下：

- `capacity`：場次固定總容量，訂位、付款、取消或逾期時都不應改變。
- `reserved`：已建立訂單、尚未付款且尚未逾期的保留數量。
- `sold`：已完成付款的售出數量。
- 可售數量：`capacity - reserved - sold`。

| 操作 | `capacity` | `reserved` | `sold` | SQL 應檢查的條件 |
| --- | ---: | ---: | ---: | --- |
| 建立待付款訂單 | 不變 | `+ quantity` | 不變 | `capacity - reserved - sold >= quantity` |
| 付款 | 不變 | `- quantity` | `+ quantity` | 特定訂單為 `PENDING_PAYMENT`，且 `reserved >= quantity` |
| 取消／逾期 | 不變 | `- quantity` | 不變 | 特定訂單為 `PENDING_PAYMENT`，且 `reserved >= quantity` |
| 退款並重新釋出票券 | 不變 | 不變 | `- quantity` | 特定訂單為 `PAID`，且 `sold >= quantity` |

其中只有「建立待付款訂單」會占用新的庫存，因此需要檢查剩餘可售數量。付款只是將已保留數轉成已售數；取消與逾期則釋放已保留數，兩者都不應使用剩餘可售數量作為執行條件。

場次數量條件只能保護統計值不變成負數，不能證明某一筆訂單有權付款、取消或退款。Service 必須先以訂單編號、登入會員及預期舊狀態成功更新特定訂單，再更新場次統計，並在任一步 affected rows 不符合預期時拋出例外 rollback。

### 驗收條件

- 重複付款不會重複增加 `sold`。
- 重複取消或逾期不會重複減少 `reserved`。
- 付款失敗時訂單與場次異動全部 rollback。
- 取消失敗時訂單與場次異動全部 rollback。
- `reserved`、`sold` 不會小於零。
- `reserved + sold` 不會大於 `capacity`。

## 第二優先：建立訂單狀態機

### 目標

集中管理合法狀態轉換，避免 Controller、Service 或 Mapper 任意寫入狀態字串。

### 建議修改

- 建立 `OrderStatus` enum。
- 建立集中處理付款、取消、逾期與退款的 Domain Service。
- SQL 必須包含預期舊狀態。
- 不合法的狀態轉換回傳明確錯誤代碼。

允許的狀態轉換：

```text
PENDING_PAYMENT ──付款──> PAID ──退款──> REFUNDED
        │
        ├──取消────────> CANCELLED
        └──逾期────────> EXPIRED
```

### 驗收條件

- 已付款訂單不能再次付款、取消或逾期。
- 已取消或逾期訂單不能付款。
- 每次狀態轉換都有明確舊狀態條件。
- 不合法轉換不會修改庫存。

## 第三優先：建立場次座位模型與防止重複售票

### 目標

讓資料庫保證同一場次、同一座位不能同時被多筆有效訂單持有。

### 建議修改

- 建立 `session_seat` 資料表。
- 建立 `(session_id, seat_id)` 唯一限制。
- 座位狀態使用 `AVAILABLE`、`RESERVED`、`SOLD`、`BLOCKED`。
- 保存 `reserved_by_order`、`reserved_until` 及 `version`。
- 使用條件更新或資料庫鎖原子式保留座位。

範例條件更新：

```sql
UPDATE session_seat
SET status = 'RESERVED',
    reserved_by_order = #{orderno},
    reserved_until = #{expires_at}
WHERE session_id = #{session_id}
  AND seat_id = #{seat_id}
  AND status = 'AVAILABLE'
```

只有更新筆數等於 1 才算成功。

### 驗收條件

- 100 個並發請求搶同一座位時恰好一筆成功。
- 不同座位可以並行訂購。
- 取消或逾期後座位恢復可售。
- 付款後座位改為 `SOLD`，不能再次保留。

## 第四優先：建立可恢復的逾期機制

### 目標

避免只依賴應用程式記憶體內的排程，使服務重新啟動後仍能處理未付款訂單。

### 建議修改

- 定期掃描 `expires_at <= CURRENT_TIMESTAMP` 的待付款訂單。
- 使用條件更新確保同一訂單只逾期一次。
- 可保留即時 TaskScheduler 作加速，但資料庫掃描必須作為補償機制。
- 多個後端實例同時執行時使用 `FOR UPDATE SKIP LOCKED` 或等效機制分工。

### 驗收條件

- 服務停止再啟動後，過期訂單仍會被處理。
- 多實例不會重複釋放同一筆庫存。
- 付款與逾期競爭時只產生一個最終狀態。

## 第五優先：API 冪等性

### 目標

避免連點、逾時重試或網路重送產生重複訂單與庫存異動。

### 建議修改

- 建立訂單 API 接受 `Idempotency-Key` Header。
- 保存會員、key、request hash、訂單編號及結果。
- 建立 `(user_id, idempotency_key)` 唯一限制。
- 相同 key 與相同內容回傳原結果。
- 相同 key 但不同內容回傳衝突錯誤。
- 付款平台交易編號建立唯一限制。

### 驗收條件

- 相同建立訂單請求重送多次只建立一筆訂單。
- 重複付款、取消與回呼不重複修改庫存或金額。

## 第六優先：統一 JWT 與登入者身分來源

### 目標

移除各 Request DTO 的 Token，避免每個 Service 重複解析 JWT，且不信任前端傳入的會員身分。

### 建議修改

- Token 統一放在 `Authorization: Bearer <token>`。
- `JwtAuthenticationFilter` 統一驗證 JWT 及 Redis Token 狀態。
- Controller 或 Service 從 Security Context 取得 user ID／email。
- Request DTO 移除 `token`、`customer` 等可由登入身分推導的欄位。
- Service 日誌不可記錄完整 Token。

### 驗收條件

- 未登入或 Token 無效時回傳 `401 Unauthorized`。
- 會員不能偽造 customer 操作他人訂單。
- Service 不再重複執行 Token 解析及 Redis 存在性檢查。

## 第七優先：明確 DTO 與錯誤處理

### 目標

讓 API 契約容易理解、驗證及測試。

### 建議修改

- 以明確 DTO 取代 `Map<String, Object>`。
- Request DTO 加入 Bean Validation。
- 建立業務例外與固定錯誤代碼。
- `GlobalExceptionHandler` 統一錯誤格式。
- 不再以 `200 OK` 搭配 `judge: false` 表示失敗。

建議錯誤代碼：

```text
ORDER_NOT_FOUND
INVALID_ORDER_STATE
ORDER_EXPIRED
SEAT_ALREADY_RESERVED
INSUFFICIENT_CAPACITY
FORBIDDEN_ORDER_ACCESS
INVENTORY_CONSISTENCY_ERROR
```

### 驗收條件

- 權限、驗證、衝突及不存在情境使用正確 HTTP 狀態碼。
- 所有錯誤包含 `code`、`message`、`traceId` 及 `timestamp`。
- OpenAPI 能清楚顯示請求與回應結構。

## 第八優先：資料庫型別與 Migration

### 目標

改善資料正確性，讓 Schema 可以安全、可重現地演進。

### 建議修改

- 使用 Flyway 或 Liquibase。
- 將日期與時間字串改成 `date`、`time` 或 `timestamptz`。
- 金額使用 `numeric(12,2)`，Java 使用 `BigDecimal`。
- 訂單使用正式 `user_id` 外鍵，並保存必要成交快照。
- 增加 Check Constraint、Foreign Key、Unique Constraint 與索引。
- 種子資料與 Schema migration 分離。

### 驗收條件

- 空白資料庫能依 migration 升級至最新版本。
- 既有資料能安全遷移。
- 非法狀態、負庫存及重複座位會被資料庫拒絕。

## 第九優先：自動化測試與 CI

### 目標

使用測試證明交易、狀態與併發規則，而不只驗證應用程式能啟動。

### 建議測試

- `BookingServiceTest`：狀態與商業規則。
- `BookingIntegrationTest`：真實交易與 rollback。
- `BookingConcurrencyTest`：並發搶位及付款／逾期競爭。
- `BookingSecurityTest`：登入與訂單所有權。
- 使用 Testcontainers 啟動 PostgreSQL 與 Redis。
- GitHub Actions 執行後端測試、前端測試與建置。

### 必測案例

1. 訂位後正確增加 `reserved`。
2. 付款後 `reserved` 減少、`sold` 增加。
3. 取消與逾期只釋放一次庫存。
4. 已付款訂單不能再次付款。
5. 付款與逾期同時執行，只有一方成功。
6. 同一座位並發搶購只有一筆成功。
7. 中途失敗時整筆交易 rollback。
8. 會員不能操作其他會員的訂單。

## 第十優先：管理後台

完成前九項的核心基礎後，再依 [PHASE3_ADMIN_DESIGN.md](PHASE3_ADMIN_DESIGN.md) 實作：

1. RBAC 與資料範圍。
2. 活動生命週期。
3. 場次、票種及限購。
4. 稽核紀錄。
5. 銷售儀表板。
6. CSV 匯出。

## 建議第一個開發批次

第一個批次只處理最直接的資料一致性風險：

1. 調整付款流程為先更新特定訂單，再更新場次統計。
2. 調整取消流程為先更新特定訂單，再釋放保留數。
3. 取消時不增加固定 `capacity`。
4. 每次 Mapper 更新都檢查 affected rows。
5. 更新筆數不符預期時拋出業務例外，觸發 rollback。
6. 補上重複付款、重複取消及交易 rollback 測試。

完成這一批後，再進入狀態機與場次座位模型。
