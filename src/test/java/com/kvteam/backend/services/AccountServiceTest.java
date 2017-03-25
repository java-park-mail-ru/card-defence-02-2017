package com.kvteam.backend.services;

import com.kvteam.backend.dataformats.UserData;
import com.kvteam.backend.exceptions.AccessDeniedException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

/**
 * Created by maxim on 12.03.17.
 */
public class AccountServiceTest {
    @Mock
    private JdbcTemplate template;

    private AccountService accountService;

    private BCryptPasswordEncoder encoder;

    private static String getUniqueUsername(){
        return "user-(" + UUID.randomUUID().toString() + ')';
    }

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
        accountService = new AccountService(template);
        encoder = new BCryptPasswordEncoder();
    }

    private boolean addUser(@NotNull String username, boolean conflicted){
        final OngoingStubbing<Integer> s = when(
                template.update(
                        eq(AccountService.SQL_INSERT_USER),
                        eq(username),
                        eq(username + "@mail.ru"),
                        anyString()
                )
        );
        if(conflicted){
            s.thenThrow(new DuplicateKeyException("user already exists"));
        } else {
            s.thenReturn(1);
        }

        return accountService.add(
                new UserData(
                        username,
                        "passwd",
                        username + "@mail.ru",
                        null,
                        null
                )
        );
    }

    private void setGetMock(){
        when(
                template.query(
                        eq(AccountService.SQL_GET_USER),
                        new Object[]{anyString()},
                        any(RowMapper.class)
                )
        ).thenReturn( new ArrayList<>() );
    }

    private void setGetMock(@NotNull UserData value){
        when(
                template.query(
                        eq(AccountService.SQL_GET_USER),
                        new Object[]{anyString()},
                        any(RowMapper.class)
                )
        ).thenReturn( Collections.singletonList( value ) );

    }

    private UUID setCorrectLoginMock(
            @NotNull String username,
            @NotNull String password){
        when(
                template.queryForObject(
                        eq(AccountService.SQL_GET_PASSWORD),
                        eq(String.class),
                        eq(username)
                )
        ).thenReturn(encoder.encode(password));
        final UUID mockSessionID = UUID.randomUUID();
        when(
                template.queryForObject(
                        eq(AccountService.SQL_INSERT_SESSION),
                        eq(UUID.class),
                        eq(username)
                )
        ).thenReturn(mockSessionID);
        return mockSessionID;
    }

    private void setFailedLoginMock(@NotNull String username){
        when(
                template.queryForObject(
                        eq(AccountService.SQL_GET_PASSWORD),
                        eq(String.class),
                        eq(username)
                )
        ).thenThrow(new EmptyResultDataAccessException(0));
    }

    private void setIsLoggedInMock(
            @NotNull UUID mockSessionID,
            @NotNull String username,
            @NotNull Integer value){
        when(
                template.update(
                        eq(AccountService.SQL_CHECK_SESSION),
                        eq(mockSessionID),
                        eq(username)
                )
        ).thenReturn(value);
    }

    private void setTryLogoutMock(
            @NotNull UUID mockSessionID,
            @NotNull String username){
        when(
                template.update(
                        eq(AccountService.SQL_DELETE_SESSION),
                        eq(mockSessionID),
                        eq(username)
                )
        ).thenReturn(1);
    }

    private void setEditMock(@NotNull String username){
        when(
                template.update(
                        eq(AccountService.SQL_EDIT_USER),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq(username)
                )
        ).thenReturn(1);
    }

    @Test
    public void addCorrectly(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username, false));
    }



    @Test
    public void addConflict(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username, false));
        assertFalse(addUser(username, true));
    }

    @Test
    public void getCorrectly(){
        final String username = getUniqueUsername();
        setGetMock(new UserData (username,username + "@mail.ru", 0, 1 ));

        assertTrue(addUser(username, false));

        final UserData data = accountService.get(username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        assertNull(data.getPassword());
        assertEquals(username + "@mail.ru", data.getEmail());
    }

    @Test
    public void getNotFound(){
        setGetMock();
        final UserData data = accountService.get("not_exist");
        assertNull(data);
    }

    @Test
    public void loginLogoutCorrectly(){
        final String username = getUniqueUsername();
        final UUID mockSessionID = setCorrectLoginMock(username, "passwd");

        assertTrue(addUser(username, false));
        final UUID sessionID = accountService.login(username, "passwd");
        assertNotNull(sessionID);

        setIsLoggedInMock(mockSessionID, username, 1);
        assertTrue(accountService.isLoggedIn(username, sessionID));

        setTryLogoutMock(mockSessionID, username);
        accountService.tryLogout(username, sessionID);

        setIsLoggedInMock(mockSessionID, username, 0);
        assertFalse(accountService.isLoggedIn(username, sessionID));
    }

    @Test
    public void loginAccessDenied(){
        final String username = getUniqueUsername();

        assertTrue(addUser(username, false));
        setFailedLoginMock("IamNotExist");
        assertNull(accountService.login("IamNotExist", "passwd"));
        setFailedLoginMock(username);
        assertNull(accountService.login(username, "incorrect passwd"));
    }

    @Test
    public void editEmail(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username, false));
        final UUID mockSessionID = setCorrectLoginMock(username, "passwd");
        final UUID sessionID = accountService.login(username, "passwd");
        assertNotNull(sessionID);

        setIsLoggedInMock(mockSessionID, username, 1);
        setEditMock(username);
        try {
            accountService.editAccount(
                    username,
                    sessionID,
                    "new" + username + "@mail.ru",
                    null);
        }catch(AccessDeniedException e){
            assertNotNull(e);
        }

        setGetMock( new UserData(username, "new" + username + "@mail.ru", 0, 1 ));
        final UserData data = accountService.get(username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        assertNull(data.getPassword());
        assertEquals("new" + username + "@mail.ru", data.getEmail());
    }


    @Test
    public void editPassword(){
        final String username = getUniqueUsername();

        assertTrue(addUser(username, false));
        final UUID mockSessionID = setCorrectLoginMock(username, "passwd");
        final UUID sessionID = accountService.login(username, "passwd");
        assertNotNull(sessionID);
        setEditMock(username);
        try {
            accountService.editAccount(
                    username,
                    sessionID,
                    null,
                    "newpasswd");
        }catch(AccessDeniedException e){
            assertNotNull(e);
        }
        setTryLogoutMock(mockSessionID, username);
        accountService.tryLogout(username, sessionID);

        setFailedLoginMock(username);
        final UUID sessionID2 = accountService.login(username, "passwd");
        assertNull(sessionID2);

        setFailedLoginMock(username);
        final UUID sessionID3 = accountService.login(username, "random");
        assertNull(sessionID3);

        setCorrectLoginMock(username, "newpasswd");
        final UUID sessionID4 = accountService.login(username, "newpasswd");
        assertNotNull(sessionID4);
    }

    @Test
    public void editAccessDenied(){
        final String username = getUniqueUsername();
        final String username2 = getUniqueUsername();

        assertTrue(addUser(username, false));
        assertTrue(addUser(username2, false));
        final UUID mockSessionID = setCorrectLoginMock(username2, "passwd");
        final UUID sessionID = accountService.login(username2, "passwd");
        assertNotNull(sessionID);

        setIsLoggedInMock(mockSessionID, username, 0);
        setEditMock(username);
        boolean raised = false;
        try {
            accountService.editAccount(
                    username,
                    sessionID,
                    null,
                    "newpasswd");
        }catch(AccessDeniedException e){
            raised = true;
        }
        assertTrue(raised);
    }


}
