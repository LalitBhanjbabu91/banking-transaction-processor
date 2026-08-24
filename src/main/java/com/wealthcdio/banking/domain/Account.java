package com.wealthcdio.banking.domain;

import com.wealthcdio.banking.exception.InsufficientBalanceException;
import com.wealthcdio.banking.exception.InvalidAmountException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal balance;

    protected Account()
    {

    }

    public Account(BigDecimal balance){

        this.balance = balance;
    }
    public Long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount){

        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException(
                    "Deposit amount must be greater than zero"
            );
        }

        balance = balance.add(amount);

    }

    public void withdraw(BigDecimal amount){

        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException(
                    "Withdrawal amount must be greater than zero"
            );
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance"
            );
        }

        balance = balance.subtract(amount);
    }
}
