# Manager disputes page

The manager disputes page retrieves server-paginated data from
`GET /api/v1/dispute/search`. It uses the existing authenticated HTTP client.
The `/api/v1/dispute` base route is for creation, not listing.

The Search form exposes these listing parameters:

- `accountId`: optional positive whole account ID.
- `status`: `OPEN` or `RESOLVED`; omit for all statuses.
- `fromDate`, `toDate`: inclusive creation-time bounds, sent as server-local
  ISO date-times without browser timezone conversion.
- `minTradeValue`, `maxTradeValue`: optional non-negative inclusive bounds.
- `sortBy`: `createdAt`, `status`, or `tradeValue`.
- `sortDirection`: `asc` or `desc`.
- `size`: 10, 20, 50, or 100 results.
- `page`: zero-based, controlled by Previous/Next.

The page defaults to OPEN disputes, page 0, size 20, newest first. The status
selector offers Open, Resolved and All. Clear filters restores Open and page 0.
Search returns to page 0. Paging keeps the last applied filters, not unsaved form edits.
Filtering and pagination run on the server, not just the current page.
New searches cancel obsolete requests. Loading, empty results, failures and
retry are shown explicitly; failed requests do not masquerade as empty results.

The endpoint does not offer free-text or severity filtering.
No unsupported client-only filters are added.
Trade values have no currency symbol because the dispute response does not
include a currency code. Messages are rendered as text, not HTML.

Rows start collapsed, showing only dispute ID, client name, status, severity,
the first 50 message characters (with `...` appended only for longer messages),
and the resolved date or "Not resolved". Clicking a summary row toggles its
details; the dispute ID button also supports keyboard activation and announces
its expanded state. Expanded details show every response field, including the
full message. Multiple rows can be expanded independently. A new search,
page change, or retry collapses all details.

## Client details

The client-detail section embeds the same disputes table with `[userId]` set to
the selected client's user ID. This is not an account ID: the optional positive
`userId` search parameter scopes results to all accounts belonging to that user.
When both `userId` and `accountId` are supplied, both constraints apply.
Non-manager searches remain restricted to the authenticated owner's disputes.

The embedded table includes both open and resolved disputes, sorted on the
server by `status` ascending (OPEN first), then by ID for stable pagination.
Filter and sort controls and the duplicate page heading are hidden; pagination,
expansion, retry, and resolution actions remain available. Client changes reset
to page zero, clear old rows, and cancel obsolete requests. Paging and refresh
after resolution preserve the user scope and ordering. The standalone manager
disputes page retains its existing defaults and visible filters.

## Resolving disputes

Open rows have a Resolve action, separate from row expansion. Already-resolved
rows do not show this action. The modal contains required resolution text and
Cancel/Resolve buttons below it on the right. Cancel, Escape, or a backdrop click
discards the draft without submitting while idle. Submission blocks dismissal
and duplicate requests until it finishes. A failed request keeps the text,
shows an error, and allows retry.

The authenticated request is `PATCH /api/v1/dispute/{id}/resolve` with a required
JSON body, for example:

```json
{ "resolutionMessage": "Reviewed the execution and corrected the issue." }
```

The former bodyless request is no longer accepted. Text must contain
non-whitespace content and be at most 2000 characters. It is trimmed and stored
separately from the original dispute message. Responses include nullable
`resolutionMessage`, shown as plain text in expanded details; older records
without text show "No resolution message". The additive Flyway migration
preserves those records.

Only managers can resolve disputes, and they cannot resolve their own account's
disputes. Repeating resolution preserves the first saved message and timestamp.
Concurrent write conflicts remain errors, not successful overwrites.

After success, the modal closes and the current page reloads using applied
filters, sorting and page size. Unsubmitted filter edits are not applied.
If the last matching row disappears from the current page, the listing loads
the last valid page (or page zero when no results remain).

Creating disputes is outside this change.

Focused verification:

```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless --include='src/app/**/*dispute*.spec.ts' --include='src/app/**/client-detail.component.spec.ts'
npm run build
```

Backend API and persistence checks (from the repository root):

```bash
mvn -B -f backend/pom.xml -Dtest=DisputeControllerTests,DisputeServiceTests,DashboardSummaryRepositoryTests test
```

The repository tests require Docker for their disposable PostgreSQL database.
Frontend execution requires an installed Chrome/Chromium binary (set
`CHROME_BIN` if it is not on the usual path).
