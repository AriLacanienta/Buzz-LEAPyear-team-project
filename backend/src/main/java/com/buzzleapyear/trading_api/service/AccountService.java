package com.buzzleapyear.trading_api.service;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.repository.TradeOrderRepository;
import com.buzzleapyear.trading_api.entity.TradeOrderStatus;
import com.buzzleapyear.trading_api.repository.TradeOrderStatusRepository;
import com.buzzleapyear.trading_api.dto.CurrentAccountResponseDto;
import com.buzzleapyear.trading_api.dto.RecentTradeOrderResponseDto;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.buzzleapyear.trading_api.dto.AccountValueResponseDto;
import com.buzzleapyear.trading_api.dto.HoldingResponseDto;
import com.buzzleapyear.trading_api.repository.AccountRepository;
import java.util.Optional;
import com.buzzleapyear.trading_api.entity.Account;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final HoldingService holdingService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private TradeOrderRepository tradeOrderRepository;
    private TradeOrderStatusRepository tradeOrderStatusRepository;

    public AccountService(
        AccountRepository accountRepository, 
        HoldingService holdingService, 
        UserRepository userRepository, 
        ClientRepository clientRepository,
        TradeOrderRepository tradeOrderRepository, 
        TradeOrderStatusRepository tradeOrderStatusRepository) {
        this.accountRepository = accountRepository;
        this.holdingService = holdingService;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.tradeOrderStatusRepository = tradeOrderStatusRepository;
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
    public Optional<Account> findAccountByIdForUser(Long accountId, String username) {
        return accountRepository.findById(accountId)
            .filter(acc -> acc.getClient().getUser().getUsername().equals(username));
    }

    @Transactional(readOnly = true)
    public Optional<List<RecentTradeOrderResponseDto>> getRecentTradeOrders(Long accountId, String username, int limit) {
        if (findAccountByIdForUser(accountId, username).isEmpty()) {
            return Optional.empty();
        }

        List<RecentTradeOrderResponseDto> recentTradeOrders = tradeOrderRepository
            .findByAccountIdOrderByOrderDateDesc(accountId, PageRequest.of(0, limit))
            .stream()
            .map(order -> {
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
                    status == null ? null : status.getReasonText()
                );
            })
            .toList();

        return Optional.of(recentTradeOrders);
    }
}