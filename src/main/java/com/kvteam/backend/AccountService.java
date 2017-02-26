package com.kvteam.backend;

import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.userdata.UserAccount;
import com.kvteam.backend.userdata.UserData;
import com.sun.istack.internal.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private Map<String, UserAccount> users = new HashMap<>();

    public UserData get(String username){
        if(users.containsKey(username)){
            return new UserData(
                    users.get(username).getUsername(),
                    null,
                    users.get(username).getEmail(),
                    null);
        }
        return null;
    }

    public void add(UserData account) throws UserAlreadyExistException{
        if(users.containsKey(account.getUsername())){
            throw new UserAlreadyExistException();
        }
        users.put(account.getUsername(),
                new UserAccount(
                        account.getUsername(),
                        account.getPassword(),
                        account.getEmail()
                ));
    }

    public UUID login(String username, String password){
        UUID sessionID = null;
        if(users.containsKey(username)){
            sessionID = users.get(username).authenticate(password);
        }
        return sessionID;
    }

    public boolean isLoggedIn(String username, UUID sessionID){
        return users.containsKey(username) && users.get(username).checkSession(sessionID);
    }

    public void tryLogout(String username, UUID sessionID){
        if(users.containsKey(username)){
            users.get(username).endSession(sessionID);
        }
    }

    public void editAccount(
            String username,
            UUID sessionID,
            String newEmail,
            String newPassword)
            throws  AccessDeniedException{
        if(isLoggedIn(username, sessionID)){
            if(!newPassword.isEmpty()){
                users.get(username).setPassword(newPassword);
            }
            if(!newEmail.isEmpty()){
                users.get(username).setEmail(newEmail);
            }
        }else{
            throw new AccessDeniedException();
        }
    }

}
