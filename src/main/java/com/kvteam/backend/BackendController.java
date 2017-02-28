package com.kvteam.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kvteam.backend.dataformats.ResponseStatusData;
import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.dataformats.UserData;
import com.sun.istack.internal.Nullable;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;


@RestController
public class BackendController {
    private AccountService accountService;
    public BackendController(AccountService accountService) {
        this.accountService = accountService;
    }

    private boolean isStringNullOrEmpty(@Nullable String str){
        return str == null || str.isEmpty();
    }

    @Nullable
    private UserData parseUserData(String body){
        UserData userData = null;
        try {
            try (Reader reader = new StringReader(body)) {
                final Gson gson = new GsonBuilder().create();
                userData = gson.fromJson(reader, UserData.class);
            }
        }catch(IOException
                | NumberFormatException ignored){
        }
        return userData;
    }

    @RequestMapping(path = "/api/login", method = RequestMethod.POST, produces = "application/json")
    public String login(
            @RequestBody String body,
            HttpServletResponse response) {
        final UserData userData = parseUserData(body);
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())
                || isStringNullOrEmpty(userData.getPassword())){
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return (new Gson()).toJson(ResponseStatusData.INVALID_REQUEST);
        }
        if(userData.getSessionID() != null) {
            accountService.tryLogout(
                    userData.getUsername(),
                    userData.getSessionID());
        }

        UserData answerData = null;
        final UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
        if(sessionID != null){
            response.setStatus(HttpStatus.OK_200);
            answerData = new UserData(
                    userData.getUsername(),
                    null,
                    null,
                    sessionID);
        }else{
            response.setStatus(HttpStatus.FORBIDDEN_403);
        }

        return answerData != null ?
                (new Gson()).toJson(answerData):
                (new Gson()).toJson(ResponseStatusData.ACCESS_DENIED);
    }

    @RequestMapping(path = "/api/logout", method = RequestMethod.POST, produces = "application/json")
    public String logout(@RequestBody String body,
                         HttpServletResponse response) {
        final UserData userData = parseUserData(body);
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return (new Gson()).toJson(ResponseStatusData.INVALID_REQUEST);
        }
        accountService.tryLogout(
                userData.getUsername(),
                userData.getSessionID());
        return (new Gson()).toJson(ResponseStatusData.SUCCESS);
    }

    @RequestMapping(path = "/api/isloggedin", method = RequestMethod.POST, produces = "application/json")
    public String isLoggedIn(@RequestBody String body,
                             HttpServletResponse response) {
        final UserData userData = parseUserData(body);
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return (new Gson()).toJson(ResponseStatusData.INVALID_REQUEST);
        }

        final boolean isLoggedIn =
                accountService.isLoggedIn(userData.getUsername(), userData.getSessionID());
        final String answer;
        if(isLoggedIn){
            response.setStatus(HttpStatus.OK_200);
            answer = (new Gson()).toJson(ResponseStatusData.SUCCESS);
        }else{
            response.setStatus(HttpStatus.FORBIDDEN_403);
            answer = (new Gson()).toJson(ResponseStatusData.ACCESS_DENIED);
        }

        return answer;
    }

    @RequestMapping(path = "/api/register", method = RequestMethod.POST, produces = "application/json")
    public String register(
            @RequestBody String body,
            HttpServletResponse response) {
        final UserData userData = parseUserData(body);
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())
                || isStringNullOrEmpty(userData.getPassword())){
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return (new Gson()).toJson(ResponseStatusData.INVALID_REQUEST);
        }
        if(userData.getSessionID() != null) {
            accountService.tryLogout(
                    userData.getUsername(),
                    userData.getSessionID());
        }

        UserData answerData = null;
        String errorAnswer = "";
        try {
            accountService.add(userData);
            final UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
            if(sessionID != null){
                response.setStatus(HttpStatus.OK_200);
                answerData = new UserData(
                        userData.getUsername(),
                        null,
                        null,
                        sessionID);
            }else{
                response.setStatus(HttpStatus.FORBIDDEN_403);
                errorAnswer = (new Gson()).toJson(ResponseStatusData.ACCESS_DENIED);
            }
        }catch (UserAlreadyExistException e){
            response.setStatus(HttpStatus.CONFLICT_409);
            errorAnswer = (new Gson()).toJson(ResponseStatusData.CONFLICT);
        }
        return answerData != null ?
                (new Gson()).toJson(answerData):
                errorAnswer;
    }


    @RequestMapping(path = "/api/editaccount", method = RequestMethod.POST, produces = "application/json")
    public String editAccount(
            @RequestBody String body,
            HttpServletResponse response) {
        final UserData userData = parseUserData(body);
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return (new Gson()).toJson(ResponseStatusData.INVALID_REQUEST);
        }

        String answer;
        try {
            accountService.editAccount(
                    userData.getUsername(),
                    userData.getSessionID(),
                    userData.getEmail(),
                    userData.getPassword());
            answer = (new Gson()).toJson(ResponseStatusData.SUCCESS);
        }catch (AccessDeniedException e){
            response.setStatus(HttpStatus.FORBIDDEN_403);
            answer = (new Gson()).toJson(ResponseStatusData.ACCESS_DENIED);
        }
        return answer;
    }

    @RequestMapping(path = "/api/getaccount", method = RequestMethod.POST, produces = "application/json")
    public String getAccount(
            @RequestBody String body,
            HttpServletResponse response) {
        final UserData userData = parseUserData(body);
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return (new Gson()).toJson(ResponseStatusData.INVALID_REQUEST);
        }
        final UserData answerData = accountService.get(userData.getUsername());
        response.setStatus(answerData == null ? HttpStatus.NOT_FOUND_404 : HttpStatus.OK_200);
        return answerData != null ?
                (new Gson()).toJson(answerData):
                (new Gson()).toJson(ResponseStatusData.NOT_FOUND);
    }

}