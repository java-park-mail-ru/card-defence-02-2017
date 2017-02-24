package com.kvteam.backend.userdata;

import java.util.UUID;

/**
 * Created by maxim on 19.02.17.
 */
public class UserData {
    private String username = "";
    private String password = "";
    private String email = "";
    private UUID sessionID = null;


    public UserData(
            String username ,
            String password,
            String email,
            UUID sessionID){
        this.username = username;
        this.password = password;
        this.email = email;
        this.sessionID = sessionID;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public String getEmail(){
        return email;
    }

    public UUID getSessionID() { return sessionID;}
}
