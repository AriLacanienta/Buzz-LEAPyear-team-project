# API endpoint contract matrix

Status: observed from the current Spring and Angular source; this is not yet a
normative OpenAPI contract. All behaviors are subject to change.

## Scope and conventions

- Base path: `/api/v1`.
- Authentication is stateless JWT. The frontend
  [`AuthInterceptor`](../frontend/src/app/interceptors/auth.interceptor.ts)
  adds the authorization header to authenticated requests.
- `401` is emitted by Spring Security when authentication is absent or the JWT
  cannot be established. `403` is expected when the principal lacks a required
  role.
- [`ErrorResponse`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/ErrorResponse.java)
  is the reusable error body for endpoints that return structured errors. It
  contains `code` (integer HTTP status), `message`, and `details`.
- [`GlobalExceptionHandler`](../backend/src/main/java/com/buzzleapyear/trading_api/exception/GlobalExceptionHandler.java)
  formats not-found exceptions, unmapped resources, and DTO validation failures
  as `ResponseEntity<ErrorResponse>`.
- Unless an endpoint documents an error body, the controller returns an empty
  response body for that status.
- `Page<T>` is Spring Data's paged JSON object (`content`, `pageable`,
  `totalElements`, `totalPages`, and related metadata), not a bare array.
- “No consuming call site found” means no frontend request or consumer was
  found in `frontend/src`; external clients may still use the endpoint.

## Combined endpoint and frontend call-site matrix

| Backend endpoint | Frontend consumer | Request DTO and parameters | Success response | Explicit errors | Other/implicit errors |
|---|---|---|---|---|---|
| [`POST /auth/login`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AuthController.java) | [`AuthService.login`](../frontend/src/app/services/auth.service.ts); [`LoginComponent`](../frontend/src/app/pages/login/login.component.ts) | [`LoginRequest`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/LoginRequest.java): `email`, `password` | `200` [`AuthResponse`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/AuthResponse.java): `token`, `type`, `username`, `email`, `roles[]` | `401` [`ErrorResponse`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/ErrorResponse.java): invalid credentials or authentication failure | No bean validation. Malformed JSON, missing fields, or deserialization errors are normally framework `400` responses. |
| [`POST /auth/register`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AuthController.java) | [`AuthService.register`](../frontend/src/app/services/auth.service.ts); [`RegisterComponent`](../frontend/src/app/pages/register/register.component.ts) | [`RegistrationRequest`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/RegistrationRequest.java): `username`, `email`, `firstName`, `lastName`, `password` | `201` [`AuthResponse`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/AuthResponse.java), with `CLIENT` role and newly created client/account | `400` [`ErrorResponse`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/ErrorResponse.java): duplicate username/email or registration failure | No bean validation. Also creates an account with `10000.00` cash. No controller transaction boundary. |
| [`GET /auth/validate`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AuthController.java) | [`AuthService.validateToken`](../frontend/src/app/services/auth.service.ts); [`authGuard`](../frontend/src/app/auth.guard.ts) | No DTO; required `Authorization` header with bearer scheme | `200` `ValidationResponse`: `valid`, `data` (`username`) | `401` `ValidationResponse`: invalid format, invalid/expired token, or validation failure | Missing header can be rejected by Spring before controller execution. |
| [`GET /accounts/me`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AccountController.java) | [`AccountService.getMyAccount`](../frontend/src/app/services/account.service.ts); [`AccountComponent`](../frontend/src/app/pages/account/account.component.ts); [`DashboardComponent`](../frontend/src/app/pages/dashboard/dashboard.component.ts); [`BalanceComponent`](../frontend/src/app/components/trade/balance/balance.component.ts); [`BuySellComponent`](../frontend/src/app/components/trade/buy-sell/buy-sell/buy-sell.component.ts); [`CurrentHoldingsComponent`](../frontend/src/app/components/trade/current-holdings/current-holdings/current-holdings.component.ts); [`RecentOrdersComponent`](../frontend/src/app/components/trade/recent-orders/recent-orders/recent-orders.component.ts) | No DTO; principal username from JWT | `200` [`CurrentAccountResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/CurrentAccountResponseDto.java): `accountId`, `accountName` | `404` user/accounts not found; `409` multiple accounts found | Other repository/service failures are uncaught and framework-rendered. |
| [`GET /accounts/{id}`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AccountController.java) | [`AccountService.getAccountBalance`](../frontend/src/app/services/account.service.ts); [`BalanceComponent`](../frontend/src/app/components/trade/balance/balance.component.ts); [`BuySellComponent`](../frontend/src/app/components/trade/buy-sell/buy-sell/buy-sell.component.ts); [`DashboardComponent`](../frontend/src/app/pages/dashboard/dashboard.component.ts) | No DTO; path `id` is declared `String` and parsed to `Long` | `200` [`AccountValueResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/AccountValueResponseDto.java): `cashAvailable`, `cashReserved`, `portfolioValue` | `404` absent/not-owned account, or null holdings | Non-numeric `id` produces framework conversion error; other failures are not normalized. |
| [`GET /accounts/{id}/tradeorders`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/AccountController.java) | [`AccountService.getRecentTradeOrders`](../frontend/src/app/services/account.service.ts); [`RecentOrdersComponent`](../frontend/src/app/components/trade/recent-orders/recent-orders/recent-orders.component.ts) | No DTO; path `id` (`Long`); query `page` default `0`, `size` default `10`, max `100` | `200` `Page<RecentTradeOrderResponseDto>`: order id, symbol, side, quantity, price, order date, latest status, update time, reason | `400` invalid page/size; `404` absent/not-owned account | Spring `Pageable` conventions differ from this endpoint's explicit validation. |
| [`GET /holdings/{accountId}`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/HoldingController.java) | [`HoldingService.getHoldingsByAccountId`](../frontend/src/app/services/holding.service.ts); [`DashboardComponent`](../frontend/src/app/pages/dashboard/dashboard.component.ts); [`CurrentHoldingsComponent`](../frontend/src/app/components/trade/current-holdings/current-holdings/current-holdings.component.ts); [`BuySellComponent`](../frontend/src/app/components/trade/buy-sell/buy-sell/buy-sell.component.ts) | No DTO; path `accountId` (`Long`) | `200` [`HoldingResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/HoldingResponseDto.java)[] | `404` absent/not-owned account | Invalid path conversion and service failures are framework responses. Frontend uses `any[]`. `getAllHoldings()` calls the nonexistent `/holdings/holdings` and has no consumer. |
| [`GET /instruments/{symbol}/details`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/InstrumentController.java) | **No consuming call site found** | No DTO; path `symbol` | `200` [`InstrumentDetailsResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/InstrumentDetailsResponseDto.java) | `404` symbol not found | No documented symbol normalization or validation. |
| [`GET /quote`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/QuoteController.java) | **No consuming call site found** | No DTO; no parameters | `200` [`QuoteResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/QuoteResponseDto.java)[] | None explicitly declared | Instruments without in-memory quotes are silently omitted; per-instrument quote errors are swallowed. |
| [`GET /quote/{symbol}`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/QuoteController.java) | [`InstrumentService.getQuote`](../frontend/src/app/services/instrument.service.ts); [`SearchComponent`](../frontend/src/app/components/trade/buy-sell/search/search.component.ts) | No DTO; path `symbol` | `200` [`QuoteResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/QuoteResponseDto.java) | `404` unknown symbol or no current quote | No documented symbol normalization or validation; frontend converts errors to `EMPTY`. |
| [`GET /quote/search`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/QuoteController.java) | [`InstrumentService.searchInstruments`](../frontend/src/app/services/instrument.service.ts); [`SearchComponent`](../frontend/src/app/components/trade/buy-sell/search/search.component.ts) | No DTO; required query `q`; Spring `Pageable` (`page`, `size`, `sort`) | `200` `Page<InstrumentSearchResponseDto>` | None explicitly declared | Empty query and invalid pageable values rely on framework/service behavior. Frontend maps only `page.content`. |
| [`GET /quote/market`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/QuoteController.java) | [`InstrumentService.getInstruments`](../frontend/src/app/services/instrument.service.ts); [`MarketsComponent`](../frontend/src/app/pages/markets/markets.component.ts); [`InstrumentService.getTopGainers`](../frontend/src/app/services/instrument.service.ts); [`InstrumentService.getTopLosers`](../frontend/src/app/services/instrument.service.ts) | No DTO; required enum query `assetType`; optional `market`; Spring `Pageable` | `200` `Page<ListMarketEquitySummaryResponseDto>` | Invalid/missing `assetType` is framework `400` | `market` maps `US→USD`, `UK→GBP`, `INDIA/INDIAN→INR`; unknown values silently remove currency filtering. |
| [`POST /tradeorders/preview`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/TradeOrderController.java) | [`TradeOrderService.previewOrder`](../frontend/src/app/services/trade-order.service.ts); **No consuming call site found** | [`TradeOrderPreviewRequestDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderPreviewRequestDto.java): `side`, `instrumentSymbol`, positive `quantity`, `accountId` | `200` [`TradeOrderPreviewResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderPreviewResponseDto.java) | `404` absent/not-owned account or absent instrument | Bean validation failures are framework `400`. Business-invalid orders are `200` with `valid:false`, including no quote. |
| [`POST /tradeorders`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/TradeOrderController.java) | [`TradeOrderService.submitOrder`](../frontend/src/app/services/trade-order.service.ts); **No consuming call site found** | [`TradeOrderDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderDTO.java): `side`, `instrumentSymbol`, positive `quantity`, positive `price`, `accountId` | `202` [`TradeOrderResponseDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderResponseDTO.java): order id, `SUBMITTED`, status message | `400` caught `IllegalArgumentException`; `404` absent/not-owned account; `500` other caught exception | Bean validation failures are framework `400`. Processing is asynchronous after persistence; later failures appear in order status. |
| [`GET /tradeorders/{orderId}`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/TradeOrderController.java) | [`TradeOrderService.getOrderStatus`](../frontend/src/app/services/trade-order.service.ts); [`BuySellComponent`](../frontend/src/app/components/trade/buy-sell/buy-sell/buy-sell.component.ts) | No DTO; path `orderId` (`Long`) | `200` [`OrderStatusDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/OrderStatusDTO.java) | `404` missing/unauthorized order or missing status; `500` caught retrieval failure | Invalid path conversion relies on framework behavior; frontend converts retrieval errors to `null`. |
| [`GET /tradeorders/accounts/{accountId}/orders`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/TradeOrderController.java) | [`RecentOrderService.getRecentOrdersByAccountId`](../frontend/src/app/services/recent-order.service.ts); [`DashboardComponent`](../frontend/src/app/pages/dashboard/dashboard.component.ts) | No DTO; path `accountId` (`Long`); query `limit` default `20` | `200` [`RecentOrderResponseDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/RecentOrderResponseDTO.java)[] | None explicitly declared | Authenticated only: no ownership check. `limit` is not validated; negative values return an empty list and very large values are accepted. |
| [`GET /user/allUsers`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/UserController.java) | [`UserService.getAllUsers`](../frontend/src/app/services/user.service.ts); [`AdminComponent`](../frontend/src/app/pages/admin/admin.component.ts) | No DTO; no parameters | `200` [`UserResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/UserResponseDto.java)[] | `401` unauthenticated; `403` without `MANAGER` | Current source has a DTO constructor mismatch in `UserService`; frontend also expects an `id` field absent from the DTO declaration. |
| [`PUT /user/{userId}`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/UserController.java) | **No consuming call site found** | No DTO; path `userId` (`int`); required query `role` | Declared `void`; intended success status is unspecified | Always throws `UnsupportedOperationException("Not yet implemented")`; invalid/missing parameters can be framework `400` | Any authenticated role is allowed by security; this is a stub, not a usable role-management contract. |
| [`GET /test`](../backend/src/main/java/com/buzzleapyear/trading_api/controller/TestController.java) | **No consuming call site found** | No DTO; no parameters | `200` plain `String` | `401` unauthenticated; `403` without `MANAGER` | Operational/test endpoint is exposed under the versioned API and has no production frontend consumer. |

## DTO and enum inventory

| DTO | Fields / meaning |
|---|---|
| [`AuthResponse`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/AuthResponse.java) | `token`, `type`, `username`, `email`, `roles[]` |
| [`LoginRequest`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/LoginRequest.java) | `email`, `password` |
| [`RegistrationRequest`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/RegistrationRequest.java) | `username`, `email`, `firstName`, `lastName`, `password` |
| [`CurrentAccountResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/CurrentAccountResponseDto.java) | `accountId`, `accountName` |
| [`AccountValueResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/AccountValueResponseDto.java) | `cashAvailable`, `cashReserved`, `portfolioValue` |
| [`HoldingResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/HoldingResponseDto.java) | Holding and valuation fields |
| [`QuoteResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/QuoteResponseDto.java) | Quote, market, volume, and timestamp fields |
| [`InstrumentDetailsResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/InstrumentDetailsResponseDto.java) | Instrument name, symbol, asset type, currency |
| [`InstrumentSearchResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/InstrumentSearchResponseDto.java) | Instrument identity plus current price/change |
| [`ListMarketEquitySummaryResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/ListMarketEquitySummaryResponseDto.java) | Market listing and quote summary |
| [`TradeOrderPreviewRequestDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderPreviewRequestDto.java) | Preview side, symbol, quantity, account |
| [`TradeOrderPreviewResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderPreviewResponseDto.java) | Pricing, account impact, validity, and reason |
| [`TradeOrderDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderDTO.java) | Submitted side, symbol, quantity, price, account |
| [`TradeOrderResponseDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/TradeOrderResponseDTO.java) | Order id, status, status message |
| [`OrderStatusDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/OrderStatusDTO.java) | Order/account status and update timestamp |
| [`RecentTradeOrderResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/RecentTradeOrderResponseDto.java) | Paged order history with status |
| [`RecentOrderResponseDTO`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/RecentOrderResponseDTO.java) | Recent order history with client/asset details |
| [`UserResponseDto`](../backend/src/main/java/com/buzzleapyear/trading_api/dto/UserResponseDto.java) | User identity and roles |

## Gaps, inconsistencies, and overlaps

1. `GET /tradeorders/accounts/{accountId}/orders` has no account ownership
   check, unlike the other account-scoped trade endpoints.
2. `PUT /user/{userId}` is unrestricted to manager roles, unimplemented, and
   has no frontend consumer.
3. Auth and global exception handling now use the reusable `ErrorResponse` for
   structured authentication, registration, not-found, and DTO validation
   failures. Other endpoint-specific error branches should migrate to the same
   schema.
4. Validation and pagination are inconsistent: trade bodies use Jakarta
   validation, auth/query/path inputs generally do not, and pagination limits
   differ by endpoint.
5. Preview models business invalidity as `200` with `valid:false`, while order
   submission uses `400` for caught business exceptions.
6. Paged account history and unpaged recent-order history expose overlapping
   but incompatible DTOs. The dashboard uses the less-safe unscoped route.
7. `/quote/search`, `/quote/market`, and `/quote/{symbol}` overlap in quote and
   instrument data but have separate response models and undocumented symbol
   normalization.
8. Frontend integration gaps include unused instrument-details and all-quotes
   routes, unused preview/submission methods, and the incorrect
   `/holdings/holdings` helper path.
9. Frontend models are not fully aligned: `UserResponse` expects `id`,
   `InstrumentService.QuoteResponse` uses `name`, and several services use
   `any` rather than DTO-shaped models.
10. The source/build mismatch in `UserService` and `UserResponseDto` must be
    resolved before generating or validating OpenAPI.

## Recommended OpenAPI source-of-truth decisions

1. Define a common `ErrorResponse` with `code`, `message`, `details`, and
   `traceId`.
2. Define a reusable bearer security scheme and operation-level role metadata.
3. Standardize page parameters and collection limits.
4. Decide whether preview invalidity remains a `200` result or becomes a `4xx`.
5. Consolidate or explicitly distinguish the two order-history projections and
   the overlapping quote summaries.
6. Remove or mark `/test` and the unimplemented role route as internal.
7. Resolve the user DTO id mismatch and align frontend models before generating
   the contract.
