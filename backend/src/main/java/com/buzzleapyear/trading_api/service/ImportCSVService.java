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
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Instrument.AssetType;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import com.buzzleapyear.trading_api.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class ImportCSVService {

    @PersistenceContext 
    private EntityManager em;

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
                        user.setUsername(values.client_id + "_" + System.currentTimeMillis());
                        user.setEmail(values.client_id + "@trading.local");
                        user.setPasswordHash("hashed_password");
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
                        instrument = new Instrument();
                        instrument.setInstrumentName(values.instrument);
                        instrument.setAssetType(values.asset_class);
                        instrument.setCurrencyCode(values.currency);
                        instrument.setInstrumentSymbol(values.instrument);
                        em.persist(instrument);
                        em.flush();
                        instrumentCache.put(values.instrument, instrument);
                    }

                    // Step 4: Create Account (depends on client_id)
                    String accountKey = client.getId() + "_Account";
                    Account account = accountCache.get(accountKey);
                    if (account == null){
                        account = new Account();
                        account.setAccountName(values.client_id + "_Account");
                        account.setClient(client); // Set the Client relationship
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
}
