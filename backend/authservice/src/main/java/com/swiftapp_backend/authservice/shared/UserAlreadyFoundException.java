package com.swiftapp_backend.authservice.shared;

public class UserAlreadyFoundException extends RuntimeException{
    public UserAlreadyFoundException(String message){
        super(message);
    }
}
