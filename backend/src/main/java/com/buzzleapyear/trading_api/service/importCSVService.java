import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.Client;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.TradeOrder;

@Service
public class importCSVService {
    public void importFromTradesCSV(Path csvFilepath) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("persistence-unit-name");
        EntityManager em = emf.createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        
        // Cache to track already-created users and clients to avoid duplicates
        Map<String, User> userCache = new HashMap<>();
        Map<String, Client> clientCache = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(csvFilepath)) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                try {
                    LineValues values = parseLine(line);
                    
                    if (!transaction.isActive()) {
                        transaction.begin();
                    }

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
                    Instrument instrument = new Instrument();
                    instrument.setInstrumentName(values.instrument);
                    instrument.setAssetType(values.asset_class);
                    instrument.setCurrencyCode(values.currency);
                    em.persist(instrument);
                    em.flush();

                    // Step 4: Create Account (depends on client_id)
                    Account account = new Account();
                    account.setAccountName(values.client_id + "_Account");
                    account.setClient(client); // Set the Client relationship
                    em.persist(account);
                    em.flush();

                    // Step 5: Create TradeOrder (depends on instrument_id and account_id)
                    TradeOrder tradeOrder = new TradeOrder();
                    tradeOrder.setTradeId(values.trade_id);
                    tradeOrder.setAccount(account); // Set Account relationship
                    tradeOrder.setInstrument(instrument); // Set Instrument relationship
                    tradeOrder.setOrderDate(values.trade_date);
                    tradeOrder.setQuantity(values.quantity);
                    tradeOrder.setPrice(values.price);
                    tradeOrder.setValue(values.value);
                    tradeOrder.setSide(values.side);
                    em.persist(tradeOrder);
                    
                    transaction.commit();
                } catch (Exception e) {
                    if (transaction.isActive()) {
                        transaction.rollback();
                    }
                    System.err.println("Error importing row: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
        }
    }

    public static class LineValues {
        public String trade_id;
        public Date trade_date;
        public String client_id;
        public String client_name;
        public String advisor;
        public String instrument;
        public String asset_class;
        public OrderSide side;
        public Integer quantity;
        public double price;
        public String currency;
        public double value;

        public LineValues(String trade_id, Date trade_date, String client_id, String client_name, 
            String advisor, String instrument, String asset_class, String side, double quantity,
             double price, String currency, double value) {

            final String[] VALID_ASSET_CLASSES = {"Equity", "Bond", "Fund", "Cash", "Crypto", "ETF"}; 
            final String[] VALID_SIDES = {"BUY", "SELL"};

            if (!Arrays.asList(VALID_ASSET_CLASSES).contains(asset_class))
                throw new IllegalArgumentException("asset_class must be one of: " + Arrays.toString(VALID_ASSET_CLASSES)); 
            if (!Arrays.asList(VALID_SIDES).contains(side.toUpperCase()))
                throw new IllegalArgumentException("Side must be one of: " + Arrays.toString(VALID_SIDES));
            if (quantity <= 0)
                throw new IllegalArgumentException("quantity must be positive");
            if (price <= 0)
                throw new IllegalArgumentException("price must be positive");
            if (value <=0)
                throw new IllegalArgumentException("value must be positive");

            this.trade_id = trade_id;
            this.trade_date = trade_date;
            this.client_id = client_id;
            this.client_name = client_name;
            this.advisor = advisor;
            this.instrument = instrument;
            this.asset_class = asset_class;
            this.side = side == "BUY" ? TradeOrder.OrderSide.BUY : TradeOrder.OrderSide.SELL;
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
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date trade_date = dateFormat.parse(values[1].trim());
            
            String client_id = values[2].trim();
            String client_name = values[3].trim();
            String advisor = values[4].trim();
            String instrument = values[5].trim();
            String asset_class = values[6].trim();
            String side = values[7].trim().toUpperCase();
            int quantity = Integer.parseInt(values[8].trim());
            double price = Double.parseDouble(values[9].trim());
            String currency = values[10].trim();
            double value = Double.parseDouble(values[11].trim());


            return new LineValues(trade_id, trade_date, client_id, client_name, advisor, instrument, asset_class, side, quantity, price, currency, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse CSV line: \'" + csvLine + "\'" + e.getMessage(), e);
        }
    }
}
