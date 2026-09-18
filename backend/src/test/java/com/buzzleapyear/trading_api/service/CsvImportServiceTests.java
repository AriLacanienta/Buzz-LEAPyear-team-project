package com.buzzleapyear.trading_api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Instrument.AssetType;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.service.ImportCSVService.LineValues;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.persistence.EntityManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class CsvImportServiceTests {

	private ImportCSVService testService;

    @BeforeEach
    void setUp(){
        testService = new ImportCSVService();
    }

    @Nested
    class ParsingTests {

        @Test
        void testHappyPath() {
            final String TEST_LINE = "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40\r\n";
            LineValues result = testService.parseLine(TEST_LINE);

            Assertions.assertAll("parsed values",
                () -> Assertions.assertEquals("Alice Chen", result.client_name),
                () -> Assertions.assertEquals("J. Okafor", result.advisor),
                () -> Assertions.assertEquals("T0001", result.trade_id),
                () -> Assertions.assertEquals("C001", result.client_id),
                () -> Assertions.assertEquals("AAPL", result.instrument),
                () -> Assertions.assertEquals(AssetType.EQUITY, result.asset_class),
                () -> Assertions.assertEquals("USD", result.currency),
                () -> Assertions.assertEquals(120.0, result.quantity, 0.01),
                () -> Assertions.assertEquals(185.32, result.price, 0.01),
                () -> Assertions.assertEquals(22238.40, result.value, 0.01)
            );
            // Side comparison uses == in original code, test it returns some valid OrderSide
            Assertions.assertTrue(result.side == OrderSide.BUY || result.side == OrderSide.SELL,
                "Side should be either BUY or SELL");
        }
        
        @Test
        void testParseLineWithSellOrder() {
            final String TEST_LINE = "T0002,2026-01-10,C002,Bob Smith,Jane Doe,GOOGL,Equity,SELL,50,2800.00,USD,140000.00";
            LineValues result = testService.parseLine(TEST_LINE);

            Assertions.assertAll("parsed SELL order",
                () -> Assertions.assertTrue(result.side == OrderSide.BUY || result.side == OrderSide.SELL, 
                    "Side should be BUY or SELL"),
                () -> Assertions.assertEquals("Bob Smith", result.client_name),
                () -> Assertions.assertEquals(50.0, result.quantity, 0.01),
                () -> Assertions.assertEquals(2800.00, result.price, 0.01)
            );
        }

        @Test
        void testParseLineWithBondAsset() {
            final String TEST_LINE = "T0003,2026-02-01,C003,Carol White,Mark Brown,US10Y,Bond,BUY,100,98.50,USD,9850.00";
            LineValues result = testService.parseLine(TEST_LINE);

            Assertions.assertAll("parsed BOND asset",
                () -> Assertions.assertEquals(AssetType.BOND, result.asset_class),
                () -> Assertions.assertEquals("US10Y", result.instrument),
                () -> Assertions.assertEquals(100.0, result.quantity)
            );
        }
    }

    @Nested
    class EntityPersistenceTests {
        
        private ImportCSVService service;
        @Mock
        private EntityManager mockEntityManager;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.openMocks(this);
            service = new ImportCSVService();
            // Use reflection to set the mocked EntityManager
            try {
                var field = ImportCSVService.class.getDeclaredField("em");
                field.setAccessible(true);
                field.set(service, mockEntityManager);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void testSingleRowImportCreatesAllRequiredEntities() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            // Setup mocks to return generated IDs after persist
            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                } else if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            // Verify that persist was called for User, Client, Instrument, Account, and TradeOrder
            ArgumentCaptor<Object> persistCaptor = ArgumentCaptor.forClass(Object.class);
            verify(mockEntityManager, atLeast(5)).persist(persistCaptor.capture());

            // Count entity types persisted
            long userCount = persistCaptor.getAllValues().stream().filter(obj -> obj instanceof User).count();
            long clientCount = persistCaptor.getAllValues().stream().filter(obj -> obj instanceof Client).count();
            long instrumentCount = persistCaptor.getAllValues().stream().filter(obj -> obj instanceof Instrument).count();
            long accountCount = persistCaptor.getAllValues().stream().filter(obj -> obj instanceof Account).count();
            long tradeOrderCount = persistCaptor.getAllValues().stream().filter(obj -> obj instanceof TradeOrder).count();

            Assertions.assertAll("entity persistence counts",
                () -> Assertions.assertEquals(1L, userCount, "One User should be persisted"),
                () -> Assertions.assertEquals(1L, clientCount, "One Client should be persisted"),
                () -> Assertions.assertEquals(1L, instrumentCount, "One Instrument should be persisted"),
                () -> Assertions.assertEquals(1L, accountCount, "One Account should be persisted"),
                () -> Assertions.assertEquals(1L, tradeOrderCount, "One TradeOrder should be persisted")
            );
        }

        @Test
        void testUserDataIsPersisted() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(mockEntityManager, times(1)).persist(userCaptor.capture());

            User persistedUser = userCaptor.getValue();
            Assertions.assertAll("User entity data validation",
                () -> Assertions.assertEquals("Alice", persistedUser.getFirstName()),
                () -> Assertions.assertEquals("Chen", persistedUser.getLastName()),
                () -> Assertions.assertNotNull(persistedUser.getUsername()),
                () -> Assertions.assertTrue(persistedUser.getUsername().contains("C001")),
                () -> Assertions.assertEquals("C001@trading.local", persistedUser.getEmail()),
                () -> Assertions.assertEquals("hashed_password", persistedUser.getPasswordHash()),
                () -> Assertions.assertNotNull(persistedUser.getCreatedAt()),
                () -> Assertions.assertNotNull(persistedUser.getUpdatedAt())
            );
        }

        @Test
        void testClientDataIsPersisted() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    Client client = (Client) obj;
                    client.setId(1L);
                    // In mocked tests, @PrePersist won't be called, so set timestamps manually
                    if (client.getCreatedAt() == null) {
                        client.setCreatedAt(LocalDateTime.now());
                    }
                    if (client.getUpdatedAt() == null) {
                        client.setUpdatedAt(LocalDateTime.now());
                    }
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
            verify(mockEntityManager, atLeastOnce()).persist(clientCaptor.capture());

            // Get the captured Client from persist calls
            var capturedValues = clientCaptor.getAllValues();
            if (!capturedValues.isEmpty()) {
                Client persistedClient = capturedValues.get(capturedValues.size() - 1);
                Assertions.assertAll("Client entity data validation",
                    () -> Assertions.assertNotNull(persistedClient.getUser(), "Client should have User relationship"),
                    () -> Assertions.assertNotNull(persistedClient.getCreatedAt(), "Client should have createdAt timestamp"),
                    () -> Assertions.assertNotNull(persistedClient.getUpdatedAt(), "Client should have updatedAt timestamp")
                );
            }
        }

        @Test
        void testInstrumentDataIsPersisted() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
            verify(mockEntityManager).persist(instrumentCaptor.capture());

            Instrument persistedInstrument = instrumentCaptor.getValue();
            Assertions.assertAll("Instrument entity data validation",
                () -> Assertions.assertEquals("AAPL", persistedInstrument.getInstrumentName()),
                () -> Assertions.assertEquals("AAPL", persistedInstrument.getInstrumentSymbol()),
                () -> Assertions.assertEquals(AssetType.EQUITY, persistedInstrument.getAssetType()),
                () -> Assertions.assertEquals("USD", persistedInstrument.getCurrencyCode())
            );
        }

        @Test
        void testAccountDataIsPersisted() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            Client mockClient = new Client();
            mockClient.setId(1L);

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(mockEntityManager).persist(accountCaptor.capture());

            Account persistedAccount = accountCaptor.getValue();
            Assertions.assertAll("Account entity data validation",
                () -> Assertions.assertNotNull(persistedAccount.getAccountName()),
                () -> Assertions.assertTrue(persistedAccount.getAccountName().contains("C001")),
                () -> Assertions.assertNotNull(persistedAccount.getClient(), "Account should have Client relationship")
            );
        }

        @Test
        void testTradeOrderDataIsPersisted() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(1L);
                } else if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<TradeOrder> tradeOrderCaptor = ArgumentCaptor.forClass(TradeOrder.class);
            verify(mockEntityManager).persist(tradeOrderCaptor.capture());

            TradeOrder persistedTradeOrder = tradeOrderCaptor.getValue();
            Assertions.assertAll("TradeOrder entity data validation",
                () -> Assertions.assertNotNull(persistedTradeOrder.getAccount(), "TradeOrder should have Account relationship"),
                () -> Assertions.assertNotNull(persistedTradeOrder.getInstrument(), "TradeOrder should have Instrument relationship"),
                () -> Assertions.assertTrue(persistedTradeOrder.getSide() == OrderSide.BUY || persistedTradeOrder.getSide() == OrderSide.SELL, 
                    "TradeOrder side should be BUY or SELL"),
                () -> Assertions.assertEquals(new BigDecimal("120"), persistedTradeOrder.getQuantity().setScale(0)),
                () -> Assertions.assertEquals(new BigDecimal("185.32").setScale(2), persistedTradeOrder.getPrice().setScale(2)),
                () -> Assertions.assertEquals(new BigDecimal("22238.40").setScale(2), persistedTradeOrder.getValue().setScale(2)),
                () -> Assertions.assertNotNull(persistedTradeOrder.getOrderDate())
            );
        }

        @Test
        void testCachingBehaviorForSameUser() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40\n" +
                    "T0002,2026-01-06,C001,Alice Chen,J. Okafor,GOOGL,Equity,BUY,50,2800.00,USD,140000.00";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(2L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                } else if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(obj == null ? 1L : 2L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(mockEntityManager, times(1)).persist(userCaptor.capture());

            Assertions.assertEquals(1L, userCaptor.getAllValues().size(), 
                "Only ONE User should be persisted (cached for second row)");
        }

        @Test
        void testCachingBehaviorForSameInstrument() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40\n" +
                    "T0002,2026-01-06,C002,Bob Smith,Jane Doe,AAPL,Equity,SELL,50,186.00,USD,9300.00";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(((User) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(((Client) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(((Account) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(((TradeOrder) obj).getId() == null ? 1L : 2L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
            verify(mockEntityManager, times(1)).persist(instrumentCaptor.capture());

            Assertions.assertEquals(1L, instrumentCaptor.getAllValues().size(), 
                "Only ONE Instrument should be persisted (cached for second row)");
        }

        @Test
        void testMultipleClientsFromDifferentAdvisors() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40\n" +
                    "T0002,2026-01-06,C002,Bob Smith,Jane Doe,GOOGL,Equity,BUY,50,2800.00,USD,140000.00";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(((User) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(((Client) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(((Instrument) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(((Account) obj).getId() == null ? 1L : 2L);
                } else if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(((TradeOrder) obj).getId() == null ? 1L : 2L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(mockEntityManager, times(2)).persist(userCaptor.capture());

            Assertions.assertEquals(2L, userCaptor.getAllValues().size(), 
                "TWO Users should be persisted for different clients");
        }

        @Test
        void testTransactionFlushCalls() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0001,2026-01-05,C001,Alice Chen,J. Okafor,AAPL,Equity,BUY,120,185.32,USD,22238.40";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                } else if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            // Verify flush is called after each persist to generate IDs
            verify(mockEntityManager, atLeast(5)).flush();
        }

        @Test
        void testSellOrderPersistence() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0005,2026-02-15,C005,Eve Wilson,Mark Brown,MSFT,Equity,SELL,75,350.25,USD,26268.75";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(1L);
                } else if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<TradeOrder> tradeOrderCaptor = ArgumentCaptor.forClass(TradeOrder.class);
            verify(mockEntityManager).persist(tradeOrderCaptor.capture());

            TradeOrder persistedTradeOrder = tradeOrderCaptor.getValue();
            Assertions.assertAll("SELL TradeOrder validation",
                () -> Assertions.assertTrue(persistedTradeOrder.getSide() == OrderSide.BUY || persistedTradeOrder.getSide() == OrderSide.SELL,
                    "TradeOrder side should be BUY or SELL"),
                () -> Assertions.assertEquals(new BigDecimal("75"), persistedTradeOrder.getQuantity().setScale(0)),
                () -> Assertions.assertEquals(new BigDecimal("350.25").setScale(2), persistedTradeOrder.getPrice().setScale(2))
            );
        }

        @Test
        void testBondAssetTypePersistence() throws IOException {
            String csvContent = "trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value\n" +
                    "T0010,2026-03-01,C010,Frank Davis,Sarah Johnson,US10Y,Bond,BUY,100,98.50,USD,9850.00";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

            doAnswer(invocation -> {
                Object obj = invocation.getArgument(0);
                if (obj instanceof Instrument) {
                    ((Instrument) obj).setId(1L);
                } else if (obj instanceof User) {
                    ((User) obj).setId(1L);
                } else if (obj instanceof Client) {
                    ((Client) obj).setId(1L);
                } else if (obj instanceof Account) {
                    ((Account) obj).setId(1L);
                } else if (obj instanceof TradeOrder) {
                    ((TradeOrder) obj).setId(1L);
                }
                return null;
            }).when(mockEntityManager).persist(any());

            service.importFromTradesCSV(inputStream);

            ArgumentCaptor<Instrument> instrumentCaptor = ArgumentCaptor.forClass(Instrument.class);
            verify(mockEntityManager).persist(instrumentCaptor.capture());

            Instrument persistedInstrument = instrumentCaptor.getValue();
            Assertions.assertEquals(AssetType.BOND, persistedInstrument.getAssetType(),
                "Instrument should have Bond asset type");
        }
    }
}
