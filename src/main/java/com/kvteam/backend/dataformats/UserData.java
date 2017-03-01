package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.util.UUID;

/**
 * Created by maxim on 19.02.17.
 */
public class UserData {
    private String username = null;
    private String password = null;
    private String email = null;
    private UUID sessionID = null;

    @JsonCreator
    public UserData(
            @JsonProperty("username") @NotNull String username,
            @JsonProperty("password") @Nullable String password,
            @JsonProperty("email") @Nullable String email,
            @JsonProperty("sessionID") @Nullable UUID sessionID) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.sessionID = sessionID;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public UUID getSessionID() {
        return sessionID;
    }
}
