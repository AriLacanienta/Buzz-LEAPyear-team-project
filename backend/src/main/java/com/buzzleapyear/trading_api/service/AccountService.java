package com.buzzleapyear.trading_api.service;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import com.buzzleapyear.trading_api.repository.DisputeRepository;
import com.buzzleapyear.trading_api.entity.DisputeStatus;
import com.buzzleapyear.trading_api.dto.CurrentAccountResponseDto;
import com.buzzleapyear.trading_api.dto.RecentTradeOrderResponseDto;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.buzzleapyear.trading_api.dto.AccountValueResponseDto;
import com.buzzleapyear.trading_api.dto.FundsRequestDto;
import com.buzzleapyear.trading_api.dto.HoldingResponseDto;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import java.util.Optional;
import com.buzzleapyear.trading_api.entity.Account;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.buzzleapyear.trading_api.entity.TradeOrder;
import java.math.RoundingMode;
import com.buzzleapyear.trading_api.dto.PortfolioSummaryResponseDto;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

@Service
public class AccountService {
    private static final BigDecimal MAX_FUNDS_AMOUNT = new BigDecimal("1000000.00");
    private final AccountRepository accountRepository;
    private final HoldingService holdingService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private TradeOrderRepository tradeOrderRepository;
    private TradeOrderStatusRepository tradeOrderStatusRepository;
    private final DisputeRepository disputeRepository;

    public AccountService(
        AccountRepository accountRepository, 
        HoldingService holdingService, 
        UserRepository userRepository, 
        ClientRepository clientRepository,
        TradeOrderRepository tradeOrderRepository, 
        TradeOrderStatusRepository tradeOrderStatusRepository,
        DisputeRepository disputeRepository) {
        this.accountRepository = accountRepository;
        this.holdingService = holdingService;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.tradeOrderStatusRepository = tradeOrderStatusRepository;
        this.disputeRepository = disputeRepository;
    }

    // Configured to only find a single user account at the moment
    @Transactional(readOnly = true)
    public CurrentAccountResponseDto getCurrentAccount(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Account> accounts = clientRepository.findByUserId(user.getId()).stream()
        .flatMap(client -> accountRepository.findByClientId(client.getId()).stream())
        .toList();

        if (accounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No accounts found for user");
        }

        if (accounts.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Multiple accounts found for user");
        }

        Account currentAccount = accounts.get(0);
        return new CurrentAccountResponseDto(
            currentAccount.getId(),
            currentAccount.getAccountName()
        );
    }

    @Transactional(readOnly = true)
    public Optional<AccountValueResponseDto> getAccountValue(String id, String username) {
        Account account = findAccountByIdForUser(Long.parseLong(id), username).orElse(null);

        if (account == null) {
            return Optional.empty();
        }

        List<HoldingResponseDto> holdings = holdingService.getHoldingsByAccountId(Long.parseLong(id));

        if (holdings == null) {
            return Optional.empty();
        }

        BigDecimal totalPortfolioValue = BigDecimal.valueOf(0.0);

        for (HoldingResponseDto holding : holdings) {
            totalPortfolioValue = totalPortfolioValue.add(holding.totalValue());
        }

        return Optional.of(new AccountValueResponseDto(
            account.getCashAvailable(),
            account.getCashReserved(),
            totalPortfolioValue
        ));
    }

    @Transactional(readOnly = true)
    public Optional<PortfolioSummaryResponseDto> getPortfolioSummary(Long accountId, String username) {
        if(findAccountByIdForUser(accountId, username).isEmpty()) {
            return Optional.empty();
        }

        Map<String, BigDecimal> qtyBoughtToday = new HashMap<>();
        Map<String, BigDecimal> costBoughtToday = new HashMap<>();
        for (TradeOrder order : tradeOrderRepository.findFilledOrdersSince(
                accountId, TradeOrder.OrderSide.BUY, TradeOrderStatus.OrderStatus.FILLED, LocalDate.now().atStartOfDay())) {
            String symbol = order.getInstrument().getInstrumentSymbol();
            qtyBoughtToday.merge(symbol, order.getQuantity(), BigDecimal::add);
            costBoughtToday.merge(symbol, order.getQuantity().multiply(order.getPrice()), BigDecimal::add);
        }

        BigDecimal portfolioValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal todayStartValue = BigDecimal.ZERO;
        List<HoldingResponseDto> holdings = holdingService.getHoldingsByAccountId(accountId);

        for (HoldingResponseDto holding : holdings) {
            BigDecimal quantity = orZero(holding.quantity());
            BigDecimal price = orZero(holding.currentPrice());
            BigDecimal previousClose = price.subtract(orZero(holding.change()));

            BigDecimal boughtToday = qtyBoughtToday.getOrDefault(holding.instrumentSymbol(), BigDecimal.ZERO);
            BigDecimal todayQty = boughtToday.min(quantity);
            BigDecimal todayCost = todayQty.signum() == 0 ? BigDecimal.ZERO
                : costBoughtToday.get(holding.instrumentSymbol())
                    .multiply(todayQty)
                    .divide(boughtToday, 10, RoundingMode.HALF_UP);
            BigDecimal olderQty = quantity.subtract(todayQty);

            portfolioValue = portfolioValue.add(orZero(holding.totalValue()));
            totalCostBasis = totalCostBasis.add(orZero(holding.totalCost()));
            todayStartValue = todayStartValue.add(olderQty.multiply(previousClose)).add(todayCost);
        }

        BigDecimal todayChange = portfolioValue.subtract(todayStartValue);
        BigDecimal allTimeChange = portfolioValue.subtract(totalCostBasis);

        return Optional.of(new PortfolioSummaryResponseDto(
            holdings.size(),
            portfolioValue.setScale(2, RoundingMode.HALF_UP),
            totalCostBasis.setScale(2, RoundingMode.HALF_UP),
            todayChange.setScale(2, RoundingMode.HALF_UP),
            percentOf(todayChange, todayStartValue),
            allTimeChange.setScale(2, RoundingMode.HALF_UP),
            percentOf(allTimeChange, totalCostBasis)
        ));
    }

    @Transactional
    public void updateFunds(Long accountId, FundsRequestDto request, String username) {
        if (request == null || request.amount() == null || request.amount().signum() <= 0 || request.amount().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        if (request.amount().compareTo(MAX_FUNDS_AMOUNT) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount cannot exceed 1000000");
        }

        Account account = findAccountByIdForUser(accountId, username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        BigDecimal amount = request.amount();
        String action = request.action() == null ? "" : request.action().toUpperCase(Locale.ROOT);

        if ("ADD".equals(action)) {
            account.setCashAvailable(account.getCashAvailable().add(amount));
        } else if ("WITHDRAW".equals(action)) {
            if (account.getCashAvailable().compareTo(amount) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient available cash");
            }
            account.setCashAvailable(account.getCashAvailable().subtract(amount));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action must be ADD or WITHDRAW");
        }

        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findAccountByIdForUser(Long accountId, String username) {
        return accountRepository.findById(accountId)
            .filter(acc -> acc.getClient().getUser().getUsername().equals(username));
    }

    @Transactional(readOnly = true)
    public Optional<Page<RecentTradeOrderResponseDto>> getRecentTradeOrders(Long accountId, String username, int page, int size) {
        if (findAccountByIdForUser(accountId, username).isEmpty()) {
            return Optional.empty();
        }

        Page<TradeOrder> orders = tradeOrderRepository
            .findByAccountIdOrderByOrderDateDesc(accountId, PageRequest.of(page, size));

            return Optional.of(orders.map( order -> {
                TradeOrderStatus status = tradeOrderStatusRepository.findFirstByTradeOrderIdOrderByTimeUpdatedDesc(order.getId());
                return new RecentTradeOrderResponseDto(
                    order.getId(),
                    order.getInstrument().getInstrumentSymbol(),
                    order.getSide(),
                    order.getQuantity(),
                    order.getPrice(),
                    order.getOrderDate(),
                    status == null ? null : status.getStatus(),
                    status == null ? null : status.getTimeUpdated(),
                    status == null ? null : status.getReasonText(),
                    disputeRepository.existsByTradeOrderIdAndStatus(order.getId(), DisputeStatus.OPEN)
                );
            }));
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal base) {
        if (base.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
}