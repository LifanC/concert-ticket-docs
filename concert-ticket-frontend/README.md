# 演唱會訂票系統前端

前端為以 Vue 3 建置，提供會員、活動、訂票與管理員操作介面。

## 技術

- Vue 3
- Vue Router
- Element Plus 2.14
- Axios
- SockJS

## 需求

- Node.js 20 或相容版本
- npm
- 可連線的後端 API（預設為 `http://localhost:8080/api`）

## 本機執行

在本目錄依 `package-lock.json` 安裝相依套件並啟動開發伺服器：

```powershell
npm ci
npm run dev
```

若要新增或更新相依套件，請使用 `npm install <package>`，並一併提交更新後的 `package.json` 與 `package-lock.json`。

開發伺服器預設網址為 http://localhost:5173。

## 後端連線設定

目前各頁面的 API base URL 及 WebSocket 位址直接寫在元件中，預設使用本機後端：

- REST API：`http://localhost:8080/api/v1/...`
- WebSocket：`http://localhost:8080/api/ws`

後端 CORS 也只允許此前端網址 `http://localhost:5173`。若部署到其他主機、網域或埠號，請一併更新前端元件中的網址與後端 `SecurityConfig` 的 CORS 設定。

## 功能

- 會員註冊、登入、修改會員資料與登出。
- 瀏覽、搜尋與篩選活動；可查看活動詳情。
- 選擇活動日期與場次後建立訂單。
- 查看票券、付款與取消訂單。
- 管理員可管理活動與場次，並查看售票資料。

## Docker

由專案根目錄執行下列指令，即可連同後端、PostgreSQL 與 Redis 一起啟動：

```powershell
docker compose up --build
```
