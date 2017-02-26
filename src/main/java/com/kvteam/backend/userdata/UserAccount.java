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

    public UUID authenticate(String passwd){
        if(this.password.equals(passwd)){
            sessionID = UUID.randomUUID();
            return sessionID;
        }else{
            return null;
        }
    }

    public boolean checkSession(UUID sessID){
        return this.sessionID != null
                && this.sessionID.equals(sessID);
    }

    public void endSession(UUID sessID){
        if(this.sessionID != null
                && this.sessionID.equals(sessID)){
            this.sessionID = null;
        }
    }

    public String getEmail(){
        return email;
    }

    public String getUsername(){
        return username;
    }

    public void setEmail(String em) {
        email = em;
    }

    public void setPassword(String newPassword){
        this.password = newPassword;
    }
}
