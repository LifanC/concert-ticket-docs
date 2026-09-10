import csv
import tempfile
import sqlite3
import unittest
from decimal import Decimal
from pathlib import Path
from unittest.mock import Mock

from analyze import SALES_SQL, fetch_sales, write_report


class SalesReportTests(unittest.TestCase):
    def test_sales_query_excludes_unpaid_cancelled_expired_and_refunded_orders(self):
        connection = sqlite3.connect(':memory:')
        self.addCleanup(connection.close)
        connection.execute("ATTACH DATABASE ':memory:' AS interviewworks_ticket")
        connection.execute('CREATE TABLE interviewworks_ticket.session (id TEXT)')
        connection.execute('CREATE TABLE interviewworks_ticket.ticket (orderno TEXT, session_id TEXT, status TEXT, payprice NUMERIC)')
        connection.executemany('INSERT INTO interviewworks_ticket.session VALUES (?)', [('S1',), ('S2',)])
        connection.executemany('INSERT INTO interviewworks_ticket.ticket VALUES (?, ?, ?, ?)', [
            ('paid', 'S1', 'PAID', 1200), ('free', 'S1', 'PAID', 0),
            ('pending', 'S1', 'PENDING_PAYMENT', 9000), ('cancelled', 'S1', 'CANCELLED', 9000),
            ('expired', 'S1', 'EXPIRED', 9000), ('refunded', 'S1', 'REFUNDED', 9000),
        ])
        self.assertEqual(connection.execute(SALES_SQL).fetchall(), [('S1', 2, 1200, 0), ('S2', 0, 0, 0)])

    def connection(self, rows):
        connection = Mock()
        connection.cursor.return_value.fetchall.return_value = rows
        return connection

    def test_missing_paid_amount_fails_instead_of_understating_revenue(self):
        connection = self.connection([('S1', 2, Decimal('100'), 1)])
        with self.assertRaisesRegex(ValueError, 'payprice'):
            fetch_sales(connection)
        connection.cursor.return_value.close.assert_called_once()

    def test_zero_sales_and_free_tickets_are_valid(self):
        connection = self.connection([('S1', 0, Decimal('0'), 0), ('S2', 1, Decimal('0'), 0)])
        self.assertEqual(fetch_sales(connection), [('S1', 0, Decimal('0')), ('S2', 1, Decimal('0'))])
        statements = [call.args[0] for call in connection.cursor.return_value.execute.call_args_list]
        self.assertEqual(statements[0], 'SET TRANSACTION READ ONLY')

    def test_csv_keeps_decimal_precision_and_excel_encoding(self):
        with tempfile.TemporaryDirectory() as directory:
            path = write_report([('S1', 2, Decimal('9007199254740993.25'))], Path(directory))
            self.assertTrue(path.read_bytes().startswith(b'\xef\xbb\xbf'))
            with path.open(encoding='utf-8-sig', newline='') as file:
                rows = list(csv.reader(file))
            self.assertEqual(rows[1], ['S1', '2', '9007199254740993.25'])

    def test_csv_neutralizes_formula_identifiers(self):
        prefixes = ['=', '+', '-', '@', '\t', '\r', '\n']
        with tempfile.TemporaryDirectory() as directory:
            path = write_report([(prefix + 'S1', 0, Decimal('0')) for prefix in prefixes], Path(directory))
            with path.open(encoding='utf-8-sig', newline='') as file:
                rows = list(csv.reader(file))[1:]
            self.assertEqual([row[0] for row in rows], ["'" + prefix + 'S1' for prefix in prefixes])

    def test_empty_report_has_headers_and_does_not_overwrite_previous_export(self):
        with tempfile.TemporaryDirectory() as directory:
            first = write_report([], Path(directory))
            second = write_report([], Path(directory))
            self.assertNotEqual(first, second)
            with first.open(encoding='utf-8-sig', newline='') as file:
                self.assertEqual(list(csv.reader(file)), [['場次編號', '已付款訂單數', '已付款訂單金額']])


if __name__ == '__main__':
    unittest.main()
