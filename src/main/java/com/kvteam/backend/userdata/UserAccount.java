package com.kvteam.backend.userdata;

import java.util.UUID;

/**
 * Created by maxim on 19.02.17.
 */
public class UserAccount {
    private String username = "";
    private String password = "";
    private String email = "";
    private UUID sessionID = null;

    public UserAccount(String username,
                       String password,
                       String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public UUID authenticate(String password){
        if(this.password.equals(password)){
            sessionID = UUID.randomUUID();
            return sessionID;
        }else{
            return null;
        }
    }

    public boolean checkSession(UUID sessionID){
        return this.sessionID != null
             && this.sessionID.equals(sessionID);
    }

    public void endSession(UUID sessionID){
        if(this.sessionID != null
                && this.sessionID.equals(sessionID)){
            this.sessionID = null;
        }
    }

    public String getEmail(){
        return email;
    }

    public String getUsername(){
        return username;
    }
}
