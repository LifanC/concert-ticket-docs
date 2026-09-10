# Roadmap 測試紀錄（2026-09-10）

## 環境與結果

- 網頁：`http://localhost:5173/`，使用本機 Microsoft Edge（Playwright `channel: msedge`）。
- Edge 自動化：基礎 6 項、真實管理員 1 項、真實一般會員 1 項與活動進入訂票 1 項，共 9 項通過；基礎測試已分別以無頭與可見視窗模式執行，帳號測試使用可見視窗。
- Java：`TicketApplicationTests` 5 項、`BookingOrderServiceTests` 13 項，共 18 項通過。
- 前端 `npm run build` 成功；仍有大於 500 kB 的 bundle 提示。
- Python `test_analyze`：6 項通過；CSV 測試首次受沙箱暫存目錄權限阻擋，沙箱外重跑全部通過。另以真實 PostgreSQL 執行 `analyze.py` 成功匯出 2 個場次；Java 排程的 `analytics-latest.log` 也記錄成功匯出。

## 本次功能修正

1. 活動搜尋支援名稱、場地與活動編號，忽略大小寫並容忍空欄位。
2. 「只看收藏」使用活動加場次的識別值，正確保留收藏的場次。

## Roadmap 對照與範圍

| 項目 | 本次驗證 | 限制 |
| --- | --- | --- |
| Phase 1.2 訂單轉換 | 待付款轉付款／取消／逾期、拒絕終態再次付款、重複逾期不釋放庫存 | Mapper 使用 mock，未證明真實 SQL 或 rollback |
| Phase 1.3 競爭保護 | 上鎖後重讀狀態、條件更新 0 筆時不異動庫存 | 非真實資料庫併發測試 |
| Phase 2 Spring 啟動 | Security chain 存在、訂單 Service 有交易代理 | 停用報表與資料初始化／補償任務，避免啟動測試修改本機資料 |
| Phase 2 Security | 匿名訂票 401、會員讀後台 403、管理員讀後台 200、會員讀自己的票券 200 | 真實 Security chain／Controller；JWT、Redis、業務 Service 使用 mock，未驗證 JWT 簽章與真實登入 |
| Phase 2 Edge | 首頁真實 API 回傳 200；匿名訂票／後台導向登入並保留目的地 | 未完成登入後實際購票付款流程 |
| 真實管理員 Edge | 使用提供的帳號登入，進入後台並切換活動管理／建立場次／查看訂單；三支後台查詢 API 均回傳 200，無 JavaScript 執行錯誤 | 唯讀驗證，未新增／刪除活動或建立場次；登入後手動點選後台導覽 |
| 後台慢速操作回歸 | 擴充同一管理員案例，驗證活動編號搜尋、無結果提示、清除搜尋、新增／修改表單開啟、場次表單與列表、訂單 `CT202609101` 顯示；可見 Edge 約 1.5 分鐘通過 | 表單僅開啟檢視，未驗證新增／修改儲存、刪除或場次狀態異動 |
| 真實會員 Edge | 登入、訂票頁、我的票券 API 200、三支管理查詢 API 403、登出清除 Token、登出後訂票重新導向登入 | 未建立訂單、付款或取消既有票券；未驗證不同會員間的資料隔離 |
| 活動進入訂票 Edge | 真實會員登入後，從活動列表選售票中活動，查看詳情，再進入訂票、選日期／場次／可用座位，開啟訂單確認視窗 | 慢速可見視窗執行約 1.3 分鐘；最後返回修改，未送出建立訂單或付款 |
| 實際訂位與重新查詢 | 後續依使用者要求送出訂單 `CT202609101`，座位 `A-01`，建立 API 回傳 201；重新登入後由「訂票 → 查看我的票券」查得相同編號、座位及 `PENDING_PAYMENT` 狀態 | 首次流程在建立成功後因「活動」文字定位重複而中止；改用導覽 link 定位，另跑既有訂單查詢通過（32.3 秒），未重複建立訂單、未付款 |
| Python 銷售報表 | 6 項測試：排除非 PAID 狀態、免費票／零營收、缺漏金額、Decimal 精度、Excel 編碼／公式字元、空報表；真實資料匯出 2 個場次 | 聚合規則測試使用 SQLite，非 PostgreSQL 鎖／交易測試；Java 排程僅核對既有成功日誌，未測逾時及關機中止；報表目前為 CSV，未整合後台圖表 |
| 前端回歸 | 場地／編號搜尋、清除篩選、收藏場次隔離、詳情進入訂票 | 使用攔截 API 的固定資料，未寫入實際訂單 |

本次未驗證：100 人搶同座位、同毫秒請求、資料庫鎖等待與 rollback、完整 JVM 重啟補償、付款／退款、主辦方權限、負載／壓力與 CI。Roadmap 原有「已完成」紀錄不代表本次重新驗證。

## 重跑

前端及後端服務啟動後，在 `concert-ticket-frontend` 執行：

```powershell
npm ci
npm run test:e2e
# 顯示 Microsoft Edge 操作視窗（每項測試結束會自動關閉）
npm run test:e2e:headed
npm run build
```

本機需已安裝 Microsoft Edge。HTML 報告位於 `concert-ticket-frontend/playwright-report/index.html`；失敗時截圖與 trace 位於 `test-results/`，均不納入 Git。

管理員測試另需由執行環境提供 `E2E_ADMIN_ACCOUNT` 與 `E2E_ADMIN_PASSWORD`，未提供時會跳過該項。執行 `npm run test:e2e:headed -- tests/e2e/admin.spec.js` 可單獨觀看；此項停用 trace／截圖／影片，帳密不寫入原始碼。HTML 報告每次執行會更新，因此單獨跑管理員測試後會顯示該次 1 項結果。

管理員測試也支援 `E2E_SLOW_MO` 與 `E2E_VIEW_PAUSE`；可選填 `E2E_VERIFY_ORDER` 驗證指定訂單出現在後台。若只需登入並保留後台視窗供人工操作，使用相同管理員環境變數執行 `node scripts/admin-demo.mjs`，關閉 Edge 後程序才結束。

一般會員測試使用 `E2E_MEMBER_ACCOUNT` 與 `E2E_MEMBER_PASSWORD`，同樣不保留 trace／截圖／影片。執行 `npm run test:e2e:headed -- tests/e2e/member.spec.js`。若要慢速觀看，可先設定 `$env:E2E_SLOW_MO='2000'`（每次操作延遲 2 秒）、`$env:E2E_VIEW_PAUSE='8000'`（登入完成、票券及登出後畫面停留 8 秒）；預設使用快速測試模式。

從活動列表進入訂票的慢速示範，使用相同會員環境變數執行 `npm run test:e2e:headed -- tests/e2e/booking.spec.js`。支援相同慢速設定，在活動列表、詳情、日期、場次、座位及確認視窗停留；需有真實售票中活動及可用座位，缺少時測試會失敗並指出前置條件。

若要實際送出訂單並查詢，另設定 `$env:E2E_CREATE_ORDER='1'`：測試會按「確認建立」，驗證 HTTP 201 與新訂單，離開訂票頁再從「訂票 → 查看我的票券」重新查詢同一訂單編號、座位及待付款狀態。此模式會保留一筆真實待付款訂單，依系統設定於 10 分鐘後逾期；預設未開啟時仍僅展示確認畫面。

若訂單已成功建立而後續測試中止，可提供 `E2E_VERIFY_ORDER`（訂單編號）與 `E2E_VERIFY_SEAT`（座位）單獨重跑查詢，避免再次建立訂單；此模式驗證待付款狀態，請於付款期限內執行。

在 `concert-ticket-backend` 執行：

```powershell
.\mvnw.cmd clean test
```

需 Java 21 及後端環境設定。使用 `clean` 排除 `target` 中舊測試 class／報告；本次新增的單元測試不需要真實資料庫。
