package com.disasterrescue.api.exception;

public final class ErrorCode {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_PENDING = "ACCOUNT_PENDING";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String INVALID_RESET_TOKEN = "INVALID_RESET_TOKEN";
    public static final String USERNAME_TAKEN = "USERNAME_TAKEN";
    public static final String EMAIL_TAKEN = "EMAIL_TAKEN";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String CANNOT_MODIFY_SELF = "CANNOT_MODIFY_SELF";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private ErrorCode() {
    }
}
