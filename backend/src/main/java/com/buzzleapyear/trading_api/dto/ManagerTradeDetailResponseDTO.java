package com.buzzleapyear.trading_api.dto;

import java.util.List;

public record ManagerTradeDetailResponseDTO(
    ManagerTradeLogResponseDTO trade,
    List<ManagerTradeStatusResponseDTO> statusHistory
) {}