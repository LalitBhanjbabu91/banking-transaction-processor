package com.wealthcdio.banking.domain;


import com.wealthcdio.banking.exception.InsufficientBalanceException;
import com.wealthcdio.banking.exception.InvalidAmountException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AccountTest {

     @Test
     void shouldDepositMoney(){

         Account account = new Account(new BigDecimal("1000.00"));
         account.deposit(new BigDecimal("500.00"));

         assertEquals(new BigDecimal("1500.00"), account.getBalance());

     }

     @Test
     void shouldRejectZeroDeposit(){

         Account account = new Account(new BigDecimal("1000.00"));
         assertThrows(InvalidAmountException.class,
                 () -> account.deposit(new BigDecimal("0")));
     }

     @Test
     void shouldRejectNegativeDeposit(){

         Account account = new Account(new BigDecimal("1000.00"));
         assertThrows(InvalidAmountException.class,
                 () -> account.deposit(new BigDecimal("-100.00")));

     }

     @Test
     void shouldWithdrawMoney(){

         Account account = new Account(new BigDecimal("1000.00"));
         account.withdraw(new BigDecimal("500.00"));

         assertEquals(new BigDecimal("500.00"), account.getBalance());
     }

     @Test
     void shouldRejectZeroWithdrawal(){

         Account account = new Account(new BigDecimal("1000.00"));
         assertThrows(InvalidAmountException.class,
                 () -> account.withdraw(new BigDecimal("0")));
     }

     @Test
     void shouldRejectNegativeWithdrawal(){

         Account account = new Account(new BigDecimal("1000.00"));
         assertThrows(InvalidAmountException.class,
                 () -> account.withdraw(new BigDecimal("-100.00")));
     }

     @Test
     void shouldRejectWithdrawalWhenBalanceIsInsufficient(){

         Account account = new Account(new BigDecimal("1000.00"));
         assertThrows(InsufficientBalanceException.class,
                 () -> account.withdraw(new BigDecimal("1500.00")));


     }
}
