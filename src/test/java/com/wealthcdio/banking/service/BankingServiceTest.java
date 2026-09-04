package com.wealthcdio.banking.service;


import com.wealthcdio.banking.domain.Account;
import com.wealthcdio.banking.domain.Transaction;
import com.wealthcdio.banking.domain.TransactionType;
import com.wealthcdio.banking.dto.TransactionResponse;
import com.wealthcdio.banking.exception.AccountNotFoundException;
import com.wealthcdio.banking.exception.InsufficientBalanceException;
import com.wealthcdio.banking.exception.InvalidAmountException;
import com.wealthcdio.banking.repository.AccountRepository;
import com.wealthcdio.banking.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BankingService bankingService;

    @Test
    void shouldDepositMoneyAndCreateLedgerTransaction(){

        Account account = new Account(new BigDecimal("1000.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        bankingService.deposit(1L, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("1500.00"), account.getBalance());

        verify(accountRepository).findById(1L);
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction transaction = transactionCaptor.getValue();

        assertEquals(account, transaction.getAccount());
        assertEquals(TransactionType.DEPOSIT, transaction.getType());
        assertEquals(new BigDecimal("500.00"), transaction.getAmount());
        assertNotNull(transaction.getTimestamp());

    }

    @Test
    void shouldWithdrawMoneyAndCreateLedgerTransaction(){

        Account account = new Account(new BigDecimal("1000.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        bankingService.withdraw(1L, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), account.getBalance());

        verify(accountRepository).findById(1L);
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction transaction = transactionCaptor.getValue();

        assertEquals(account, transaction.getAccount());
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        assertEquals(new BigDecimal("300.00"), transaction.getAmount());
        assertNotNull(transaction.getTimestamp());

    }

    @Test
    void shouldNotCreateTransactionWhenWithdrawalFails(){

        Account account = new Account(new BigDecimal("1000.00"));

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(account));

        assertThrows(InsufficientBalanceException.class,
                () -> bankingService.withdraw(1L,
                        new BigDecimal("1500.00")));

        assertEquals(new BigDecimal("1000.00"),
                account.getBalance());

        verify(accountRepository).findById(1L);

        verify(accountRepository, never())
                .save(any(Account.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void shouldTransferMoneyBetweenAccountsAndCreateLedgerTransactions(){

        Account sourceAccount = new Account(new BigDecimal("1000.00"));
        Account destinationAccount = new Account(new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinationAccount));

        bankingService.transfer(1L, 2L, new BigDecimal("300.00"));

        // Verify balances
        assertEquals(new BigDecimal("700.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("800.00"), destinationAccount.getBalance());

        // Verify accounts were retrieved
        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);

        // Verify accounts were saved
        verify(accountRepository).save(sourceAccount);
        verify(accountRepository).save(destinationAccount);

        // Capture both ledger transactions
        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> transactions = transactionCaptor.getAllValues();

        // Source ledger
        Transaction sourceTransaction = transactions.getFirst();
        assertEquals(sourceAccount, sourceTransaction.getAccount());
        assertEquals(TransactionType.TRANSFER_OUT, sourceTransaction.getType());
        assertEquals(new BigDecimal("300.00"), sourceTransaction.getAmount());
        assertNotNull(sourceTransaction.getTimestamp());

        // Destination ledger
        Transaction destinationTransaction =transactions.get(1);
        assertEquals(destinationAccount, destinationTransaction.getAccount());
        assertEquals(TransactionType.TRANSFER_IN, destinationTransaction.getType());
        assertEquals(new BigDecimal("300.00"), destinationTransaction.getAmount());
        assertNotNull(destinationTransaction.getTimestamp());


    }

    @Test
    void shouldRejectZeroTransfer(){

        Account sourceAccount = new Account(new BigDecimal("1000.00"));
        Account destinationAccount = new Account(new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinationAccount));

        assertThrows(InvalidAmountException.class,
                () -> bankingService.transfer(1L, 2L, new BigDecimal("0")));

    }

    @Test
    void shouldRejectNegativeTransfer(){

        Account sourceAccount = new Account(new BigDecimal("1000.00"));
        Account destinationAccount = new Account(new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinationAccount));

        assertThrows(InvalidAmountException.class,
                () -> bankingService.transfer(1L, 2L, new BigDecimal("-100.00")));
    }

    @Test
    void shouldRejectTransferWhenSourceBalanceIsInsufficient(){

        Account sourceAccount = new Account(new BigDecimal("1000.00"));
        Account destinationAccount = new Account(new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(destinationAccount));

        assertThrows(InsufficientBalanceException.class,
                () -> bankingService.transfer(1L, 2L, new BigDecimal("1500.00")));

        // balances must remain unchanged
        assertEquals(new BigDecimal("1000.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), destinationAccount.getBalance());

        // No account or ledger should be saved
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));

    }

    @Test
    void shouldRejectTransferWhenSourceAccountDoesNotExist(){

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> bankingService.transfer(1L,2L, new BigDecimal("300.00")));

        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).findById(2L);
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void shouldRejectTransferWhenDestinationAccountDoesNotExist(){

        Account sourceAccount = new Account(new BigDecimal("1000.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                ()-> bankingService.transfer(1L, 2L, new BigDecimal("300.00")));

        assertEquals(new BigDecimal("1000.00"), sourceAccount.getBalance());

        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));

    }

    @Test
    void shouldRejectTransferToSameAccount(){

        assertThrows(IllegalArgumentException.class,
                ()-> bankingService.transfer(1L,1L, new BigDecimal("300.00")));

        verify(accountRepository, never()).findById(anyLong());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));

    }

    @Test
    void shouldReturnAccountBalance(){

        Account account = new Account(new BigDecimal("1250.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        BigDecimal balance = bankingService.getBalance(1L);

        assertEquals(new BigDecimal("1250.00"), balance);
        verify(accountRepository).findById(1L);

    }
    @Test
    void shouldReturnTransactionHistoryForAccount() {

        Account account = new Account(new BigDecimal("1000.00"));

        Transaction deposit = new Transaction(
                account,
                TransactionType.DEPOSIT,
                new BigDecimal("500.00"),
                Instant.parse("2026-08-24T10:00:00Z")
        );

        Transaction withdrawal = new Transaction(
                account,
                TransactionType.WITHDRAWAL,
                new BigDecimal("200.00"),
                Instant.parse("2026-08-24T11:00:00Z")
        );

        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository.findByAccountId(1L))
                .thenReturn(List.of(deposit, withdrawal));

        List<TransactionResponse> transactions =
                bankingService.getTransactionHistory(1L);

        assertEquals(2, transactions.size());

        TransactionResponse depositResponse = transactions.getFirst();

        assertEquals(TransactionType.DEPOSIT, depositResponse.type());
        assertEquals(new BigDecimal("500.00"), depositResponse.amount());
        assertEquals(
                Instant.parse("2026-08-24T10:00:00Z"),
                depositResponse.timestamp()
        );

        TransactionResponse withdrawalResponse = transactions.get(1);

        assertEquals(TransactionType.WITHDRAWAL, withdrawalResponse.type());
        assertEquals(new BigDecimal("200.00"), withdrawalResponse.amount());
        assertEquals(
                Instant.parse("2026-08-24T11:00:00Z"),
                withdrawalResponse.timestamp()
        );

        verify(accountRepository).findById(1L);
        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void shouldCreateAccountWithInitialBalance() {

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Account savedAccount =
                bankingService.createAccount(
                        new BigDecimal("1000.00")
                );

        assertEquals(new BigDecimal("1000.00"),
                savedAccount.getBalance()
        );

        ArgumentCaptor<Account> accountCaptor =
                ArgumentCaptor.forClass(Account.class);

        verify(accountRepository)
                .save(accountCaptor.capture());

        Account accountToSave =
                accountCaptor.getValue();

        assertEquals(
                new BigDecimal("1000.00"),
                accountToSave.getBalance()
        );
    }
}

