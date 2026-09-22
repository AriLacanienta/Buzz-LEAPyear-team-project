package com.buzzleapyear.trading_api.util;

import com.buzzleapyear.trading_api.dto.ListInstrumentsResponseDto;
import java.util.List;
import java.util.Arrays;
import java.math.BigDecimal;

public class InstrumentUtil {

    public static List<ListInstrumentsResponseDto> getMockInstruments() {
        return Arrays.asList(
            new ListInstrumentsResponseDto(
                "Apple Inc.",
                "AAPL",
                new BigDecimal("150.75"),
                new BigDecimal("1.25"),
                new BigDecimal("0.84"),
                new BigDecimal("1000000"),
                new BigDecimal("250000000")
            ),
            new ListInstrumentsResponseDto(
                "Microsoft Corporation",
                "MSFT",
                new BigDecimal("300.50"),
                new BigDecimal("2.10"),
                new BigDecimal("0.70"),
                new BigDecimal("2000000"),
                new BigDecimal("220000000")
            ),
            new ListInstrumentsResponseDto(
                "Alphabet Inc.",
                "GOOGL",
                new BigDecimal("2800.00"),
                new BigDecimal("15.00"),
                new BigDecimal("0.54"),
                new BigDecimal("1500000"),
                new BigDecimal("1800000000")
            ),
            new ListInstrumentsResponseDto(
                "Tesla, Inc.",
                "TSLA",
                new BigDecimal("700.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1.45"),
                new BigDecimal("3000000"),
                new BigDecimal("800000000")
            ),
            new ListInstrumentsResponseDto(
                "Amazon.com, Inc.",
                "AMZN",
                new BigDecimal("3500.00"),
                new BigDecimal("20.00"),
                new BigDecimal("0.57"),
                new BigDecimal("2500000"),
                new BigDecimal("1700000000")
            ),
            new ListInstrumentsResponseDto(
                "Meta Platforms, Inc.",
                "META",
                new BigDecimal("250.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.04"),
                new BigDecimal("1800000"),
                new BigDecimal("900000000")
            ),
            new ListInstrumentsResponseDto(
                "NVIDIA Corporation",
                "NVDA",
                new BigDecimal("220.00"),
                new BigDecimal("3.50"),
                new BigDecimal("1.62"),
                new BigDecimal("1200000"),
                new BigDecimal("550000000")
            ),
            new ListInstrumentsResponseDto(
                "Sony Group Corporation",
                "SONY",
                new BigDecimal("100.00"),
                new BigDecimal("2.00"),
                new BigDecimal("2.04"),
                new BigDecimal("1000000"),
                new BigDecimal("500000000")
            ),
            new ListInstrumentsResponseDto(
                "Intel Corporation",
                "INTC",
                new BigDecimal("55.00"),
                new BigDecimal("1.50"),
                new BigDecimal("2.73"),
                new BigDecimal("800000"),
                new BigDecimal("44000000")
            ),
            new ListInstrumentsResponseDto(
                "Advanced Micro Devices, Inc.",
                "AMD",
                new BigDecimal("100.00"),
                new BigDecimal("4.00"),
                new BigDecimal("1.25"),
                new BigDecimal("900000"),
                new BigDecimal("360000000")
            ),
            new ListInstrumentsResponseDto(
                "Qualcomm Incorporated",
                "QCOM",
                new BigDecimal("150.00"),
                new BigDecimal("3.00"),
                new BigDecimal("1.80"),
                new BigDecimal("700000"),
                new BigDecimal("300000000")
            ),
            new ListInstrumentsResponseDto(
                "Texas Instruments Incorporated",
                "TXN",
                new BigDecimal("180.00"),
                new BigDecimal("2.50"),
                new BigDecimal("1.39"),
                new BigDecimal("600000"),
                new BigDecimal("270000000")
            ),
            new ListInstrumentsResponseDto(
                "Broadcom Inc.",
                "AVGO",
                new BigDecimal("500.00"),
                new BigDecimal("3.50"),
                new BigDecimal("1.75"),
                new BigDecimal("500000"),
                new BigDecimal("250000000")
            ),
            new ListInstrumentsResponseDto(
                "Micron Technology, Inc.",
                "MU",
                new BigDecimal("70.00"),
                new BigDecimal("2.00"),
                new BigDecimal("1.50"),
                new BigDecimal("400000"),
                new BigDecimal("200000000")
            )
        );
    }
}
