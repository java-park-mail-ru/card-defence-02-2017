package com.kvteam.backend.services;

import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.users.UserAccount;
import com.kvteam.backend.dataformats.UserData;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountService {
    private Map<String, UserAccount> users = new HashMap<>();

    @Nullable
    public UserData get(@NotNull String username) {
        if (users.containsKey(username)) {
            final UserAccount acc = users.get(username);
            return new UserData(
                    acc.getUsername(),
                    null,
                    acc.getEmail(),
                    0,
                    1);
        }
        return null;
    }

    public boolean add(@NotNull UserData account) {
        if (users.containsKey(account.getUsername())) {
            return false;
        }
        users.put(account.getUsername(),
                new UserAccount(
                        account.getUsername(),
                        account.getPassword(),
                        account.getEmail()
                ));
        return true;
    }

    @Nullable
    public UUID login(@NotNull String username, @NotNull String password) {
        UUID sessionID = null;
        if (users.containsKey(username)) {
            sessionID = users.get(username).authenticate(password);
        }
        return sessionID;
    }

    public boolean isLoggedIn(@NotNull String username, @Nullable UUID sessionID) {
        return users.containsKey(username) && users.get(username).checkSession(sessionID);
    }

    public void tryLogout(@NotNull String username, @Nullable UUID sessionID) {
        if (users.containsKey(username)) {
            users.get(username).endSession(sessionID);
        }
    }

    public void editAccount(
            @NotNull String username,
            @Nullable UUID sessionID,
            @Nullable String newEmail,
            @Nullable String newPassword)
            throws AccessDeniedException {
        if (sessionID != null
                && isLoggedIn(username, sessionID)) {
            if (newPassword != null) {
                users.get(username).setPassword(newPassword);
            }
            if (newEmail != null) {
                users.get(username).setEmail(newEmail);
            }
        } else {
            throw new AccessDeniedException();
        }
    }


    public List<UserData> getLeaders(@Nullable Integer limit){
        limit = limit == null ? Integer.MAX_VALUE : limit;

        final List<UserData> list = new ArrayList<>();
        for (UserAccount value : users.values()) {
            if(list.size() == limit){
                break;
            }
            list.add(
                    new UserData(value.getUsername(),
                            null,
                            null,
                            0,
                            1)
            );
        }

        return list;
    }

}
