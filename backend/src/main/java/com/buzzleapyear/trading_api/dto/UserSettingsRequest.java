package com.buzzleapyear.trading_api.dto;

import java.util.Map;

public record UserSettingsRequest(String phoneNumber, String country, Map<String, Object> preferences) {}
