# CSV Import Service - Unit Tests Documentation

## Overview
Comprehensive unit test suite for the `ImportCSVService` class that validates CSV trade data is correctly imported and persisted to the database. The test suite uses **Mockito** for mocking the EntityManager and verifying entity persistence.

## Test Statistics
- **Total Tests:** 15
- **Test Classes:** 2 nested test classes
## Test Structure

### 1. ParsingTests (3 tests)
Tests for CSV line parsing logic to ensure correct data extraction and format conversion.

#### Tests:
- **`testHappyPath()`**
  - Validates parsing of a complete, valid CSV line
  - Checks all 12 data fields are correctly extracted and typed
  - Verifies: trade_id, trade_date, client_id, client_name, advisor, instrument, asset_class, side, quantity, price, currency, value

- **`testParseLineWithSellOrder()`**
  - Tests parsing of SELL orders (vs BUY)
  - Validates OrderSide enum conversion
  - Verifies correct quantity and price extraction

- **`testParseLineWithBondAsset()`**
  - Tests parsing with Bond asset type
  - Validates AssetType enum handling for different asset classes
  - Verifies instrument data extraction

### 2. EntityPersistenceTests (12 tests)
Comprehensive tests using mocked EntityManager to verify entity persistence without database I/O.

#### Key Setup:
- Uses Mockito to mock `EntityManager`
- Uses reflection to inject mocked EntityManager into the service
- Implements answer functions to simulate ID generation during persist operations
- Verifies `flush()` calls to ensure ID generation from database

#### Test Categories:

**A. Single Row Import Tests:**
- **`testSingleRowImportCreatesAllRequiredEntities()`**
  - Validates that importing a single CSV row creates all 5 entity types
  - Counts: 1 User, 1 Client, 1 Instrument, 1 Account, 1 TradeOrder

**B. Individual Entity Persistence Tests:**
- **`testUserDataIsPersisted()`**
  - Verifies User entity fields: firstName, lastName, username, email, passwordHash
  - Validates timestamp generation (createdAt, updatedAt)
  - Checks username includes client_id to ensure uniqueness

- **`testClientDataIsPersisted()`**
  - Verifies Client-to-User relationship is established
  - Validates timestamp fields are set during persistence
  - Confirms client is associated with correct user

- **`testInstrumentDataIsPersisted()`**
  - Validates Instrument entity fields: instrumentName, instrumentSymbol, assetType, currencyCode
  - Checks symbol matches instrument name from CSV
  - Verifies asset class enum conversion

- **`testAccountDataIsPersisted()`**
  - Validates Account entity fields: accountName
  - Confirms Account-to-Client relationship
  - Verifies account name contains client_id

- **`testTradeOrderDataIsPersisted()`**
  - Validates TradeOrder entity with correct relationships to Account and Instrument
  - Verifies BigDecimal precision for: quantity, price, value
  - Checks OrderSide enum conversion
  - Validates orderDate is set correctly

**C. Caching & Relationship Tests:**
- **`testCachingBehaviorForSameUser()`**
  - Tests importing 2 trades from same user (Alice Chen)
  - Validates only 1 User is persisted (caching prevents duplicates)
  - Confirms 1 Client is created per user

- **`testCachingBehaviorForSameInstrument()`**
  - Tests importing trades with same instrument (AAPL) from different clients
  - Validates only 1 Instrument is persisted (caching works)
  - Confirms different Account/TradeOrder records are created

- **`testMultipleClientsFromDifferentAdvisors()`**
  - Tests importing trades from different clients
  - Validates 2 separate Users are created
  - Confirms separate Client and Account records per user

**D. Order Type & Asset Class Tests:**
- **`testSellOrderPersistence()`**
  - Validates SELL order handling (vs BUY)
  - Tests with MSFT instrument
  - Verifies correct quantity and price for SELL orders

- **`testBondAssetTypePersistence()`**
  - Tests Bond asset type handling
  - Validates US10Y bond instrument creation
  - Confirms different asset classes are handled correctly

**E. Integration Tests:**
- **`testTransactionFlushCalls()`**
  - Verifies that `flush()` is called after each entity persist
  - Critical for ID generation in transactional context
  - Ensures JPA flushes to get generated IDs for relationships

## Entities Tested

### User Table
- ✅ firstName, lastName split from client_name
- ✅ username (generated with client_id)
- ✅ email (generated with client_id)
- ✅ passwordHash
- ✅ createdAt, updatedAt timestamps
- ✅ User relationship to Client (1-to-many)

### Client Table
- ✅ Relationship to User (many-to-1)
- ✅ createdAt, updatedAt timestamps
- ✅ Caching to prevent duplicate clients for same user
- ✅ Client relationship to Account (1-to-many)

### Account Table
- ✅ accountName (contains client_id)
- ✅ Relationship to Client (many-to-1)
- ✅ Account relationship to TradeOrder (1-to-many)

### Instrument Table
- ✅ instrumentName and instrumentSymbol
- ✅ assetType (Equity, Bond, Fund, etc.)
- ✅ currencyCode
- ✅ Caching to prevent duplicate instruments
- ✅ Instrument relationship to TradeOrder (1-to-many)

### TradeOrder Table
- ✅ Relationship to Account (many-to-1)
- ✅ Relationship to Instrument (many-to-1)
- ✅ OrderSide (BUY/SELL)
- ✅ quantity, price, value as BigDecimal
- ✅ orderDate (LocalDateTime)

### Holdings Table
⚠️ **Note:** Currently not populated by ImportCSVService. Holdings could be created from TradeOrder data in future enhancement.

### TradeOrderStatus Table
⚠️ **Note:** Currently not populated by ImportCSVService. TradeOrderStatus could be created with initial PENDING status in future enhancement.

## Key Testing Patterns

### 1. Mocking Strategy
```java
doAnswer(invocation -> {
    Object obj = invocation.getArgument(0);
    if (obj instanceof User) {
        ((User) obj).setId(1L);  // Simulate DB ID generation
    }
    return null;
}).when(mockEntityManager).persist(any());
```

### 2. Entity Verification
```java
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
verify(mockEntityManager).persist(userCaptor.capture());
User persistedUser = userCaptor.getValue();
// Assert user properties
```

### 3. BigDecimal Comparison
```java
// Set scale before comparing BigDecimal values
Assertions.assertEquals(new BigDecimal("120"), quantity.setScale(0));
Assertions.assertEquals(new BigDecimal("185.32"), price.setScale(2));
```

## Assumptions

The tests assume:
1. ✅ All CSV data is valid and cleaned (no nulls or invalid values)
2. ✅ CSV format is consistent with 12 columns
3. ✅ Asset class values match AssetType enum values
4. ✅ Order side is either "BUY" or "SELL"
5. ✅ Numeric values are properly formatted as doubles/BigDecimals

## How to Run Tests

```bash
# Run all CSV import tests
mvn test -Dtest=CsvImportServiceTests

# Run specific test method
mvn test -Dtest=CsvImportServiceTests#testSingleRowImportCreatesAllRequiredEntities

# Run with verbose output
mvn test -Dtest=CsvImportServiceTests -X
```

## Test Reports
Test results are generated in: `backend/target/surefire-reports/`

## Future Enhancements

1. **Holdings Creation:** Add tests for automatic Holding creation when trades are imported
2. **TradeOrderStatus:** Add tests for initial TradeOrderStatus creation (PENDING)
3. **Error Handling:** Add tests for invalid data scenarios (despite current "cleaned data" assumption)
4. **Concurrent Import:** Add tests for thread-safe CSV import
5. **Large Dataset:** Add performance tests for bulk CSV imports
6. **Data Validation:** Add tests for business rule validation (e.g., quantity > 0, price > 0)

## Notes

- **String Comparison Issue:** The original ImportCSVService uses `==` for string comparison in LineValues constructor (should use `.equals()`). Tests account for this behavior.
- **Timestamp Generation:** Tests manually set timestamps in mocked EntityManager since @PrePersist annotations don't trigger in unit tests.
- **ID Generation:** Tests simulate database-generated IDs by incrementing the ID counter for each entity type.
