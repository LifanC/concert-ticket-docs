# 安全設定與部署

## 目前有哪些保護？

- **登入安全**：檢查登入憑證（JWT）的用途、簽章、期限，以及 Redis 中的登入紀錄。每位會員只能保留一次有效登入，重新登入後，舊登入會失效。
- **登出後無法繼續使用**：登出會刪除登入與憑證紀錄；即使同時更新憑證，也不會讓舊登入恢復有效。
- **更新憑證**：Refresh token 有固定期限，使用時必須與 Redis 紀錄完全相符，目前不會每次換發新的 refresh token。
- **權限與資料保護**：用完整 email 判斷權限；新的 Redis 會員快取不存密碼雜湊，日誌不記錄 Authorization 標頭或驗證失敗的欄位值。
- **請求次數限制**：限制 API、登入、註冊、更新憑證、下單及即時通知的請求量。超量回傳 `429` 與建議重試時間（`Retry-After`）；Redis 故障時拒絕請求，回傳 `503`。
- **即時通知（WebSocket）**：只允許指定網站連線，只能訂閱自己的通知（`/user/queue/notifications`）。每條連線只能訂閱一次，不能主動傳送訊息；傳送通知前會檢查登入狀態，每 5 秒清理失效或逾時連線。
- **資料庫連線**：Docker Compose 的 PostgreSQL、Redis 主機連接埠只開放給本機（`127.0.0.1`）。

## 部署前要設定什麼？

| 設定 | 使用方式 |
| --- | --- |
| `PAYMENT_SIMULATION_ENABLED` | 預設 `false`，只有本機示範才設為 `true`。目前沒有串接真實金流。 |
| `REFRESH_COOKIE_SECURE` | 正式 HTTPS 環境設為 `true`；本機 HTTP 開發才用 `false`。 |
| `ALLOWED_ORIGINS` | 填入允許連線的前端網址，多個網址用逗號分隔。正式部署時移除 localhost。 |

模擬付款關閉時，`/api/v1/booking/dopayprice` 會回傳 `403` 與 `PAYMENT_SIMULATION_DISABLED`，不會更新訂單。

更新後，舊 JWT 因缺少 `token_use` 欄位而失效，使用者需要重新登入。舊 Redis 會員快取不再讀取，會按原期限自動刪除；舊日誌若含憑證，仍需管理存取權限與保留時間。

## 流量限制

後端預設每 60 秒的上限：

| 範圍 | 上限 |
| --- | --- |
| 每個 IP | 全部 API 300 次、登入 20 次、註冊 5 次、更新憑證 60 次 |
| 每個帳號／實際會員 | 登入 10 次 |
| 每次有效登入（session） | 更新憑證 20 次 |
| 每位會員 | 下單 20 次 |

可透過 `security.limits.*` 調整，例如 `SECURITY_LIMITS_API_PER_IP=600`。

短時間內登入太多次時，會員頁會提示還要等幾秒，等待期間不能再按登入。依預設設定，最多等 60 秒；例如這一分鐘已過 20 秒，就再等約 40 秒。若是 Nginx 擋下請求，則提示等待 60 秒。倒數結束後需要自己再按登入，系統不會自動重試。

按下登入後也會顯示處理中，避免連續點擊。若伺服器沒有提供有效的等待時間，畫面只會提示稍後再試。等待時間由 `Retry-After` 回應標頭提供，後端已允許不同網域的前端讀取。

WebSocket 在**每個後端執行個體**最多允許 1000 條連線，每個 IP 20 條、每位會員 5 條。請求次數限制透過 Redis 跨執行個體共用，但連線數上限各自計算。

Nginx 另外限制一般 API 每秒 10 次、登入與註冊每分鐘 10 次，並限制短時間突增的請求與同時連線數。這些規則只在前端 Dockerfile 的最後 production 階段生效；目前 Compose 使用 Vite 開發伺服器，不會套用。

## 使用反向代理時要注意什麼？

Render 的公開網站入口會提供使用者 IP（`CF-Connecting-IP`）。當 Render 自動設定的 `RENDER=true` 生效時，Nginx 與後端會使用這個 IP；本機預設忽略代理標頭。

前端 Nginx 再轉送到公開後端時，後端看到的連線來源會變成前端主機。因此，前後端需要共用密鑰，讓後端確認轉送的使用者 IP 確實來自前端。

部署步驟：

1. 在 PowerShell 執行下方指令，產生 64 個十六進位字元的隨機密鑰。只需執行一次，前後端使用同一組值。
2. 在 Render 前端與後端服務的 Environment 都加入 `PROXY_SHARED_SECRET`，填入同一組值。不要加上 `VITE_` 前綴，也不要把密鑰寫進 Git。
3. 重新部署前後端。前端必須使用本專案 Dockerfile 的 Nginx 階段；Render Static Site 不會執行這份 Nginx 設定。
4. 確認不同網路的使用者不會共用限流額度；偽造 `X-Forwarded-For` 或沒有正確密鑰的 `X-Ticket-Client-IP` 不應改變後端判斷的 IP。

產生密鑰的 PowerShell 指令：

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[BitConverter]::ToString($bytes).Replace('-', '').ToLower()
$rng.Dispose()
```

將輸出的整串文字複製到 Render 前後端的 `PROXY_SHARED_SECRET`，不要加引號。`RENDER=true` 由 Render 自動提供，不需要另外新增。

Render 前端缺少合格密鑰時會停止啟動，避免部署後無聲地共用 IP 額度。後端只有密鑰相符時，才採用前端轉送的 IP；直接呼叫後端則使用 Render 入口提供的 IP。API 與 WebSocket 連線上限使用相同的 IP 判斷。

保留 `server.forward-headers-strategy=none`，不直接信任 `X-Forwarded-For`。這套設定只適用於 Render 公開 Web Service；不要在可被外部直接連入的自架主機，或 Render 私有服務上手動啟用 `RENDER=true`。

參考：[Render 的使用者 IP 說明](https://render.com/articles/host-pocketbase-on-render)。

目前使用單節點 Redis。若改用 Redis Cluster，需先調整登入腳本，讓相關資料鍵位於同一個 hash slot。

## 如何確認功能正常？

- 後端執行 `./mvnw test`，前端執行 `npm run build`。
- 測試涵蓋憑證用途、登入失效與替換、流量限制、Redis 故障、WebSocket 權限、停用模擬付款，以及同時登入或更新憑證的情況。
- 單元測試使用模擬的 Redis 與資料庫。上線前仍需在隔離測試環境，搭配真實 Redis、PostgreSQL、Nginx、代理 IP 與 SockJS 備援連線，確認登入、更新憑證、登出、通知、付款停用與流量限制正常。

Render 已自動提供免費 DDoS 防護，不需要另外啟用。程式限流仍需保留，以減少 API 被濫用；也應持續監控流量與主機負載。平台防護不代表網站保證不會中斷。參考：[Render DDoS 防護](https://render.com/docs/ddos-protection)。

若改用其他平台，需重新確認入口防護與代理 IP 設定。Render 禁止對其基礎設施進行 DoS 測試，請勿對 Render 站台發動這類測試。參考：[Render 測試政策](https://render.com/docs/penetration-testing)。
