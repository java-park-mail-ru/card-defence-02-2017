package com.kvteam.backend.services;

import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.dataformats.UserData;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AccountService {
    static final String SQL_GET_USER =
            "select\n" +
            "  username,\n" +
            "  email\n" +
            "from users\n" +
            "where username = ?;";

    static final String SQL_INSERT_USER =
            "insert into users (username, email, password)\n" +
            "values (?, ?, ?);";

    static final String SQL_GET_PASSWORD =
            "select \n" +
            "  password as password\n" +
            "from users\n" +
            "where username = ?;";

    static final String SQL_INSERT_SESSION =
            "insert into sessions(username)\n" +
            "  select\n" +
            "    u.username\n" +
            "  from users u\n" +
            "  where u.username = ?\n" +
            "on conflict(username) do update\n" +
            "  set\n" +
            "    id = uuid_generate_v4(),\n" +
            "    updated = now()\n" +
            "returning id;";

    static final String SQL_CHECK_SESSION =
            "update\n" +
            "  sessions\n" +
            "set \n" +
            "  updated = now()\n" +
            "where\n" +
            "  ID = ?\n" +
            "  and username = ?";

    static final String SQL_DELETE_SESSION =
            "delete from\n" +
            "  sessions\n" +
            "where\n" +
            "  ID = ?\n" +
            "  and username = ?;";

    static final String SQL_EDIT_USER =
            "update \n" +
            "  users\n" +
            "set\n" +
            "  email = case when ? <> '' \n" +
            "      then ?\n" +
            "      else email\n" +
            "  end,\n" +
            "  password = case when ? <> '' \n" +
            "      then ?\n" +
            "      else password\n" +
            "  end\n" +
            "where\n" +
            "  username = ?;";

    static final String SQL_GET_LEADERS =
            "select \n" +
            "  u.username,\n" +
            "  0 as level,\n" +
            "  1 as rating\n" +
            "from users u\n" +
            "limit ?;";

    static final String SQL_DELETE_OLD_SESSIONS =
            "delete from sessions\n" +
            "where EXTRACT(EPOCH FROM (now() - updated)) > 86400;\n";

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder encoder;


    public AccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.encoder = new BCryptPasswordEncoder();
    }


    @Nullable
    public UserData get(@NotNull String username) {
        final List<UserData> result = jdbcTemplate.query(
                SQL_GET_USER,
                new Object[]{username},
                (resultSet, i) ->
                        new UserData(
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            0,
                            1
                        )
        );
        return !result.isEmpty() ? result.get(0) : null;
    }

    public boolean add(@NotNull UserData account) {
        if(account.getUsername() == null
                || account.getPassword() == null
                || account.getEmail() == null) {
            return false;
        }

        boolean result = false;
        try{
            jdbcTemplate.update(
                    SQL_INSERT_USER,
                    account.getUsername(),
                    account.getEmail(),
                    encoder.encode(account.getPassword())
            );
            result = true;
        } catch (DataAccessException ignored) {

        }
        return result;
    }

    @Nullable
    public UUID login(@NotNull String username, @NotNull String password) {
        try {
            final String userPassword = jdbcTemplate.queryForObject(
                    SQL_GET_PASSWORD,
                    String.class,
                    username
            );
            if (encoder.matches(password, userPassword)) {
                return jdbcTemplate.queryForObject(
                        SQL_INSERT_SESSION,
                        UUID.class,
                        username
                );
            }
        } catch (EmptyResultDataAccessException ignored) {

        }

        return null;
    }

    public boolean isLoggedIn(@NotNull String username, @Nullable UUID sessionID) {
        final int rowUpdated = jdbcTemplate.update(
                SQL_CHECK_SESSION,
                sessionID,
                username
        );

        return rowUpdated != 0;
    }

    public void tryLogout(@NotNull String username, @Nullable UUID sessionID) {
        jdbcTemplate.update(
                SQL_DELETE_SESSION,
                sessionID,
                username
        );
    }

    public void editAccount(
            @NotNull String username,
            @Nullable UUID sessionID,
            @Nullable String newEmail,
            @Nullable String newPassword)
            throws AccessDeniedException {
        if (sessionID != null
                && isLoggedIn(username, sessionID)) {
            jdbcTemplate.update(
                    SQL_EDIT_USER,
                    newEmail != null ? newEmail : "",
                    newEmail != null ? newEmail : "",
                    newPassword != null ? encoder.encode(newPassword) : "",
                    newPassword != null ? encoder.encode(newPassword) : "",
                    username
            );
        } else {
            throw new AccessDeniedException();
        }
    }


    public List<UserData> getLeaders(@Nullable Integer limit){
        return jdbcTemplate.query(
                SQL_GET_LEADERS,
                new Object[]{limit != null ? limit : Integer.MAX_VALUE},
                (resultSet, i) ->
                        new UserData(
                                resultSet.getString("username"),
                                resultSet.getInt("rating"),
                                resultSet.getInt("level")
                        )
        );
    }

    /**
     * Каждые полдня удаление сессий, на которые давно не заходили
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 12)
    private void deleteOldSessions(){
        jdbcTemplate.update(SQL_DELETE_OLD_SESSIONS);
    }

}
