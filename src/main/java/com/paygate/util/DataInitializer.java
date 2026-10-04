package com.paygate.util;

import com.paygate.model.entity.WalletAccount;
import com.paygate.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Data seeder that populates initial wallet accounts when the app starts up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final WalletAccountRepository walletAccountRepository;

    @Override
    public void run(String... args) {
        if (walletAccountRepository.count() == 0) {
            log.info("Seeding initial wallet accounts into H2 database...");

            WalletAccount user1 = WalletAccount.builder()
                    .userId("user_101")
                    .balance(new BigDecimal("1000.0000"))
                    .currency("INR")
                    .build();

            WalletAccount user2 = WalletAccount.builder()
                    .userId("user_102")
                    .balance(new BigDecimal("2500.5000"))
                    .currency("INR")
                    .build();

            walletAccountRepository.save(user1);
            walletAccountRepository.save(user2);

            log.info("Successfully seeded 2 test wallet accounts: user_101 (₹1000.00) & user_102 (₹2500.50)");
        }
    }
}
