# Phase 3：管理後台設計規格

## 1. 文件目的

本文件定義管理後台 Phase 3 的可實作範圍，包括銷售儀表板、活動與場次管理、角色權限、資料範圍及稽核。此文件是設計規格，不代表功能已完成。

前置條件：訂單狀態機與庫存交易已穩定、防超賣測試已通過、JWT 已由 `Authorization` Header 傳入，且 Schema 已納入 migration 管理。

## 2. 範圍

### 本階段包含

- 銷售摘要、趨勢與場次報表
- 活動、場次、票種、售票期間與限購管理
- 活動草稿、上架、下架及封存
- 系統管理員、活動主辦方及驗票人員權限
- 主辦方與工作人員的資料範圍限制
- 重要操作的稽核紀錄
- CSV 匯出

### 本階段不包含

- 真實金流與退款串接
- QR Code 產生及現場驗票流程
- 虛擬排隊室與機器人偵測
- 大型檔案或影音素材管理

## 3. 角色與授權模型

### 3.1 角色

| 角色 | 說明 |
| --- | --- |
| `SYSTEM_ADMIN` | 管理所有活動、使用者、角色、報表及系統設定 |
| `ORGANIZER` | 管理被授權的活動、場次、票種及其報表 |
| `CHECKIN_STAFF` | 存取被指派場次的驗票資料；本階段只建立權限與指派關係 |
| `MEMBER` | 使用前台瀏覽、收藏、訂票及管理自己的訂單 |

既有 `ADMIN_ITEM_IMPLEMENT` 與 `USER_ITEM_IMPLEMENT` 可在 migration 過渡期間保留，新功能改用細粒度權限。

### 3.2 權限代碼

| 權限 | 用途 |
| --- | --- |
| `ADMIN_DASHBOARD_READ` | 查看授權範圍內的統計 |
| `ADMIN_REPORT_EXPORT` | 匯出報表 |
| `ACTIVITY_READ` | 查看後台活動資料 |
| `ACTIVITY_CREATE` | 建立活動草稿 |
| `ACTIVITY_UPDATE` | 編輯活動與場次 |
| `ACTIVITY_PUBLISH` | 上架、下架或封存活動 |
| `TICKET_TYPE_MANAGE` | 管理票種、售價與限購 |
| `ROLE_MANAGE` | 管理角色與權限；限系統管理員 |
| `AUDIT_LOG_READ` | 查看稽核紀錄 |
| `CHECKIN_READ`／`CHECKIN_WRITE` | Phase 4 驗票功能使用 |

### 3.3 雙層授權

每個後台請求都必須同時通過：

1. 功能權限：是否具備對應 permission。
2. 資料範圍：是否能操作該 `activity_id` 或 `session_id`。

| 角色 | 資料範圍 |
| --- | --- |
| `SYSTEM_ADMIN` | 全部活動與場次 |
| `ORGANIZER` | `organizer_activity` 中已授權的活動 |
| `CHECKIN_STAFF` | `staff_session_assignment` 中已指派的場次 |
| `MEMBER` | 不可使用後台 API |

前端隱藏按鈕不構成安全控制。後端必須從已驗證身分取得操作者，不接受 request body 傳入操作者帳號。

## 4. 活動與場次管理

### 4.1 管理狀態

| 狀態 | 規則 |
| --- | --- |
| `DRAFT` | 僅後台可見，可自由編輯 |
| `PUBLISHED` | 前台可見，關鍵欄位異動受限制 |
| `UNPUBLISHED` | 前台隱藏，不接受新訂單，既有訂單仍可處理 |
| `ARCHIVED` | 僅供歷史查詢，不可再次上架 |

售票狀態 `COMING_SOON`、`TICKETS_ARE_ON_SALE`、`SOLD_OUT`、`ENDED` 應依售票時間、活動時間與庫存推導，不由管理員任意指定。

```text
DRAFT ──上架──> PUBLISHED ──下架──> UNPUBLISHED
  │                 │                    │
  └──封存───────────┴──────封存──────────> ARCHIVED
                    └──重新上架（符合條件）──> PUBLISHED
```

### 4.2 上架檢查

- 名稱、分類、說明、場館及封面完整。
- 至少一個尚未開始且時間合法的場次。
- 每個場次至少一個有效票種及正整數庫存。
- 售票開始早於結束，售票結束不晚於演出時間。
- 票價、幣別、每人限購及座位設定合法。

### 4.3 上架後修改

- 名稱、說明與圖片可修改，但必須留下稽核紀錄。
- 已產生訂單的場次不得直接刪除。
- 場次時間或場館異動必須記錄原因並建立通知事件。
- 票種總量不得降低至 `reserved + sold` 以下。
- 票價異動不得回寫既有訂單；訂單保留成交價格快照。

### 4.4 票種

| 欄位 | 說明 |
| --- | --- |
| `id` | 票種識別碼 |
| `session_id` | 所屬場次 |
| `name` | VIP、一般、早鳥等名稱 |
| `price`／`currency` | 精確金額與幣別 |
| `quota` | 可售總量 |
| `sale_starts_at`／`sale_ends_at` | 售票期間 |
| `purchase_limit` | 每位會員限購數 |
| `status` | `ACTIVE` 或 `INACTIVE` |

價格、優惠與限購都由後端查詢及計算，不信任前端提交的結果。

## 5. 銷售儀表板

### 5.1 指標

| 指標 | 定義 |
| --- | --- |
| 容量 | 場次或票種設定的總可售數 |
| 有效保留 | 未逾期 `PENDING_PAYMENT` 數量 |
| 已售 | `PAID` 且尚未退款的票數 |
| 剩餘 | `capacity - reserved - sold` |
| 售票率 | `sold / capacity * 100%`；容量為零時為 0 |
| 銷售總額 | 指定期間內成功付款金額合計 |
| 退款總額 | 指定期間內成功退款金額合計 |
| 淨營收 | 銷售總額減退款總額 |

統計時區預設為 `Asia/Taipei`，時間區間採 `[from, to)`，營收歸屬預設採付款日。

### 5.2 篩選與畫面

篩選條件：日期區間、活動、場次、分類與統計粒度（日／週／月）。

頁面包含摘要卡、銷售與退款趨勢、活動營收排行、訂單狀態分布、可分頁排序的場次表格及 CSV 匯出。

統計初期由 PostgreSQL 聚合；確認效能不足後再考慮物化檢視或彙總表，不以 Redis 作為財務報表的唯一資料來源。

## 6. API 規格

Base path：`/v1/admin`

### 6.1 儀表板與報表

| Method | Path | 權限 | 說明 |
| --- | --- | --- | --- |
| `GET` | `/dashboard/summary` | `ADMIN_DASHBOARD_READ` | 銷售摘要 |
| `GET` | `/dashboard/trends` | `ADMIN_DASHBOARD_READ` | 日／週／月趨勢 |
| `GET` | `/dashboard/sessions` | `ADMIN_DASHBOARD_READ` | 活動與場次統計 |
| `GET` | `/reports/sales.csv` | `ADMIN_REPORT_EXPORT` | 匯出銷售 CSV |

共同 query：`from`、`to`、`activityId`、`sessionId`、`granularity`、`page`、`size`。

### 6.2 活動與場次

| Method | Path | 權限 | 說明 |
| --- | --- | --- | --- |
| `GET`／`POST` | `/activities` | `ACTIVITY_READ`／`ACTIVITY_CREATE` | 查詢活動／建立草稿 |
| `GET`／`PATCH` | `/activities/{activityId}` | `ACTIVITY_READ`／`ACTIVITY_UPDATE` | 查詢／編輯活動 |
| `POST` | `/activities/{activityId}/publish` | `ACTIVITY_PUBLISH` | 上架 |
| `POST` | `/activities/{activityId}/unpublish` | `ACTIVITY_PUBLISH` | 下架 |
| `POST` | `/activities/{activityId}/archive` | `ACTIVITY_PUBLISH` | 封存 |
| `POST` | `/activities/{activityId}/sessions` | `ACTIVITY_UPDATE` | 建立場次 |
| `PATCH` | `/sessions/{sessionId}` | `ACTIVITY_UPDATE` | 編輯場次 |
| `POST` | `/sessions/{sessionId}/ticket-types` | `TICKET_TYPE_MANAGE` | 建立票種 |
| `PATCH` | `/ticket-types/{ticketTypeId}` | `TICKET_TYPE_MANAGE` | 編輯票種 |

### 6.3 權限與稽核

| Method | Path | 權限 | 說明 |
| --- | --- | --- | --- |
| `GET` | `/roles` | `ROLE_MANAGE` | 查詢角色與權限 |
| `PUT` | `/users/{userId}/roles` | `ROLE_MANAGE` | 更新使用者角色 |
| `PUT` | `/activities/{activityId}/organizers` | `ROLE_MANAGE` | 指派主辦方 |
| `PUT` | `/sessions/{sessionId}/checkin-staff` | `ROLE_MANAGE` | 指派驗票人員 |
| `GET` | `/audit-logs` | `AUDIT_LOG_READ` | 查詢稽核紀錄 |

### 6.4 API 共通規則

- 列表一律分頁並限制 `size` 上限。
- 使用明確 DTO 與 Bean Validation。
- 更新資源使用 `version` 或 ETag；衝突回傳 `409 Conflict`。
- 找不到資源回傳 `404`；無權限回傳 `403`；無資料範圍時回傳 `404`，避免洩漏資源存在性。
- 錯誤至少包含 `code`、`message`、`traceId`、`timestamp` 及欄位錯誤。
- CSV 須設定正確檔名與 Content-Type，並防止 CSV Formula Injection。

## 7. 資料模型

| 資料表 | 用途 |
| --- | --- |
| `role`、`permission` | 角色與權限定義 |
| `role_permission`、`user_role` | RBAC 關聯 |
| `organizer_activity` | 主辦方活動範圍 |
| `staff_session_assignment` | 驗票人員場次範圍 |
| `ticket_type` | 場次票種、價格、額度、期間與限購 |
| `activity_image` | 圖片 URL、排序與用途 |
| `audit_log` | 操作者、動作、資源、前後值、trace ID 與時間 |

`activity` 增加 `management_status`、`version`、`published_at`；`session` 與 `ticket_type` 也增加 `version`。時間使用 `timestamptz`，金額使用 `numeric(12,2)` 並保存幣別。

建議索引：`ticket(session_id, status, paid_at)`、`ticket(created_date, status)`、`session(activity_id, date)`、`organizer_activity(user_id, activity_id)`、`audit_log(resource_type, resource_id, created_at)`。

## 8. 稽核與安全

活動生命週期、場次異動、票價／庫存／限購調整、角色指派與報表匯出都必須寫入稽核紀錄。

紀錄至少包含操作者 ID、動作、資源、結果、異動前後摘要、IP、trace ID 與時間。密碼、Token、付款資料及完整個資不得寫入紀錄。業務異動與稽核紀錄應在同一交易內；通知可使用 transactional outbox 在提交後送出。

## 9. 非功能需求

- 儀表板一般查詢在代表性資料量下 P95 小於 500 ms。
- CSV 限制日期範圍及最大筆數；大量匯出改為非同步工作。
- 所有後台 API 具備輸入驗證、權限及資料範圍測試。
- 金額不使用 `float` 或 `double`。
- 圖片限制 MIME type 與大小；正式環境使用物件儲存。
- 個資依最小揭露原則回傳，列表預設遮罩敏感資訊。

## 10. 驗收測試

| 情境 | 預期結果 |
| --- | --- |
| 未登入呼叫後台 API | `401 Unauthorized` |
| `MEMBER` 呼叫後台 API | `403 Forbidden` |
| 主辦方讀取自己的活動 | 成功 |
| 主辦方猜測他人活動 ID | `404 Not Found` |
| 不完整草稿上架 | `422 Unprocessable Entity` |
| 兩位管理員同時修改同一活動 | 一方成功，另一方 `409 Conflict` |
| 票種額度降至已售數以下 | 拒絕且資料不變 |
| 退款訂單進入報表 | 銷售與退款分列，淨營收正確 |
| 匯出欄位含公式字首 | 內容已安全轉義 |
| 權限或價格異動 | 可由稽核紀錄追查 |

## 11. 實作里程碑

1. Milestone A：建立 RBAC、主辦方／工作人員指派及資料隔離測試。
2. Milestone B：建立活動生命週期、上架檢查、樂觀鎖、票種及稽核事件。
3. Milestone C：完成統計 SQL、儀表板、CSV 匯出及效能測試。

## 12. Phase 3 完成定義

- 不同角色只能使用被授權功能，且只能操作授權資料。
- 活動依合法狀態流程建立、上架、下架及封存。
- 後台修改不會破壞既有訂單或庫存一致性。
- 儀表板及匯出結果與相同條件下的資料庫結果一致。
- 重要異動皆有不可由一般後台使用者修改的稽核紀錄。
- 權限、資料隔離、樂觀鎖、統計及匯出皆有自動化測試。
