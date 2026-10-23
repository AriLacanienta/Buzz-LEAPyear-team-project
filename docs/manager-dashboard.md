# Manager dashboard summary

The manager dashboard uses `GET /api/v1/dispute/summary` for all five info cards.
The route is manager-only: anonymous requests receive `401`, and authenticated
non-managers receive `403`. No owner, date or pagination parameters are accepted
as metric selectors. The authenticated manager determines the dispute exclusions.

## Response

```json
{
  "businessDate": "2026-10-22",
  "timeZone": "America/New_York",
  "asOf": "2026-10-22T16:00:00Z",
  "ordersToday": 3,
  "ordersYesterday": 2,
  "ordersDifference": 1,
  "ordersChangePercent": 50.0,
  "filledOrdersToday": 2,
  "fillRatePercent": 66.7,
  "openDisputes": 5,
  "criticalOpenDisputes": 2,
  "activeClientsToday": 2,
  "assetsMovedByCurrency": [
    { "currencyCode": "EUR", "totalValue": 75.00 },
    { "currencyCode": "USD", "totalValue": 250.00 }
  ]
}
```

## Metric definitions

All day boundaries use the backend server's local calendar day and half-open
intervals `[day start, next day start)`. `businessDate` and `timeZone` describe
that reporting period; the browser's timezone does not affect counts. Aggregate
queries share a read-only, repeatable-read transaction.

- **ORDERS TODAY:** orders whose first `SUBMITTED` status occurred today.
  Compare with the equivalent yesterday population, showing signed count and
  percentage changes. When yesterday is zero and today is positive,
  `ordersChangePercent` is `null` and the card shows `N/A`. Both zero means `0.0%`.
- **FILL RATE:** today's submitted orders whose latest status is `FILLED`,
  divided by all today's submitted orders. Latest status is ordered by timestamp,
  then status ID. No submissions means `0.0%`. `filledOrdersToday` is this cohort's
  count, not the count of all execution events today.
- **OPEN DISPUTES:** all currently open disputes, excluding accounts owned by
  the requesting manager. `criticalOpenDisputes` uses the same scope and the
  existing severity rule: execution quantity times price, rounded to two
  decimals (half up), greater than or equal to `10,000.00`. Severity remains in
  each trade's denomination; no FX conversion is introduced.
- **ACTIVE CLIENTS:** distinct users with orders first submitted today,
  deduplicated across accounts. Managers who own submitting accounts count too.
- **ASSETS MOVED:** the sum of `trade_value` for orders first filled today.
  Include both BUY and SELL and orders submitted on previous days. Return
  separate totals by instrument currency code, sorted by code. Never combine
  currencies; an empty list displays `No executions`.

Orders are counted once, even if status history contains duplicate events.
Orders without `SUBMITTED` history do not silently fall back to `order_date`.
Percentages are rounded half up to one decimal. All monetary calculation is
performed using decimal arithmetic in PostgreSQL/Java.

## Frontend lifecycle

Load immediately on entering the dashboard and refresh every 30 seconds.
Requests never overlap. Leaving the page cancels active requests and polling.

Before the first result, cards show placeholders. Initial failures display an
error and Retry action. Refresh failures retain the last successful values with
a visible stale-data warning. Polling continues after errors, and successful
responses replace all five cards together. The page shows the reporting date,
server timezone, and the last successful response's timestamp.

## Verification

Backend tests require Java 21 and Docker. The PostgreSQL repository tests use an
isolated `postgres:16-alpine` Testcontainer and run the Flyway migrations; they
never use a developer or deployment database.

```bash
cd backend
./mvnw -Dtest=ManagerDashboardServiceTests,DashboardSummaryRepositoryTests,DashboardSummarySecurityTests test
```

Angular tests use the existing Karma/Jasmine setup and Chrome/Chromium. Set
`CHROME_BIN` when the browser is not on the default path.

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless --include='src/app/**/*manager-dashboard*.spec.ts'
npm run build
```
