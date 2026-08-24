package com.wealthcdio.banking.controller;


import com.wealthcdio.banking.domain.Account;
import com.wealthcdio.banking.domain.Transaction;
import com.wealthcdio.banking.dto.*;
import com.wealthcdio.banking.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService){

        this.bankingService = bankingService;
    }

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<Void> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody AmountRequest request){

        bankingService.deposit(accountId, request.amount());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Void> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody AmountRequest request){


        bankingService.withdraw(accountId, request.amount());

            return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            @Valid @RequestBody TransferRequest request){

        bankingService.transfer(request.sourceAccountId(),
                request.destinationAccountId(), request.amount());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long accountId){


            return ResponseEntity.ok(
                    new BalanceResponse(
                            accountId,
                            bankingService.getBalance(accountId)
                    )
            );

    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable Long accountId){

        return ResponseEntity.ok(
                bankingService.getTransactionHistory(accountId)
        );

    }

    @PostMapping
    public ResponseEntity<BalanceResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request){

        Account account = bankingService.createAccount(request.initialBalance());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        new BalanceResponse(
                                account.getId(),
                                account.getBalance()
                        )
                );

    }
}
