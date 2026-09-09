"""Export current paid-order totals per session without modifying the database."""

import argparse
import csv
import os
import sys
from contextlib import closing
from datetime import datetime, timezone
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
SALES_SQL = """
SELECT s.id,
       COUNT(t.orderno) AS paid_orders,
       COALESCE(SUM(t.payprice), 0) AS paid_amount,
       COUNT(t.orderno) - COUNT(t.payprice) AS missing_amounts
FROM interviewworks_ticket.session s
LEFT JOIN interviewworks_ticket.ticket t
  ON t.session_id = s.id AND t.status = 'PAID'
GROUP BY s.id
ORDER BY s.id
"""


def fetch_sales(connection):
    with closing(connection.cursor()) as cursor:
        cursor.execute("SET TRANSACTION READ ONLY")
        cursor.execute("SET LOCAL statement_timeout = '30s'")
        cursor.execute(SALES_SQL)
        rows = cursor.fetchall()
    if any(row[3] for row in rows):
        raise ValueError("已付款訂單有缺少 payprice 的資料，請先確認金額再產生報表。")
    return [tuple(row[:3]) for row in rows]


def write_report(rows, output_dir):
    output_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S_%fZ")
    path = output_dir / f"sales_report_{stamp}.csv"
    with path.open("x", encoding="utf-8-sig", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(["場次編號", "已付款訂單數", "已付款訂單金額"])
        for session_id, count, amount in rows:
            # Treat database identifiers as text when opened in spreadsheets.
            if session_id.startswith(("=", "+", "-", "@", "\t", "\r", "\n")):
                session_id = "'" + session_id
            writer.writerow([session_id, count, format(amount, ".2f")])
    return path


def main():
    parser = argparse.ArgumentParser(description="匯出各場次目前已付款訂單的數量與金額。")
    parser.add_argument("--output-dir", type=Path, default=BASE_DIR / "reports",
                        help="報表目錄（預設為程式旁的 reports；自訂相對路徑相對於目前目錄）")
    args = parser.parse_args()
    try:
        import pg8000.dbapi as db
        from dotenv import load_dotenv
    except ImportError:
        print("缺少套件，請使用同一個 Python 執行 pip install -r requirements.txt。", file=sys.stderr)
        return 1

    try:
        load_dotenv(BASE_DIR / ".env", override=False, encoding="utf-8-sig")
        password = os.environ.get("POSTGRES_PASSWORD")
        if not password or password == "your_password_here":
            raise ValueError("請在 analytics 的 .env 或環境變數設定 POSTGRES_PASSWORD。")
        port = int(os.environ.get("PGPORT", "5432"))
        if not 1 <= port <= 65535:
            raise ValueError("PGPORT 必須介於 1 與 65535。")
        with closing(db.connect(
            host=os.environ.get("PGHOST", "localhost"),
            port=port,
            database=os.environ.get("PGDATABASE", "interviewworks"),
            user=os.environ.get("PGUSER", "postgres"),
            password=password,
            timeout=10,
            application_name="concert-ticket-analytics",
        )) as connection:
            rows = fetch_sales(connection)
        path = write_report(rows, args.output_dir)
    except ValueError as exc:
        print(f"設定或資料錯誤：{exc}", file=sys.stderr)
        return 1
    except db.Error:
        print("資料庫連線或查詢失敗，請確認 PostgreSQL 已啟動、連線設定、資料表與 SELECT 權限。",
              file=sys.stderr)
        return 1
    except OSError:
        print("無法讀取設定或寫入報表，請確認路徑與檔案權限。", file=sys.stderr)
        return 1
    print(f"已匯出 {len(rows)} 個場次：{path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
