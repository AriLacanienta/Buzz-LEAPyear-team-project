package com.buzzleapyear.trading_api.util;

import java.util.Set;

public final class SupportedCountries {
    private static final Set<String> COUNTRIES = Set.of(
            "Australia", "Canada", "France", "Germany", "India", "Ireland",
            "Japan", "Netherlands", "New Zealand", "Singapore", "United Kingdom", "United States"
    );

    private SupportedCountries() {}

    public static boolean contains(String country) {
        return COUNTRIES.contains(country);
    }
}
