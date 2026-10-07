package com.buzzleapyear.trading_api.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.Holding;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Instrument.AssetType;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus.OrderStatus;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.entity.Quote;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.HoldingRepository;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class ImportCSVService {

    @PersistenceContext 
    private EntityManager em;
    
    private final InstrumentRepository instrumentRepository;
    private final HoldingRepository holdingRepository;
    
    public ImportCSVService(
            InstrumentRepository instrumentRepository,
            HoldingRepository holdingRepository) {
        this.instrumentRepository = instrumentRepository;
        this.holdingRepository = holdingRepository;
    }

    @Transactional
    public void importFromTradesCSV(InputStream csvInputStream) throws IOException {        
        // Cache to track already-created users and clients to avoid duplicates
        Map<String, User> userCache = new HashMap<>();
        Map<String, Client> clientCache = new HashMap<>();
        Map<String, Instrument> instrumentCache = new HashMap<>();
        Map<String, Account> accountCache = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvInputStream))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // Skip blank lines
                if (line.isBlank())
                    continue;
                
                LineValues values = parseLine(line);
                try {
                

                    // Step 1: Create or retrieve User (generates user_id)
                    User user = userCache.get(values.client_name);
                    if (user == null) {
                        user = new User();
                        String[] nameParts = values.client_name.split(" ", 2);
                        user.setFirstName(nameParts[0]);
                        user.setLastName(nameParts.length > 1 ? nameParts[1] : nameParts[0]);
                        user.setUsername(values.client_id);
                        user.setEmail(values.client_id + "@trading.demo");
                        user.setPasswordHash("$2a$10$QEG13Tgr8.fgIP4n1.CxVeEoUhKYjWF7S2HK4mwhWh9V0zz80GUzi");
                        user.setCreatedAt(LocalDateTime.now());
                        user.setUpdatedAt(LocalDateTime.now());
                        em.persist(user);
                        em.flush(); // Flush to generate user_id
                        userCache.put(values.client_name, user);
                    }

                    // Step 2: Create or retrieve Client (depends on user_id)
                    String clientKey = values.client_id + "_" + user.getId();
                    Client client = clientCache.get(clientKey);
                    if (client == null) {
                        client = new Client();
                        client.setUser(user); // Set the User relationship
                        em.persist(client);
                        em.flush(); // Flush to generate client_id
                        clientCache.put(clientKey, client);
                    }

                    // Step 3: Create Instrument (independent)
                    Instrument instrument = instrumentCache.get(values.instrument);
                    if (instrument == null) {
                        // Check database for existing instrument
                        instrument = instrumentRepository.findByInstrumentSymbol(values.instrument).orElse(null);
                        
                        // Only create if doesn't exist in database
                        if (instrument == null) {
                            instrument = new Instrument();
                            instrument.setInstrumentName(values.instrument);
                            instrument.setAssetType(values.asset_class);
                            instrument.setCurrencyCode(values.currency);
                            instrument.setInstrumentSymbol(values.instrument);
                            em.persist(instrument);
                            em.flush();
                        }
                        instrumentCache.put(values.instrument, instrument);
                    }

                    // Step 4: Create Account (depends on client_id)
                    String accountKey = client.getId() + "_Account";
                    Account account = accountCache.get(accountKey);
                    if (account == null){
                        account = new Account();
                        account.setClient(client); // Set the Client relationship
                        account.setAccountName(values.client_id + "_Account");
                        em.persist(account);
                        em.flush();
                        accountCache.put(accountKey, account);
                    }

                    // Step 5: Create TradeOrder (depends on instrument_id and account_id)
                    TradeOrder tradeOrder = new TradeOrder();
                    tradeOrder.setAccount(account); // Set Account relationship
                    tradeOrder.setInstrument(instrument); // Set Instrument relationship
                    tradeOrder.setOrderDate(values.trade_date);
                    tradeOrder.setQuantity(BigDecimal.valueOf(values.quantity));
                    tradeOrder.setPrice(BigDecimal.valueOf(values.price));
                    tradeOrder.setValue(BigDecimal.valueOf(values.value));
                    tradeOrder.setSide(values.side);
                    em.persist(tradeOrder);
                    em.flush();

                    // Step 6: Create TradeOrderStatus (depends on tradeOrder)
                    TradeOrderStatus status = new TradeOrderStatus();
                    status.setTradeOrder(tradeOrder); // Set TradeOrder relationship
                    status.setTimeUpdated(values.trade_date);
                    status.setStatus(OrderStatus.FILLED);
                    em.persist(status);
                    em.flush();
                    // Step 6: update Holdings
                    Holding holding = holdingRepository.findByAccountIdAndInstrumentId(account.getId(), instrument.getId()).orElse(null);
                    if (holding == null){
                        holding = new Holding();
                        holding.setAccount(account);
                        holding.setInstrument(instrument);
                        holding.setQuantity(BigDecimal.valueOf(values.side == OrderSide.SELL ? values.quantity + 1.0 : 0.0 ));
                        holding.setTotalCost(BigDecimal.valueOf(values.value));
                        em.persist(holding);
                        em.flush();
                    }
                    if (values.side == OrderSide.BUY){
                        holding.setQuantity(holding.getQuantity().add(BigDecimal.valueOf(values.quantity)));
                    } 
                    // SELL
                    else {
                        holding.setQuantity(holding.getQuantity().subtract(BigDecimal.valueOf(values.quantity)));
                        if (holding.getQuantity().compareTo(BigDecimal.valueOf(0.0)) <= 0) {
                            account.getHoldings().remove(holding);
                            em.remove(holding);
                        }
                    }
                    em.persist(holding);
                    
                } catch (Exception e) {
                    System.err.println("Error persisting row: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void importFromInstrumentsCSV(InputStream csvInputStream) throws IOException {
        // Cache to track already-created instruments to avoid duplicates
        Map<String, Instrument> instrumentCache = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvInputStream))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // Skip blank lines
                if (line.isBlank())
                    continue;
                
                try {
                    InstrumentLineValues values = parseInstrumentLine(line);
                    
                    // Create or retrieve Instrument by symbol
                    Instrument instrument = instrumentCache.get(values.instrument_symbol);
                    if (instrument == null) {
                        // Check database for existing instrument
                        instrument = instrumentRepository.findByInstrumentSymbol(values.instrument_symbol)
                            .orElse(null);
                        
                        if (instrument == null) {
                            // Only create if doesn't exist in database
                            instrument = new Instrument();
                            instrument.setInstrumentName(values.instrument_name);
                            instrument.setInstrumentSymbol(values.instrument_symbol);
                            instrument.setAssetType(values.asset_type);
                            instrument.setCurrencyCode(values.currency_code);
                            em.persist(instrument);
                            em.flush(); // Flush to generate instrument_id
                        }
                        instrumentCache.put(values.instrument_symbol, instrument);
                    }
                } catch (Exception e) {
                    System.err.println("Error persisting instrument row: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading instruments CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void importFromQuotesCSV(InputStream csvInputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvInputStream))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // Skip blank lines
                if (line.isBlank())
                    continue;
                
                try {
                    QuoteLineValues values = parseQuoteLine(line);
                    
                    // Fetch the Instrument by ID
                    Instrument instrument = em.find(Instrument.class, values.instrument_id);
                    if (instrument == null) {
                        System.err.println("Instrument with ID " + values.instrument_id + " not found. Skipping quote.");
                        continue;
                    }
                    
                    // Create Quote
                    Quote quote = new Quote();
                    quote.setInstrument(instrument); // Set Instrument relationship
                    quote.setPrice(values.price);
                    quote.setHighPriceOfDay(values.high_price_of_day);
                    quote.setLowPriceOfDay(values.low_price_of_day);
                    quote.setOpenPriceOfDay(values.open_price_of_day);
                    quote.setPreviousClosePriceOfDay(values.previous_close_price_of_day);
                    quote.setChange(values.change_amount);
                    quote.setChangePercent(values.change_percent);
                    quote.setVolume(values.volume);
                    quote.setMarketCap(values.market_cap);
                    quote.setTimestamp(values.timestamp);
                    em.persist(quote);
                    em.flush();
                    
                } catch (Exception e) {
                    System.err.println("Error persisting quote row: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading quotes CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class LineValues {
        public String trade_id;
        public LocalDateTime trade_date;
        public String client_id;
        public String client_name;
        public String advisor;
        public String instrument;
        public AssetType asset_class;
        public TradeOrder.OrderSide side;
        public double quantity;
        public double price;
        public String currency;
        public double value;

        public LineValues(String trade_id, LocalDateTime trade_date, String client_id, String client_name, 
            String advisor, String instrument, AssetType asset_class, String side, double quantity,
             double price, String currency, double value) {

            this.trade_id = trade_id;
            this.trade_date = trade_date;
            this.client_id = client_id;
            this.client_name = client_name;
            this.advisor = advisor;
            this.instrument = instrument;
            this.asset_class = asset_class;
            this.side = side.equals("BUY") ? TradeOrder.OrderSide.BUY : TradeOrder.OrderSide.SELL;
            this.quantity = quantity;
            this.price = price;
            this.currency = currency;
            this.value = value;
        }
    }

    // trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value
    protected LineValues parseLine(String csvLine) throws IllegalArgumentException {
        try {
            String[] values = csvLine.split(",");

            String trade_id = values[0].trim();
            LocalDate trade_date = LocalDate.parse(values[1].trim());
            LocalDateTime trade_date_time = LocalDateTime.of(trade_date, LocalTime.MIN);
            String client_id = values[2].trim();
            String client_name = values[3].trim();
            String advisor = values[4].trim();
            String instrument = values[5].trim();
            String asset_class_str = values[6].trim();
            String side = values[7].trim().toUpperCase();
            Double quantity = Double.parseDouble(values[8].trim());
            double price = Double.parseDouble(values[9].trim());
            String currency = values[10].trim();
            double value = Double.parseDouble(values[11].trim());

            // Convert asset_class string to AssetType enum
            AssetType asset_class;
            try {
                asset_class = AssetType.valueOf(asset_class_str.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid asset_class: " + asset_class_str + ". Must be one of: " + Arrays.toString(AssetType.values()), e);
            }
            final String[] VALID_SIDES = {"BUY", "SELL"};

            if (!Arrays.asList(VALID_SIDES).contains(side.toUpperCase()))
                throw new IllegalArgumentException("Side must be one of: " + Arrays.toString(VALID_SIDES));
            if (quantity <= 0)
                throw new IllegalArgumentException("quantity must be positive");
            if (price <= 0)
                throw new IllegalArgumentException("price must be positive");
            if (value <=0)
                throw new IllegalArgumentException("value must be positive");

            return new LineValues(trade_id, trade_date_time, client_id, client_name, advisor, instrument, asset_class, side, quantity, price, currency, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV line: \'" + csvLine + "\' " + e.getMessage(), e);
        }
    }

    // instrument_name,instrument_symbol,asset_type,currency_code
    protected InstrumentLineValues parseInstrumentLine(String csvLine) throws IllegalArgumentException {
        try {
            String[] values = csvLine.split(",");
            
            if (values.length < 4) {
                throw new IllegalArgumentException("Instrument CSV line must have at least 4 columns");
            }

            String instrument_name = values[0].trim();
            String instrument_symbol = values[1].trim();
            String asset_type_str = values[2].trim().toUpperCase();
            String currency_code = values[3].trim();

            // Validate asset_type
            AssetType asset_type;
            try {
                asset_type = AssetType.valueOf(asset_type_str);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid asset_type: " + asset_type_str + ". Must be one of: " + Arrays.toString(AssetType.values()), e);
            }

            if (instrument_name.isEmpty())
                throw new IllegalArgumentException("instrument_name cannot be empty");
            if (instrument_symbol.isEmpty())
                throw new IllegalArgumentException("instrument_symbol cannot be empty");
            if (currency_code.isEmpty() || currency_code.length() != 3)
                throw new IllegalArgumentException("currency_code must be a 3-letter code (e.g., USD, GBP)");

            return new InstrumentLineValues(instrument_name, instrument_symbol, asset_type, currency_code);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse instrument CSV line: \'" + csvLine + "\' " + e.getMessage(), e);
        }
    }

    // instrument_id,price,high_price_of_day,low_price_of_day,open_price_of_day,previous_close_price_of_day,change_amount,change_percent,volume,market_cap,timestamp
    protected QuoteLineValues parseQuoteLine(String csvLine) throws IllegalArgumentException {
        try {
            String[] values = csvLine.split(",");

            if (values.length < 11) {
                throw new IllegalArgumentException("Quote CSV line must have at least 11 columns");
            }

            Long instrument_id = Long.parseLong(values[0].trim());
            BigDecimal price = new BigDecimal(values[1].trim());
            BigDecimal high_price_of_day = new BigDecimal(values[2].trim());
            BigDecimal low_price_of_day = new BigDecimal(values[3].trim());
            BigDecimal open_price_of_day = new BigDecimal(values[4].trim());
            BigDecimal previous_close_price_of_day = new BigDecimal(values[5].trim());
            BigDecimal change_amount = new BigDecimal(values[6].trim());
            BigDecimal change_percent = new BigDecimal(values[7].trim());
            long volume = Long.parseLong(values[8].trim());
            long market_cap = Long.parseLong(values[9].trim());
            LocalDateTime timestamp = LocalDateTime.parse(values[10].trim());

            // Validate prices
            if (price.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("price must be positive");
            if (high_price_of_day.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("high_price_of_day must be positive");
            if (low_price_of_day.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("low_price_of_day must be positive");
            if (volume < 0)
                throw new IllegalArgumentException("volume cannot be negative");

            return new QuoteLineValues(instrument_id, price, high_price_of_day, low_price_of_day, 
                open_price_of_day, previous_close_price_of_day, change_amount, change_percent, 
                volume, market_cap, timestamp);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse quote CSV line: \'" + csvLine + "\' " + e.getMessage(), e);
        }
    }

    public static class InstrumentLineValues {
        public String instrument_name;
        public String instrument_symbol;
        public AssetType asset_type;
        public String currency_code;

        public InstrumentLineValues(String instrument_name, String instrument_symbol, 
            AssetType asset_type, String currency_code) {
            this.instrument_name = instrument_name;
            this.instrument_symbol = instrument_symbol;
            this.asset_type = asset_type;
            this.currency_code = currency_code;
        }
    }

    public static class QuoteLineValues {
        public Long instrument_id;
        public BigDecimal price;
        public BigDecimal high_price_of_day;
        public BigDecimal low_price_of_day;
        public BigDecimal open_price_of_day;
        public BigDecimal previous_close_price_of_day;
        public BigDecimal change_amount;
        public BigDecimal change_percent;
        public long volume;
        public long market_cap;
        public LocalDateTime timestamp;

        public QuoteLineValues(Long instrument_id, BigDecimal price, BigDecimal high_price_of_day, 
            BigDecimal low_price_of_day, BigDecimal open_price_of_day, BigDecimal previous_close_price_of_day,
            BigDecimal change_amount, BigDecimal change_percent, long volume, long market_cap, 
            LocalDateTime timestamp) {
            this.instrument_id = instrument_id;
            this.price = price;
            this.high_price_of_day = high_price_of_day;
            this.low_price_of_day = low_price_of_day;
            this.open_price_of_day = open_price_of_day;
            this.previous_close_price_of_day = previous_close_price_of_day;
            this.change_amount = change_amount;
            this.change_percent = change_percent;
            this.volume = volume;
            this.market_cap = market_cap;
            this.timestamp = timestamp;
        }
    }
}
