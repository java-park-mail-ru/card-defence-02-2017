package com.kvteam.backend.users;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.util.UUID;

/**
 * Created by maxim on 19.02.17.
 */
public class UserAccount {
    private String username = "";
    private String password = "";
    private String email = null;
    private UUID sessionID = null;

    public UserAccount(@NotNull String username,
                       @NotNull String password,
                       @Nullable String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Nullable
    public UUID authenticate(@NotNull String passwd){
        if(this.password.equals(passwd)){
            sessionID = UUID.randomUUID();
            return sessionID;
        }else{
            return null;
        }
    }

    public boolean checkSession(@Nullable UUID sessID){
        return this.sessionID != null
                && this.sessionID.equals(sessID);
    }

    public void endSession(@Nullable UUID sessID){
        if(this.sessionID != null
                && this.sessionID.equals(sessID)){
            this.sessionID = null;
        }
    }

    @Nullable
    public String getEmail(){
        return email;
    }
    @NotNull
    public String getUsername(){
        return username;
    }

    public void setEmail(@Nullable String em) {
        email = em;
    }

    public void setPassword(@NotNull String newPassword){
        this.password = newPassword;
    }
}
