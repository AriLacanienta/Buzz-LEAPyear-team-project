package com.buzzleapyear.trading_api.dto;

import java.util.Map;

public record UserSettingsResponse(String phoneNumber, String country, Map<String, Object> preferences) {}
