package com.wealthcdio.banking.service;

import com.wealthcdio.banking.domain.Account;
import com.wealthcdio.banking.domain.Transaction;
import com.wealthcdio.banking.domain.TransactionType;
import com.wealthcdio.banking.dto.CreateAccountRequest;
import com.wealthcdio.banking.dto.TransactionResponse;
import com.wealthcdio.banking.exception.AccountNotFoundException;
import com.wealthcdio.banking.repository.AccountRepository;
import com.wealthcdio.banking.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankingService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository){

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;

    }

    @Transactional
    public void deposit(Long accountId, BigDecimal amount)
    {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        account.deposit(amount);

        accountRepository.save(account);

        Transaction transaction = new Transaction(

                account,
                TransactionType.DEPOSIT,
                amount,
                Instant.now());

        transactionRepository.save(transaction);

    }

    @Transactional
    public void withdraw(Long accountId, BigDecimal amount){

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        account.withdraw(amount);

        accountRepository.save(account);

        Transaction transaction = new Transaction(

                account,
                TransactionType.WITHDRAWAL,
                amount,
                Instant.now());

        transactionRepository.save(transaction);

    }

    @Transactional
    public void transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount){


            if (sourceAccountId.equals(destinationAccountId)){
                throw new IllegalArgumentException(
                        "Source and destination accounts must be different"
                );
            }

            Account soruceAccount = accountRepository.findById(sourceAccountId)
                    .orElseThrow(() ->
                            new AccountNotFoundException(
                                    "Account not found with id: " + sourceAccountId
                            ));

            Account destionationAccount = accountRepository.findById(destinationAccountId)
                    .orElseThrow(() ->
                            new AccountNotFoundException(
                                    "Account not found with id: " + destinationAccountId
                            ));

            soruceAccount.withdraw(amount);
            destionationAccount.deposit(amount);

            accountRepository.save(soruceAccount);
            accountRepository.save(destionationAccount);

            Transaction withdrwalTransaction = new Transaction(
                    soruceAccount,
                    TransactionType.TRANSFER_OUT,
                    amount,
                    Instant.now());

            Transaction depositTransaction = new Transaction(
                    destionationAccount,
                    TransactionType.TRANSFER_IN,
                    amount,
                    Instant.now());

            transactionRepository.save(withdrwalTransaction);
            transactionRepository.save(depositTransaction);

    }

    public BigDecimal getBalance(Long accountId){

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        return account.getBalance();
    }

    public List<TransactionResponse> getTransactionHistory(Long accountId){

        accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found with id: " + accountId
                        ));

        return transactionRepository.findByAccountId(accountId)
                .stream()
                .map(transaction ->
                        new TransactionResponse(
                                transaction.getId(),
                                transaction.getAccount().getId(),
                                transaction.getType(),
                                transaction.getAmount(),
                                transaction.getTimestamp()
                        )).toList();
    }

    @Transactional
    public Account createAccount(BigDecimal initialBalance){

        Account account = new Account(initialBalance);

        return accountRepository.save(account);
    }
}
