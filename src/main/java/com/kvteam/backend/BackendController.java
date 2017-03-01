package com.kvteam.backend;

import com.kvteam.backend.dataformats.ResponseStatusData;
import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.dataformats.UserData;
import com.sun.istack.internal.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
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

    @RequestMapping(path = "/api/login", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Object> login(
            @RequestBody UserData userData,
            HttpServletResponse response) {
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())
                || isStringNullOrEmpty(userData.getPassword())){
            response.setStatus(HttpStatus.BAD_REQUEST.value());

            return new ResponseEntity<>(
                    ResponseStatusData.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST);
        }
        if(userData.getSessionID() != null) {
            accountService.tryLogout(
                    userData.getUsername(),
                    userData.getSessionID());
        }

        UserData answerData = null;
        final UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
        if(sessionID != null){
            response.setStatus(HttpStatus.OK.value());
            answerData = new UserData(
                    userData.getUsername(),
                    null,
                    null,
                    sessionID);
        }else{
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }

        return answerData != null ?
                new ResponseEntity<>(answerData, HttpStatus.OK):
                new ResponseEntity<>(
                        ResponseStatusData.ACCESS_DENIED,
                        HttpStatus.FORBIDDEN);
    }

    @RequestMapping(path = "/api/logout", method = RequestMethod.POST, produces = "application/json")
    public ResponseStatusData logout(@RequestBody UserData userData,
                         HttpServletResponse response) {
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseStatusData.INVALID_REQUEST;
        }
        accountService.tryLogout(
                userData.getUsername(),
                userData.getSessionID());
        return ResponseStatusData.SUCCESS;
    }

    @RequestMapping(path = "/api/isloggedin", method = RequestMethod.POST, produces = "application/json")
    public ResponseStatusData isLoggedIn(@RequestBody UserData userData,
                             HttpServletResponse response) {
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseStatusData.INVALID_REQUEST;
        }

        final boolean isLoggedIn =
                accountService.isLoggedIn(userData.getUsername(), userData.getSessionID());
        final ResponseStatusData answer;
        if(isLoggedIn){
            response.setStatus(HttpStatus.OK.value());
            answer = ResponseStatusData.SUCCESS;
        }else{
            response.setStatus(HttpStatus.FORBIDDEN.value());
            answer = ResponseStatusData.ACCESS_DENIED;
        }

        return answer;
    }

    @RequestMapping(path = "/api/account", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Object> register(
            @RequestBody UserData userData,
            HttpServletResponse response) {
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())
                || isStringNullOrEmpty(userData.getPassword())){
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(
                    ResponseStatusData.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST);
        }
        if(userData.getSessionID() != null) {
            accountService.tryLogout(
                    userData.getUsername(),
                    userData.getSessionID());
        }

        UserData answerData = null;
        ResponseEntity<Object> errorAnswer = null ;

        // true - если добавлен, false - если уже такой есть
        if(accountService.add(userData)){
            final UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
            if(sessionID != null){
                response.setStatus(HttpStatus.OK.value());
                answerData = new UserData(
                        userData.getUsername(),
                        null,
                        null,
                        sessionID);
            }else{
                response.setStatus(HttpStatus.FORBIDDEN.value());
                errorAnswer = new ResponseEntity<>(
                                ResponseStatusData.ACCESS_DENIED,
                                HttpStatus.FORBIDDEN);
            }
        }else{
            response.setStatus(HttpStatus.CONFLICT.value());
            errorAnswer = new ResponseEntity<>(
                    ResponseStatusData.CONFLICT,
                    HttpStatus.CONFLICT);
        }
        return answerData != null ?
                new ResponseEntity<>(answerData, HttpStatus.OK):
                errorAnswer;
    }


    @RequestMapping(path = "/api/account", method = RequestMethod.PUT, produces = "application/json")
    public ResponseStatusData editAccount(
            @RequestBody UserData userData,
            HttpServletResponse response) {
        if(userData == null
                || isStringNullOrEmpty(userData.getUsername())){
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseStatusData.INVALID_REQUEST;
        }

        ResponseStatusData answer;
        try {
            accountService.editAccount(
                    userData.getUsername(),
                    userData.getSessionID(),
                    userData.getEmail(),
                    userData.getPassword());
            answer = ResponseStatusData.SUCCESS;
        }catch (AccessDeniedException e){
            response.setStatus(HttpStatus.FORBIDDEN.value());
            answer = ResponseStatusData.ACCESS_DENIED;
        }
        return answer;
    }

    @RequestMapping(path = "/api/account", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Object> getAccount(
            @RequestParam("username") String username,
            HttpServletResponse response) {
        if(isStringNullOrEmpty(username)){
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(
                    ResponseStatusData.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST);
        }
        final UserData answerData = accountService.get(username);
        response.setStatus(answerData == null ? HttpStatus.NOT_FOUND.value() : HttpStatus.OK.value());
        return answerData != null ?
                new ResponseEntity<>(answerData, HttpStatus.OK):
                new ResponseEntity<>(
                        ResponseStatusData.NOT_FOUND,
                        HttpStatus.NOT_FOUND);
    }

}