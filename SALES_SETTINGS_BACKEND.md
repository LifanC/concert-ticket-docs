# 場次分區、票種及限購

前後端已串接。管理後台的「分區與限購」可讀取與儲存場次設定；購票頁可選票種、篩選分區、查看實際票價及會員剩餘限購額度。每筆訂單仍為一個座位，會員可建立多筆訂單直到場次限購上限。

## 資料庫更新

新資料庫使用 `db-init/init.sql`，已包含新增資料表與欄位。

所有欄位與外鍵已直接寫入 `CREATE TABLE`，不使用 `ALTER TABLE`，原本的 `001_sales_settings.sql` 已移除。**請在全新空白資料庫執行 `db-init/init.sql`，再啟動後端。** 此初始化檔不會替既有資料表補欄位。沒有售票設定的場次繼續使用活動票價。

若 Docker Compose 的資料庫已建立為空白資料庫，可在專案根目錄的 PowerShell 執行：

```powershell
Get-Content -Raw -Encoding UTF8 db-init/init.sql |
  docker compose exec -T db psql -U postgres -d interviewworks -v ON_ERROR_STOP=1
```

本次調整沒有重建或刪除既有應用資料庫。

## 操作方式

1. 建立活動與場次，進入「分區與限購」選擇場次。
2. 設定每會員限購張數。未設定場次預填全場原價、全票與限購 4 張；按儲存才生效。
3. 依排別範圍設定分區名稱、顏色與價格。每一排只能屬於一區，所有排別必須分配。
4. 新增票種與價格比例，例如學生票 80% 代表八折。各票種適用所有分區。
5. 檢查預覽並按「儲存設定」。重新載入會讀回已儲存版本，匯出 JSON 只是下載備份。
6. 購票端選擇票種、分區與座位，再建立訂單。後端確認價格並保存訂單快照。

已有保留或售出座位的場次不可修改設定。已設定分區的場次不能更換活動；延後日期或更改狀態時維持已儲存容量與座位配置。

## API

實際 URL 前綴為 `/api`。

| Method | 路徑 | 權限 |
| --- | --- | --- |
| GET | `/v1/admin/sessions/{sessionId}/sales-settings` | ADMIN_ITEM_IMPLEMENT |
| PUT | `/v1/admin/sessions/{sessionId}/sales-settings` | ADMIN_ITEM_IMPLEMENT |
| GET | `/v1/booking/sessions/{sessionId}/sales-settings` | USER_ITEM_IMPLEMENT |

PUT 範例（排別範圍須符合實際場次）：

```json
{
  "version": 0,
  "maxTicketsPerMember": 4,
  "zones": [
    { "name": "A 區", "color": "#409eff", "rowStart": "A", "rowEnd": "J", "price": 2800 },
    { "name": "B 區", "color": "#67c23a", "rowStart": "K", "rowEnd": "T", "price": 1800 }
  ],
  "ticketTypes": [
    { "name": "全票", "pricePercent": 100, "eligibility": "" },
    { "name": "學生票", "pricePercent": 80, "eligibility": "入場時須出示學生證" }
  ]
}
```

GET 與 PUT 成功回傳 `configured`、`version`、`locked`、`rowLabels`、`seatsPerRow`、`capacity`、`defaultPrice`、`maxTicketsPerMember`、`zones` 與 `ticketTypes`。未設定場次的 version 為 0，zones 與 ticketTypes 為空陣列。

已設定分區含 `id`、`capacity`、`available` 及 `prices`；prices 以 ticketTypeId 為鍵，提供後端算好的票價。會員 GET 另回傳 `memberTicketQuantity`、`remainingAllowance`；未設定限購時 remainingAllowance 為 null。

`POST /v1/booking/saveTicket` 新增 `ticketTypeId`。已設定場次必須提供同場次有效票種，未設定場次可省略。price 為相容舊客戶端而保留，但不作定價依據，新前端不傳 price。冪等雜湊包含票種，重送不新增訂單。

## 儲存與訂位規則

- 使用 MyBatis、Spring 交易與既有場次列鎖；儲存設定與訂位都先取得同一場次鎖。
- PUT 驗證 version，避免覆蓋其他管理員的新設定。版本衝突回傳 409 SETTINGS_VERSION_CONFLICT，前端可重新載入。
- 分區重疊、漏配、名稱重複、無效價格／比例及容量不一致回傳 400；已有訂位時回傳 409 SALES_SETTINGS_LOCKED。
- 票價用 BigDecimal 計算「分區價格 × 價格比例 / 100」，以 HALF_UP 四捨五入至兩位小數，支援零元分區。
- 限購合計 PAID 與仍有效 PENDING_PAYMENT 的 quantity；取消、逾期與退款狀態不占額度。超額回傳 409 PURCHASE_LIMIT_REACHED。
- 限購檢查與建立訂單在同一交易內，並行請求不能突破限購。
- 票種資格說明供購票及入場提示，尚未自動驗證學生身分等證明文件。

## 新增資料

| 資料 | 用途 |
| --- | --- |
| session_sales_settings | 場次限購、版本、排別與每排席數快照 |
| session_zone | 分區價格、顏色與排數起訖 |
| session_ticket_type | 票種價格比例與資格說明 |
| session_seat.zone_id | 座位與分區關聯，複合外鍵確保屬於同場次 |
| ticket.zone_id / ticket_type_id / zone_name / ticket_type_name | 訂單分區及票種快照 |

票券 price 保存下單時價格，付款沿用此價格寫入 payprice。ticket 歷史分區／票種欄位不設外鍵，允許取消後重新配置時保留舊訂單快照。

## 目前驗證狀態

前後端功能已串接，前端正式版建置通過；後端在測試還原後執行 `./mvnw.cmd test-compile -DskipTests`，編譯通過，未執行測試。

後端只保留基本 Spring 啟動測試；銷售設定 Service、方法權限與前端銷售設定模擬 API 測試，以及測試輔助檔案已移除，預計之後再加入。目前測試的範圍與執行方式見 [後端 README](concert-ticket-backend/README.md#目前測試狀態) 與 [前端 README](concert-ticket-frontend/README.md#playwright-網頁自動化測試microsoft-edge)。

分區完整涵蓋、版本衝突、已售座位禁止修改、價格與限購、真實 PostgreSQL 交易回滾及併發仍需後續驗收；建置成功不代表上述流程已全部驗證。
