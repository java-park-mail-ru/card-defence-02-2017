package com.kvteam.backend.services;

import com.kvteam.backend.Application;
import com.kvteam.backend.dataformats.UserData;
import com.kvteam.backend.exceptions.AccessDeniedException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by maxim on 12.03.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountServiceTest {
    @Autowired
    private JdbcTemplate template;

    private AccountService accountService;

    private static String getUniqueUsername(){
        return "user-(" + UUID.randomUUID().toString() + ')';
    }

    @Before
    public void setup(){
        accountService = new AccountService(template);
    }

    private boolean addUser(@NotNull String username){
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

    @Test
    public void addCorrectly(){
        assertTrue(addUser(getUniqueUsername()));
    }

    @Test
    public void addConflict(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username));
        assertFalse(addUser(username));
    }

    @Test
    public void getCorrectly(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username));

        final UserData data = accountService.get(username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        assertNull(data.getPassword());
        assertEquals(username + "@mail.ru", data.getEmail());
    }

    @Test
    public void getNotFound(){
        final UserData data = accountService.get("not_exist");
        assertNull(data);
    }

    @Test
    public void loginLogoutCorrectly(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username));
        final UUID sessionID = accountService.login(username, "passwd");
        assertNotNull(sessionID);
        assertTrue(accountService.isLoggedIn(username, sessionID));
        accountService.tryLogout(username, sessionID);
        assertFalse(accountService.isLoggedIn(username, sessionID));
    }

    @Test
    public void loginAccessDenied(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username));
        assertNull(accountService.login("IamNotExist", "passwd"));
        assertNull(accountService.login(username, "incorrect passwd"));
    }

    @Test
    public void editEmail(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username));
        final UUID sessionID = accountService.login(username, "passwd");
        assertNotNull(sessionID);
        try {
            accountService.editAccount(
                    username,
                    sessionID,
                    "new" + username + "@mail.ru",
                    null);
        }catch(AccessDeniedException e){
            assertNotNull(e);
        }
        final UserData data = accountService.get(username);
        assertNotNull(data);
        assertEquals(username, data.getUsername());
        assertNull(data.getPassword());
        assertEquals("new" + username + "@mail.ru", data.getEmail());
    }


    @Test
    public void editPassword(){
        final String username = getUniqueUsername();
        assertTrue(addUser(username));
        final UUID sessionID = accountService.login(username, "passwd");
        assertNotNull(sessionID);
        try {
            accountService.editAccount(
                    username,
                    sessionID,
                    null,
                    "newpasswd");
        }catch(AccessDeniedException e){
            assertNotNull(e);
        }
        accountService.tryLogout(username, sessionID);
        final UUID sessionID2 = accountService.login(username, "passwd");
        assertNull(sessionID2);
        final UUID sessionID3 = accountService.login(username, "random");
        assertNull(sessionID3);
        final UUID sessionID4 = accountService.login(username, "newpasswd");
        assertNotNull(sessionID4);
    }

    @Test
    public void editAccessDenied(){
        final String username = getUniqueUsername();
        final String username2 = getUniqueUsername();
        assertTrue(addUser(username));
        assertTrue(addUser(username2));
        final UUID sessionID = accountService.login(username2, "passwd");
        assertNotNull(sessionID);
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
