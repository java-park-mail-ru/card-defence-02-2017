package com.kvteam.backend.dataformats;

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


    public UserData(
            @NotNull String username ,
            @Nullable String password,
            @Nullable String email,
            @Nullable UUID sessionID){
        this.username = username;
        this.password = password;
        this.email = email;
        this.sessionID = sessionID;
    }

    @NotNull
    public String getUsername(){
        return username;
    }
    @Nullable
    public String getPassword(){
        return password;
    }
    @Nullable
    public String getEmail(){
        return email;
    }
    @Nullable
    public UUID getSessionID() { return sessionID;}
}
