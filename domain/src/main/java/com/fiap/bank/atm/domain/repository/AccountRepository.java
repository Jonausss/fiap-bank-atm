package com.fiap.bank.atm.domain.repository;

import com.fiap.bank.atm.domain.model.Account;

public interface AccountRepository extends ATMRepository<Account> {
    Optional<Account> findByAccountNumber(String accountNumber);

    void save(Account account);
}