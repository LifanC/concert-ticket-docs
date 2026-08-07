# 演唱會訂票系統文件
## 功能

<ol>
    <li>
        <dl>
            <dt>使用者</dt>
            <dd>註冊</dd>
            <dd>登入</dd>
            <dd>修改會員資料</dd>
        </dl>
    </li>
    <li>
        <dl>
            <dt>活動</dt>
            <dd>查看活動</dd>
            <dd>搜尋活動</dd>
            <dd>查看活動詳情</dd>
        </dl>
    </li>
    <li>
        <dl>
            <dt>訂票</dt>
            <dd>選日期</dd>
            <dd>選場次</dd>
            <dd>選座位</dd>
            <dd>建立訂單</dd>
            <dd>查看我的票券</dd>
            <dd>取消訂單</dd>
        </dl>
    </li>
    <li>
        <dl>
            <dt>管理員</dt>
            <dd>新增活動</dd>
            <dd>修改活動</dd>
            <dd>刪除活動</dd>
            <dd>建立場次</dd>
            <dd>查看訂單</dd>
        </dl>
    </li>
</ol>

```
concert-ticket-docs/
│
├── docker-compose.yml
│
├── concert-ticket-backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│
├── concert-ticket-frontend/
│   ├── Dockerfile
│   ├── package.json
│   └── src/
│
└── .env
```
```
Docker-Compose (Run)
1. 啟動 Docker
- 停止服務 + 清掉容器（加上 --rmi all 就連 image 也刪）
- docker compose down -v --rmi all
2. 重新啟動所有服務
- docker compose up *前景執行（會顯示 log）*
- docker compose up -d *背景執行*
3. 即時查看執行狀況
- docker compose logs *看 log（前景）*
- docker compose logs -f *持續追蹤 log*
4. 看目前有哪些服務在跑
- docker compose ps
5. 查看 PostgreSQL
- docker exec -it <container_name> psql -U postgres
- \c interviewworks
- \dn
- \dt interviewworks_schema.*
- SELECT * FROM interviewworks_schema.roles;
- 或
- SET search_path TO interviewworks_schema;
- SELECT * FROM roles;
6. 查看 redis
- docker exec -it <container_name> redis-cli
- KEYS *
- GET <key_name>
- TTL <key_name> *查看 TTL (剩餘時間)*
- FLUSHALL 全清
```

##
## [演唱會訂票系統 前端](https://github.com/LifanC/concert-ticket-docs/concert-ticket-frontend)
## [演唱會訂票系統 後端](https://github.com/LifanC/concert-ticket-docs/concert-ticket-backend)
