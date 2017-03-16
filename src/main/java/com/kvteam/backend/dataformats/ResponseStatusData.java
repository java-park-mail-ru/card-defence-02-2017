package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

/**
 * Created by maxim on 28.02.17.
 */
public class ResponseStatusData {
    public static final ResponseStatusData SUCCESS =
            new ResponseStatusData(HttpStatus.OK.value(), "success");

    public static final ResponseStatusData INVALID_REQUEST =
            new ResponseStatusData(HttpStatus.BAD_REQUEST.value(), "invalid request");

    public static final ResponseStatusData UNAUTHORIZED =
            new ResponseStatusData(HttpStatus.UNAUTHORIZED.value(), "unauthorized");

    public static final ResponseStatusData ACCESS_DENIED =
            new ResponseStatusData(HttpStatus.FORBIDDEN.value(), "Access denied");

    public static final ResponseStatusData NOT_FOUND =
            new ResponseStatusData(HttpStatus.NOT_FOUND.value(), "object not found");

    public static final ResponseStatusData CONFLICT =
            new ResponseStatusData(HttpStatus.CONFLICT.value(), "already exists");

    private int code = HttpStatus.OK.value();
    private String message = "Internal error";

    @JsonCreator
    public ResponseStatusData(
            @JsonProperty("code") int statusCode,
            @JsonProperty("message") String mess) {
        code = statusCode;
        message = mess;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
