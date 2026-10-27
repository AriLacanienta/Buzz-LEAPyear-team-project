package com.buzzleapyear.trading_api.service;

import com.buzzleapyear.trading_api.dto.ChatResponse;
import com.buzzleapyear.trading_api.dto.CurrentAccountResponseDto;
import com.buzzleapyear.trading_api.dto.PortfolioSummaryResponseDto;
import com.buzzleapyear.trading_api.dto.TradeOrderPreviewResponseDto;
import com.buzzleapyear.trading_api.dto.TopMoversResponseDto;
import com.buzzleapyear.trading_api.dto.StockComparisonResponseDto;
import com.buzzleapyear.trading_api.entity.Account;
import com.buzzleapyear.trading_api.entity.Instrument;
import com.buzzleapyear.trading_api.entity.TradeOrder.OrderSide;
import com.buzzleapyear.trading_api.entity.TradeOrder.QuantityMode;
import com.buzzleapyear.trading_api.repository.InstrumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private static final Pattern DISPUTE_PATTERN = Pattern.compile("\\bdisputes?\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern QUOTE_PATTERN = Pattern.compile(
        "quote\\s+([a-z0-9.-]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMPARE_PATTERN = Pattern.compile(
        "(?:compare\\s+)?([a-z][a-z0-9.-]*)\\s+(?:and|with|vs\\.?|versus)\\s+([a-z][a-z0-9.-]*)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TRADE_SIDE_PATTERN = Pattern.compile(
        "\\b(buy|sell)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
        "\\b(\\d+(?:\\.\\d+)?)\\b"
    );

    private static final Pattern SYMBOL_AFTER_OF_PATTERN = Pattern.compile(
        "\\b(?:of|symbol|ticker)\\s+([a-z][a-z0-9.-]*)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MARKET_MOVERS_PATTERN = Pattern.compile(
        "top\\s+(?:10\\s+)?(?:earners|gainers|losers)|"
            + "(?:top\\s+)?(?:earners\\s+and\\s+losers|gainers\\s+and\\s+losers)",
        Pattern.CASE_INSENSITIVE
    );

    private final AccountService accountService;
    private final QuoteService quoteService;
    private final InstrumentRepository instrumentRepository;
    private final OrderProcessor orderProcessor;

    public ChatbotService(
        AccountService accountService,
        QuoteService quoteService,
        InstrumentRepository instrumentRepository,
        OrderProcessor orderProcessor
    ) {
        this.accountService = accountService;
        this.quoteService = quoteService;
        this.instrumentRepository = instrumentRepository;
        this.orderProcessor = orderProcessor;
    }

    public ChatResponse respond(String rawMessage, String username) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return new ChatResponse(
                "UNKNOWN",
                "Please enter a question or command.",
                null,
                null
            );
        }

        String message = rawMessage.trim().toLowerCase(Locale.ROOT);

        if (DISPUTE_PATTERN.matcher(message).find()) {
            return new ChatResponse(
                "DISPUTE_FORM",
                "Select an executed order from your order history in the form below. Review the details before confirming. Only orders belonging to your signed-in account can be disputed.",
                null,
                "CREATE_DISPUTE"
            );
        }

        if (message.equals("help")) {
            return new ChatResponse(
                "HELP",
                    "You can ask about your portfolio, quotes, stock comparisons (e.g., compare AAPL and MSFT), top gainers/losers, or trade previews. "
                        + "Try: compare AAPL and MSFT, quote AAPL, buy 5 shares of AAPL, or raise a dispute.",
                null,
                null
            );
        }

        if (MARKET_MOVERS_PATTERN.matcher(message).matches()) {
                boolean wantsGainers = message.contains("earner") || message.contains("gainer");
            boolean wantsLosers = message.contains("loser");

            if (!wantsGainers && !wantsLosers) {
                wantsGainers = true;
                wantsLosers = true;
            }

            return new ChatResponse(
                "MARKET_MOVERS",
                wantsGainers && wantsLosers
                    ? "Here are today's top 10 gainers and losers."
                    : wantsGainers
                        ? "Here are today's top 10 gainers."
                        : "Here are today's top 10 losers.",
                new TopMoversResponseDto(
                    wantsGainers ? quoteService.getTopGainers(10) : java.util.List.of(),
                    wantsLosers ? quoteService.getTopLosers(10) : java.util.List.of()
                ),
                null
            );
        }

        Matcher comparisonMatcher = COMPARE_PATTERN.matcher(message);

        if (comparisonMatcher.matches()) {
            String firstSymbol = comparisonMatcher.group(1).toUpperCase(Locale.ROOT);
            String secondSymbol = comparisonMatcher.group(2).toUpperCase(Locale.ROOT);

            var firstQuote = quoteService.getLatestQuoteBySymbol(firstSymbol);
            var secondQuote = quoteService.getLatestQuoteBySymbol(secondSymbol);

            if (firstQuote.isEmpty() || secondQuote.isEmpty()) {
                return new ChatResponse(
                    "COMPARISON",
                    "I could not find a current quote for both stocks.",
                    null,
                    null
                );
            }

            return new ChatResponse(
                "COMPARISON",
                "Here is a comparison of " + firstSymbol + " and " + secondSymbol + ".",
                new StockComparisonResponseDto(firstQuote.get(), secondQuote.get()),
                null
            );
        }

        Matcher quoteMatcher = QUOTE_PATTERN.matcher(message);

        if (quoteMatcher.matches()) {
            String symbol = quoteMatcher.group(1).toUpperCase(Locale.ROOT);

            return quoteService.getLatestQuoteBySymbol(symbol)
                .map(quote -> new ChatResponse(
                    "QUOTE",
                    symbol + " is currently priced at " + quote.price(),
                    quote,
                    null
                ))
                .orElse(new ChatResponse(
                    "QUOTE",
                    "I could not find a quote for " + symbol + ".",
                    null,
                    null
                ));
        }

        TradeCommand tradeCommand = parseTradeCommand(message);

        if (tradeCommand != null) {
            return createTradePreview(tradeCommand, username);
        }

        if (message.contains("portfolio")) {
            CurrentAccountResponseDto account =
                accountService.getCurrentAccount(username);

            PortfolioSummaryResponseDto portfolio =
                accountService
                    .getPortfolioSummary(account.accountId(), username)
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Portfolio not found"
                    ));

            return new ChatResponse(
                "PORTFOLIO",
                "Here is your current portfolio summary.",
                portfolio,
                "VIEW_PORTFOLIO"
            );
        }

        return new ChatResponse(
            "UNKNOWN",
                "I can help with:\n- Portfolio summaries\n- Stock comparisons (e.g., AAPL vs MSFT)\n- Top gainers/losers\n- Quotes (e.g., quote AAPL)\n- Trade (e.g., buy 5 shares of AAPL)\n- Trade disputes (e.g., raise a dispute)",
            null,
            null
        );
    }

    private TradeCommand parseTradeCommand(String message) {
        Matcher sideMatcher = TRADE_SIDE_PATTERN.matcher(message);
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(message);

        if (!sideMatcher.find() || !quantityMatcher.find()) {
            return null;
        }

        OrderSide side = sideMatcher.group(1).equalsIgnoreCase("buy")
            ? OrderSide.BUY
            : OrderSide.SELL;
        BigDecimal quantity = new BigDecimal(quantityMatcher.group(1));

        Matcher symbolAfterOfMatcher = SYMBOL_AFTER_OF_PATTERN.matcher(message);
        String symbol = symbolAfterOfMatcher.find()
            ? symbolAfterOfMatcher.group(1)
            : findSymbolToken(message, sideMatcher.group(1), quantityMatcher.group(1));

        if (symbol == null) {
            return null;
        }

        return new TradeCommand(side, quantity, symbol.toUpperCase(Locale.ROOT));
    }

    private String findSymbolToken(String message, String side, String quantity) {
        String symbol = null;

        for (String token : message.split("\\s+")) {
            String candidate = token.replaceAll("[^a-z0-9.-]", "");

            if (candidate.isBlank()
                || candidate.equals(side)
                || candidate.equals(quantity)
                || isTradeFillerWord(candidate)
                || !candidate.matches("[a-z][a-z0-9.-]*")) {
                continue;
            }

            symbol = candidate;
        }

        return symbol;
    }

    private boolean isTradeFillerWord(String word) {
        return switch (word) {
            case "i", "want", "would", "like", "please", "to", "shares",
                "share", "of", "stock", "stocks", "order", "place", "for",
                "me", "an", "the" -> true;
            default -> false;
        };
    }

    private ChatResponse createTradePreview(
        TradeCommand tradeCommand,
        String username
    ) {
        OrderSide side = tradeCommand.side();
        BigDecimal quantity = tradeCommand.quantity();
        String symbol = tradeCommand.symbol();

        CurrentAccountResponseDto currentAccount =
            accountService.getCurrentAccount(username);

        Account account = accountService
            .findAccountByIdForUser(currentAccount.accountId(), username)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Account not found"
            ));

        Instrument instrument = instrumentRepository
            .findByInstrumentSymbol(symbol)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Instrument not found: " + symbol
            ));

        TradeOrderPreviewResponseDto preview =
            orderProcessor.previewOrder(
                account,
                instrument,
                side,
                QuantityMode.SHARES,
                quantity,
                null
            );

        String message = preview.valid()
            ? "This trade preview is valid. Review it before submitting."
            : "This trade cannot currently be completed: "
                + preview.reason();

        return new ChatResponse(
            "TRADE_PREVIEW",
            message,
            preview,
            "VIEW_TRADE"
        );
    }

    private record TradeCommand(OrderSide side, BigDecimal quantity, String symbol) {}
}
