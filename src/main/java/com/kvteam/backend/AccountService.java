package com.kvteam.backend;

import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.users.UserAccount;
import com.kvteam.backend.dataformats.UserData;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private Map<String, UserAccount> users = new HashMap<>();

    @Nullable
    public UserData get(@NotNull String username){
        if(users.containsKey(username)){
            return new UserData(
                    users.get(username).getUsername(),
                    null,
                    users.get(username).getEmail(),
                    null);
        }
        return null;
    }

    public void add(@NotNull UserData account) throws UserAlreadyExistException{
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

    @Nullable
    public UUID login(@NotNull String username, @NotNull String password){
        UUID sessionID = null;
        if(users.containsKey(username)){
            sessionID = users.get(username).authenticate(password);
        }
        return sessionID;
    }

    public boolean isLoggedIn(@NotNull String username, @Nullable UUID sessionID){
        return users.containsKey(username) && users.get(username).checkSession(sessionID);
    }

    public void tryLogout(@NotNull String username, @Nullable UUID sessionID){
        if(users.containsKey(username)){
            users.get(username).endSession(sessionID);
        }
    }

    public void editAccount(
            @NotNull String username,
            @Nullable UUID sessionID,
            @Nullable String newEmail,
            @Nullable String newPassword)
            throws  AccessDeniedException{
        if(sessionID != null
                && isLoggedIn(username, sessionID)){
            if(newPassword != null){
                users.get(username).setPassword(newPassword);
            }
            if(newEmail != null){
                users.get(username).setEmail(newEmail);
            }
        }else{
            throw new AccessDeniedException();
        }
    }

}
