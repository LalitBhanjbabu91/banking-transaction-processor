package com.wealthcdio.banking.controller;


import com.wealthcdio.banking.domain.Account;
import com.wealthcdio.banking.domain.Transaction;
import com.wealthcdio.banking.domain.TransactionType;
import com.wealthcdio.banking.dto.TransactionResponse;
import com.wealthcdio.banking.exception.AccountNotFoundException;
import com.wealthcdio.banking.exception.GlobalExceptionHandler;
import com.wealthcdio.banking.exception.InsufficientBalanceException;
import com.wealthcdio.banking.repository.AccountRepository;
import com.wealthcdio.banking.service.BankingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankingService bankingService;


    @Test
    void shouldReturnAccountBalance() throws Exception {

            when(bankingService.getBalance(1L))
                    .thenReturn(new BigDecimal("1000.00"));

            mockMvc.perform( get("/api/accounts/1/balance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(1))
                    .andExpect(jsonPath("$.balance").value(1000.00));

    }

    @Test
    void shouldDepositMoney() throws Exception {

        mockMvc.perform(
                post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "amount": 500.00
                            }
                            """)
        ).andExpect(status().isOk());

        verify(bankingService).deposit(1L, new BigDecimal("500.00"));

    }

    @Test
    void shouldWithdrawMoney() throws Exception {

        mockMvc.perform(
                post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "amount": 300.00
                            }
                            """)
        ).andExpect(status().isOk());

        verify(bankingService).withdraw(1L, new BigDecimal("300.00"));

    }

    @Test
    void shouldTransferMoney() throws Exception {

        mockMvc.perform(
                post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "sourceAccountId": 1,
                              "destinationAccountId": 2,
                              "amount": 300.00
                            }
                            """)

        ).andExpect(status().isOk());

        verify(bankingService).transfer(1L,2L, new BigDecimal("300.00"));

    }

    @Test
    void shouldReturnTransactionHistory() throws Exception {
        Account account = new Account(new BigDecimal("1000.00"));

        TransactionResponse deposit = new TransactionResponse(
                1L,
                1L,
                TransactionType.DEPOSIT,
                new BigDecimal("500.00"),
                Instant.parse("2026-08-24T10:00:00Z")
        );

        TransactionResponse withdrawal = new TransactionResponse(
                2L,
                1L,
                TransactionType.WITHDRAWAL,
                new BigDecimal("200.00"),
                Instant.parse("2026-08-24T11:00:00Z")
        );


        when(bankingService.getTransactionHistory(1L))
                .thenReturn(List.of(deposit, withdrawal));

        mockMvc.perform(
                get("/api/accounts/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(500.00))
                .andExpect(jsonPath("$[1].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[1].amount").value(200.00));

        verify(bankingService).getTransactionHistory(1L);
    }

    @Test
    void shouldRejectZeroDepositAmount() throws Exception {

        mockMvc.perform(
                post("/api/accounts/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "amount": 0
                            }
                            """)
        ).andExpect(status().isBadRequest());

        verify(bankingService, never())
                .deposit(anyLong(), any(BigDecimal.class));
    }

    @Test
    void shouldRejectNegativeWithdrawalAmount() throws Exception {

        mockMvc.perform(
                post("/api/accounts/1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "amount": -100.00
                            }
                            """)
        ).andExpect(status().isBadRequest());

        verify(bankingService, never())
                .withdraw(anyLong(), any(BigDecimal.class));
    }

    @Test
    void shouldRejectZeroTransferAmount() throws Exception {


        mockMvc.perform(
                post("/api/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "sourceAccountId": 1,
                              "destinationAccountId": 2,
                              "amount": 0
                            }
                            """)
        ).andExpect(status().isBadRequest());

        verify(bankingService, never())
                .transfer(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    void shouldRejectTransferWhenDestinationAccountIdIsMissing() throws Exception {

        mockMvc.perform(
                        post("/api/accounts/transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "sourceAccountId": 1,
                              "amount": 300.00
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verify(bankingService, never())
                .transfer(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    void shouldReturn404WhenAccountDoesNotExist() throws Exception {

        when(bankingService.getBalance(99L))
                .thenThrow(
                        new AccountNotFoundException(
                                "Account not found with id: 99"
                        )
                );

        mockMvc.perform(
                get("/api/accounts/99/balance")
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Account not found with id: 99"));
    }


    @Test
    void shouldCreateAccountAndReturn201() throws Exception {

        Account account =
                new Account(new BigDecimal("1000.00"));

        when(bankingService.createAccount(
                new BigDecimal("1000.00")))
                .thenReturn(account);

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "initialBalance": 1000.00
                            }
                            """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(1000.00));

        verify(bankingService)
                .createAccount(
                        new BigDecimal("1000.00")
                );
    }

    @Test
    void shouldRejectAccountCreationWithNegativeInitialBalance()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "initialBalance": -100.00
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verify(bankingService, never())
                .createAccount(any(BigDecimal.class));
    }

    @Test
    void shouldRejectAccountCreationWhenInitialBalanceIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verify(bankingService, never())
                .createAccount(any(BigDecimal.class));
    }
}

