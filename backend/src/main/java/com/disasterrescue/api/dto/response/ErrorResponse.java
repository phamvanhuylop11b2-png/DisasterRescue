package com.disasterrescue.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private List<FieldErrorItem> fieldErrors;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(int status, String code, String message) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(int status, String code, String message, List<FieldErrorItem> fieldErrors) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldErrorItem> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
