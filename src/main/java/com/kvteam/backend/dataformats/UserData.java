package com.kvteam.backend.dataformats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Created by maxim on 19.02.17.
 */
public class UserData {
    @Nullable
    private String username = null;
    @Nullable
    private String password = null;
    @Nullable
    private String email = null;
    @Nullable
    private Integer rating = 0;

    /**
     * Объект с информацией о достижениях(уровень и рейтинг)
     */
    public UserData(
            @Nullable String username,
            @Nullable Integer rating) {
        this.username = username;
        this.password = null;
        this.email = null;
        this.rating = rating;
    }

    /**
     * Объект с общедоступной информацией
     */
    public UserData(
            @Nullable String username,
            @Nullable String email,
            @Nullable Integer rating) {
        this.username = username;
        this.password = null;
        this.email = email;
        this.rating = rating;
    }

    /**
     * Для десериализации
     */
    @JsonCreator
    public UserData(
            @JsonProperty("username") @Nullable String username,
            @JsonProperty("password") @Nullable String password,
            @JsonProperty("email") @Nullable String email,
            @JsonProperty("rating") @Nullable Integer rating) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.rating = rating;
    }

    @Nullable
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
    public Integer getRating(){
        return rating;
    }

}
