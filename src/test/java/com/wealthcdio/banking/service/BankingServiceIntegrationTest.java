package com.wealthcdio.banking.service;

import com.wealthcdio.banking.domain.Account;
import com.wealthcdio.banking.domain.Transaction;
import com.wealthcdio.banking.repository.AccountRepository;
import com.wealthcdio.banking.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class BankingServiceIntegrationTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase(){
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

    }

    @Test
    void shouldTransferMoneyAtomically(){

        Account source = accountRepository.save(new Account(new BigDecimal("1000.00")));
        Account destination = accountRepository.save(new Account(new BigDecimal("500.00")));

        bankingService.transfer(source.getId(), destination.getId(), new BigDecimal("300.00"));

        Account updateSource = accountRepository.findById(source.getId()).orElseThrow();
        Account updateDestination = accountRepository.findById(destination.getId()).orElseThrow();

        assertEquals(new BigDecimal("700.00"), updateSource.getBalance());
        assertEquals(new BigDecimal("800.00"), updateDestination.getBalance());

        assertEquals(2, transactionRepository.count());

    }

    @Test
    void shouldRollbackTransferWhenLedgerSaveFails(){

        Account source = accountRepository.save(new Account(new BigDecimal("1000.00")));
        Account destination = accountRepository.save(new Account(new BigDecimal("500.00")));

        doThrow(new RuntimeException("Ledger save failed"))
                .when(transactionRepository).save(any(Transaction.class));

        assertThrows(RuntimeException.class,
                ()-> bankingService.transfer(source.getId(),
                        destination.getId(), new BigDecimal("300.00")));

        Account sourceAfterFailure =
                accountRepository.findById(source.getId()).orElseThrow();

        Account destinationAfterFailure =
                accountRepository.findById(destination.getId()).orElseThrow();

        assertEquals(new BigDecimal("1000.00"), sourceAfterFailure.getBalance());
        assertEquals(new BigDecimal("500.00"), destinationAfterFailure.getBalance());

        assertEquals(0, transactionRepository.count());
    }
}
