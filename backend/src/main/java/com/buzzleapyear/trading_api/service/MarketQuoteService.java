package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.Pricing;
import com.buzzleapyear.trading_api.repository.PricingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * MarketQuoteService provides access to the latest market prices for instruments
 * 
 * @author Ari Lacanienta
 */
@Service
public class MarketQuoteService {
    
    private final PricingRepository pricingRepository;

    public MarketQuoteService(PricingRepository pricingRepository) {
        this.pricingRepository = pricingRepository;
    }

    /**
     * Get the latest market price for an instrument
     * @param instrument the instrument to get price for
     * @return the latest price, or null if no pricing data is available
     */
    @SuppressWarnings("null")
    public BigDecimal getLatestPrice(Instrument instrument) {
        if (instrument == null || instrument.getId() == null) {
            return null;
        }
        
        Optional<Pricing> pricing = pricingRepository.findLatestByInstrument(instrument);
        return pricing.map(Pricing::getPrice).orElse(null);
    }

    /**
     * Get the latest market price for an instrument by ID
     * @param instrumentId the instrument ID
     * @return the latest price, or null if no pricing data is available
     */
    @SuppressWarnings("null")
    public BigDecimal getLatestPriceById(Long instrumentId) {
        if (instrumentId == null || instrumentId <= 0) {
            return null;
        }
        
        Optional<Pricing> pricing = pricingRepository.findLatestByInstrumentId(instrumentId);
        return pricing.map(Pricing::getPrice).orElse(null);
    }

    /**
     * Check if pricing data exists for an instrument
     * @param instrument the instrument to check
     * @return true if pricing data exists, false otherwise
     */
    public boolean hasPricingData(Instrument instrument) {
        return getLatestPrice(instrument) != null;
    }
}
