# 演唱會訂票系統文件
## 功能

### 👤 使用者功能

- 註冊
- 登入
- 修改會員資料
```
登入 API
↓
取得 Access Token

其他 API
↓
JwtAuthenticationFilter
↓
解析 JWT
↓
建立 GrantedAuthority
↓
@PreAuthorize 驗證 Permission
```
```
Refresh Token
      |
      | 取得 account
      ↓
查 DB User Role Permission
      |
      ↓
產生 Access Token
      |
      | JWT claim:
      | permissions=["ADMIN_ITEM_IMPLEMENT"]
      | permissions=["USER_ITEM_IMPLEMENT"]
      ↓
JWT Filter
      |
      ↓
GrantedAuthority
      |
      ↓
@PreAuthorize("hasAuthority('ADMIN_ITEM_IMPLEMENT')")
```
```
Vue
 |
 | POST /api/v1/?/?
 ↓
SecurityFilterChain ✅
 |
JwtAuthenticationFilter ✅
 |
@PreAuthorize ✅ (如果沒403)
 |
Controller ✅
 |
@Valid DTO ✅
```

### 🎵 活動功能

- 查看活動
- 搜尋活動
- 查看活動詳情

### 🎫 訂票功能

- 選擇日期
- 選擇場次
- 選擇座位
- 建立訂單
- 查看我的票券
- 取消訂單

### 🔧 管理員功能

- 新增活動
- 修改活動
- 刪除活動
- 建立場次
- 查看訂單


## 使用技能

### Backend

- **Language**
    - Java

- **Framework**
    - Spring Boot

- **Database**
    - PostgreSQL

- **Cache**
    - Redis
	- JWT 黑名單管理
	- Refresh Token 快取

- **Authentication**
    - JWT（JSON Web Token）

- **API Documentation**
    - Swagger
	- Swagger UI：網址/api/swagger-ui/index.html
    - OpenAPI JSON：網址/api/v3/api-docs

- **Security**
    - CORS（跨來源資源共享設定）

## [演唱會訂票系統 前端](https://github.com/LifanC/concert-ticket-frontend)
