package com.paygate.repository;

import com.paygate.model.entity.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for WalletAccount entity operations.
 */
@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    /**
     * Look up wallet account by customer user ID.
     */
    Optional<WalletAccount> findByUserId(String userId);

    /**
     * Check if a wallet exists for a specific user ID.
     */
    boolean existsByUserId(String userId);
}
