package com.kvteam.backend.controllers;

import com.kvteam.backend.dataformats.ResponseStatusData;
import com.kvteam.backend.dataformats.UserData;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Created by maxim on 12.03.17.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@RunWith(SpringRunner.class)
public class BackendControllerTest {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private TestRestTemplate restTemplate;

    private static String getUniqueUsername(){
        return "user-(" + UUID.randomUUID().toString() + ')';
    }

    private ResponseEntity<ResponseStatusData> register(String username, String passwd, HttpStatus expectedStatus){
        final ResponseEntity<ResponseStatusData> answer = restTemplate.postForEntity(
                "/api/account",
                new UserData(
                        username,
                        passwd,
                        username + "@mail.ru",
                        0
                ),
                ResponseStatusData.class
        );
        assertEquals(expectedStatus, answer.getStatusCode());
        return answer;
    }

    @Nullable
    private UserData get(String username, HttpStatus expectedStatus, @Nullable String expectedEmail){
        final ResponseEntity<LinkedHashMap> user = restTemplate.getForEntity(
                "/api/account/"+username,
                LinkedHashMap.class
        );
        assertEquals(expectedStatus, user.getStatusCode());
        if(expectedStatus == HttpStatus.OK){
            assertEquals(
                    username,
                    (user.getBody()).get("username"));
            assertEquals(
                    expectedEmail,
                    (user.getBody()).get("email"));
            return new UserData(
                    (user.getBody()).get("username").toString(),
                    (user.getBody()).get("email").toString(),
                    0);
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private void edit(
            List<String> cookie,
            @Nullable String email,
            @Nullable String password,
            HttpStatus expectedStatus){
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.put(HttpHeaders.COOKIE, cookie);
        final HttpEntity<UserData> requestEntity = new HttpEntity<>(
                new UserData(
                        null,
                        password,
                        email,
                        0
                ),
                requestHeaders);
        final ResponseEntity<ResponseStatusData> answer =
                restTemplate.exchange("/api/account", HttpMethod.PUT, requestEntity, ResponseStatusData.class);
        assertEquals(expectedStatus, answer.getStatusCode());
    }

    private List<String> cookieFromEntity(ResponseEntity entity){
        return entity.getHeaders().get("Set-Cookie");
    }

    private ResponseEntity<ResponseStatusData> login(String username, String password, HttpStatus expectedStatus){
        final ResponseEntity<ResponseStatusData> answerLogin = restTemplate.postForEntity(
                "/api/login",
                new UserData(
                        username,
                        password
                ),
                ResponseStatusData.class
        );

        assertEquals(expectedStatus, answerLogin.getStatusCode());
        if(answerLogin.getStatusCode() == HttpStatus.OK) {
            assertEquals(username, answerLogin.getBody().getMessage());
        }
        return answerLogin;
    }

    private void logout(List<String> cookie){
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.put(HttpHeaders.COOKIE, cookie);
        final HttpEntity requestEntity = new HttpEntity(requestHeaders);
        restTemplate.exchange("/api/logout", HttpMethod.GET, requestEntity, ResponseStatusData.class);
    }

    private boolean isLoggedIn(List<String> cookie){
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.put(HttpHeaders.COOKIE, cookie);
        final HttpEntity requestEntity = new HttpEntity(requestHeaders);
        final ResponseEntity<ResponseStatusData> answer =
            restTemplate.exchange("/api/isloggedin", HttpMethod.GET, requestEntity, ResponseStatusData.class);

        return answer.getStatusCodeValue() == HttpStatus.OK.value();
    }

    @Test
    public void registerCorrect() {
        final String username = getUniqueUsername();
        final ResponseEntity<ResponseStatusData> answer = register(username, "123", HttpStatus.OK);
        assertEquals(username, answer.getBody().getMessage());
        logout(cookieFromEntity(answer));
    }

    @Test
    public void registerConflict() {
        final String username = getUniqueUsername();
        final ResponseEntity<ResponseStatusData> answer1 = register(username,"123", HttpStatus.OK);
        assertEquals(username, answer1.getBody().getMessage());

        register(username,"312", HttpStatus.CONFLICT);

        logout(cookieFromEntity(answer1));
    }

    @Test
    public void registerEmptyPassword() {
        register(getUniqueUsername(),"", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void registerEmptyUsername() {
        register("","passwd", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void getNotFound() {
        get(getUniqueUsername(), HttpStatus.NOT_FOUND, null);
    }

    @Test
    public void getCorrect() {
        final String username = getUniqueUsername();
        logout(cookieFromEntity(register(username, "passwd", HttpStatus.OK)));
        get(username, HttpStatus.OK, username + "@mail.ru");
    }


    @Test
    public void loginCorrect(){
        final String username = getUniqueUsername();
        final ResponseEntity<ResponseStatusData> answer = register(username, "123", HttpStatus.OK);
        logout(cookieFromEntity(answer));
        final ResponseEntity<ResponseStatusData> answer2 = login(username, "123", HttpStatus.OK);
        logout(cookieFromEntity(answer2));
    }


    @Test
    public void loginAccessDenied(){
        final String username = getUniqueUsername();
        login("user_not_exists", "321", HttpStatus.FORBIDDEN);
        final ResponseEntity<ResponseStatusData> answer = register(username, "123", HttpStatus.OK);
        logout(cookieFromEntity(answer));
        login(username, "321", HttpStatus.FORBIDDEN);
    }


    @Test
    public void isLoggedIn() {
        final String username = getUniqueUsername();
        assertFalse(isLoggedIn(new ArrayList<>()));
        final ResponseEntity answer = register(username, "password", HttpStatus.OK);
        assertTrue(isLoggedIn(cookieFromEntity(answer)));
        logout(cookieFromEntity(answer));
        assertFalse(isLoggedIn(cookieFromEntity(answer)));
        final ResponseEntity answer2 = login(username, "password", HttpStatus.OK);
        assertTrue(isLoggedIn(cookieFromEntity(answer2)));
        logout(cookieFromEntity(answer2));
        assertFalse(isLoggedIn(new ArrayList<>()));
    }

    @Test
    public void editCorrect(){
        final String username = getUniqueUsername();
        final ResponseEntity<ResponseStatusData> answer = register(username, "passwd", HttpStatus.OK);
        get(username, HttpStatus.OK, username + "@mail.ru");
        edit(cookieFromEntity(answer), "new" + username + "@mail.ru", "newpasswd", HttpStatus.OK);
        get(username, HttpStatus.OK, "new" + username + "@mail.ru");
        logout(cookieFromEntity(answer));
        assertFalse(isLoggedIn(cookieFromEntity(answer)));
        final ResponseEntity answer2 = login(username, "newpasswd", HttpStatus.OK);
        assertTrue(isLoggedIn(cookieFromEntity(answer2)));
        logout(cookieFromEntity(answer2));
    }

    @Test
    public void editAccessDenied(){
        final String username = getUniqueUsername();
        final ResponseEntity<ResponseStatusData> answer = register(username, "passwd", HttpStatus.OK);
        logout(cookieFromEntity(answer));
        edit(cookieFromEntity(answer), "new@mail.ru", "newpasswd", HttpStatus.FORBIDDEN);
    }
}
