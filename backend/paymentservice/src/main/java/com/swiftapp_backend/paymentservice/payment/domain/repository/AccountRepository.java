package com.swiftapp_backend.paymentservice.payment.domain.repository;

import com.swiftapp_backend.paymentservice.payment.domain.model.AccountDetails;

public interface AccountRepository {
    AccountDetails save(AccountDetails account);
    AccountDetails getAccountDetails(Long userId);
    AccountDetails deleteAccountDetails(Long userId);
    AccountDetails updateAccountDetails(AccountDetails account);
}
