package com.buzzleapyear.trading_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveDisputeRequest(
    @NotBlank @Size(max = 2000) String resolutionMessage
) {}
