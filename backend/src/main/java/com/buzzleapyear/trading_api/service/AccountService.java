package com.buzzleapyear.trading_api.service;
import com.buzzleapyear.trading_api.repository.UserRepository;
import com.buzzleapyear.trading_api.entity.User;
import com.buzzleapyear.trading_api.repository.ClientRepository;
import com.buzzleapyear.trading_api.dto.CurrentAccountResponseDto;
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

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final HoldingService holdingService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public AccountService(AccountRepository accountRepository, HoldingService holdingService, UserRepository userRepository, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.holdingService = holdingService;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
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
        Account account = accountRepository.findById(Long.parseLong(id))
            .filter(account -> account.getClient().getUser().getUsername().equals(username))
            .orElse(null);

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
}