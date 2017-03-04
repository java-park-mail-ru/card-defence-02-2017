package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by maxim on 19.02.17.
 */
public class UserData {
    private String username = null;
    @Nullable
    private String password = null;
    @Nullable
    private String email = null;

    @JsonCreator
    public UserData(
            @JsonProperty("username") @Nullable String username,
            @JsonProperty("password") @Nullable String password,
            @JsonProperty("email") @Nullable String email) {
        this.username = username;
        this.password = password;
        this.email = email;
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
}
