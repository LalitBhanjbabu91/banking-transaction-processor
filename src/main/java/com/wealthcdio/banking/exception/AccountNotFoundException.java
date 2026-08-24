package com.wealthcdio.banking.exception;



public class AccountNotFoundException extends RuntimeException{

    public AccountNotFoundException(String message){

        super(message);
    }
}
