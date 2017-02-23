package com.kvteam.backend;

import com.kvteam.backend.Exceptions.UserAlreadyExistException;
import com.kvteam.backend.userdata.UserAccount;
import com.kvteam.backend.userdata.UserData;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by maxim on 19.02.17.
 */
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

}
