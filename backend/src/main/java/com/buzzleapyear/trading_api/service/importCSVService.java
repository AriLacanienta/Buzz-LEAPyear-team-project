import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.IOException;
import org.springframework.stereotype.Service;

import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.TradeOrder;

@Service
public class importCSVService {
    public void importFromTradesCSV(Path csvFilepath){

        // open file for reading
        try (BufferedReader br = Files.newBufferedReader(csvFilepath)) {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory();
            
            String line;
            while ((line = br.readLine()) != null) {
                LineValues values = parseLine(line);

                /*
                    Insert Client (user_id, name, client_id)
                    Insert instrument (instrument, asset_class, currency) -> instrument_id
                    Insert account (client_id, advisor) -> account_id
                    Insert trade orders (trade_id, instrument_id, account_id, trade_date, quantity, price, value)
                */
               User user = new User();
               Instrument instrument = new Instrument(values.instrument, values.asset_class, values.currency);
               Account account = new Account(values.client_id, values.advisor);
               TradeOrder tradeOrder = new TradeOrder(values.trade_id, instrument.getInstrumentId(), account.getAccountId(), values.trade_date, values.quantity, values.price, values.value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public static class lineValues {
        public String trade_id;
        public Date trade_date;
        public String client_id;
        public String client_name;
        public String advisor;
        public String instrument;
        public String asset_class;
        public String side;
        public double quantity;
        public double price;
        public String currency;
        public double value;

        public lineValues(String trade_id, Date trade_date, String client_id, String client_name, 
            String advisor, String instrument, String asset_class, String side, double quantity,
             double price, String currency, double value) {

            final String[] VALID_ASSET_CLASSES = {"Equity", "Bond", "Fund", "Cash", "Crypto", "ETF"}; 
            final String[] VALID_SIDES = {"BUY", "SELL"};

            if (!Arrays.asList(VALID_ASSET_CLASSES).contains(asset_class))
                throw new IllegalArgumentException("asset_class must be one of: " + Arrays.toString(VALID_ASSET_CLASSES)); 
            if (!Arrays.asList(VALID_SIDES).contains(side))
                throw new IllegalArgumentException("Side must be one of: " + Arrays.toString(VALID_SIDES);
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
            this.side = side;
            this.quantity = quantity;
            this.price = price;
            this.currency = currency;
            this.value = value;
        }
    }

    // trade_id,trade_date,client_id,client_name,advisor,instrument,asset_class,side,quantity,price,currency,value
    protected LineValues parseLine(String csvLine) {
        String[] values = csvLine.split(",");
        String trade_id = values[0];
        Date trade_date = new DateFormat.parse(values[1]);
        String client_id = values[2];
        String client_name = values[3];
        String advisor = values[4];
        String instrument = values[5];
        String asset_class = values[6];
        String side = values[7].toUpperCase();
        double quantity = Double.parseDouble(values[8]);
        double price = Double.parseDouble(values[9]);
        String currency = values[10];
        double value = Double.parseDouble(values[11]);

        return new LineValues(trade_id, trade_date, client_id, client_name, advisor, instrument, asset_class, side, quantity, price, currency, value);
    }
}
