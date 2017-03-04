package com.kvteam.backend;

import com.kvteam.backend.dataformats.ResponseStatusData;
import com.kvteam.backend.exceptions.*;
import com.kvteam.backend.dataformats.UserData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@CrossOrigin
@RestController
public class BackendController {
    private AccountService accountService;

    public BackendController(AccountService accountService) {
        this.accountService = accountService;
    }

    private boolean isStringNullOrEmpty(@Nullable String str) {
        return str == null || str.isEmpty();
    }

    private void tryLogout(@NotNull HttpSession session){
        final UUID oldSessionID = (UUID)session.getAttribute("sessionID");
        final String oldUsername = (String)session.getAttribute("username");
        if (oldSessionID != null
                && isStringNullOrEmpty(oldUsername)) {
            accountService.tryLogout(
                    oldUsername,
                    oldSessionID);
            session.removeAttribute("sessionID");
            session.removeAttribute("username");
        }
    }

    @RequestMapping(path = "/api/login", method = RequestMethod.POST, produces = "application/json")
    public ResponseStatusData login(
            @RequestBody UserData userData,
            HttpServletResponse response,
            HttpSession session) {
        if (userData == null
                || isStringNullOrEmpty(userData.getUsername())
                || isStringNullOrEmpty(userData.getPassword())) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseStatusData.INVALID_REQUEST;
        }
        tryLogout(session);

        final ResponseStatusData answerData;
        final UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
        if (sessionID != null) {
            response.setStatus(HttpStatus.OK.value());
            session.setAttribute("sessionID", sessionID);
            session.setAttribute("username", userData.getUsername());
            answerData = ResponseStatusData.SUCCESS;
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            answerData = ResponseStatusData.ACCESS_DENIED;
        }

        return answerData;
    }

    @RequestMapping(path = "/api/logout", method = RequestMethod.GET, produces = "application/json")
    public ResponseStatusData logout(HttpSession session) {
        tryLogout(session);
        session.invalidate();
        return ResponseStatusData.SUCCESS;
    }

    @RequestMapping(path = "/api/isloggedin", method = RequestMethod.GET, produces = "application/json")
    public ResponseStatusData isLoggedIn(HttpSession session, HttpServletResponse response) {
        final UUID sessionID = (UUID)session.getAttribute("sessionID");
        final String username = (String)session.getAttribute("username");
        if (sessionID != null
                && !isStringNullOrEmpty(username)
                && accountService.isLoggedIn(username, sessionID)){
            response.setStatus(HttpStatus.OK.value());
            return ResponseStatusData.SUCCESS;
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return ResponseStatusData.ACCESS_DENIED;
        }
    }

    @RequestMapping(path = "/api/account", method = RequestMethod.POST, produces = "application/json")
    public ResponseStatusData register(
            @RequestBody UserData userData,
            HttpServletResponse response,
            HttpSession session) {
        if (userData == null
                || isStringNullOrEmpty(userData.getUsername())
                || isStringNullOrEmpty(userData.getPassword())) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseStatusData.INVALID_REQUEST;
        }
        tryLogout(session);

        final ResponseStatusData answer;

        // true - если добавлен, false - если уже такой есть
        if (accountService.add(userData)) {
            final UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
            if (sessionID != null) {
                response.setStatus(HttpStatus.OK.value());
                answer = ResponseStatusData.SUCCESS;
                session.setAttribute("username", userData.getUsername());
                session.setAttribute("sessionID", sessionID);
            } else {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                answer = ResponseStatusData.ACCESS_DENIED;
            }
        } else {
            response.setStatus(HttpStatus.CONFLICT.value());
            answer = ResponseStatusData.CONFLICT;
        }
        return answer;
    }


    @RequestMapping(path = "/api/account", method = RequestMethod.PUT, produces = "application/json")
    public ResponseStatusData editAccount(
            @RequestBody UserData userData,
            HttpServletResponse response,
            HttpSession session) {
        if (userData == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return ResponseStatusData.INVALID_REQUEST;
        }

        final UUID sessionID = (UUID)session.getAttribute("sessionID");
        final String username = (String)session.getAttribute("username");
        ResponseStatusData answer;
        if (sessionID != null
                && !isStringNullOrEmpty(username)){
            try {
                accountService.editAccount(
                        username,
                        sessionID,
                        userData.getEmail(),
                        userData.getPassword());
                answer = ResponseStatusData.SUCCESS;
            } catch (AccessDeniedException e) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                answer = ResponseStatusData.ACCESS_DENIED;
            }
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            answer = ResponseStatusData.ACCESS_DENIED;
        }

        return answer;
    }

    @RequestMapping(path = "/api/account/{username}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Object> getAccount(
            @PathVariable("username") String username,
            HttpServletResponse response) {
        if (isStringNullOrEmpty(username)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return new ResponseEntity<>(
                    ResponseStatusData.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST);
        }
        final UserData answerData = accountService.get(username);
        response.setStatus(answerData == null ? HttpStatus.NOT_FOUND.value() : HttpStatus.OK.value());
        return answerData != null ?
                new ResponseEntity<>(answerData, HttpStatus.OK) :
                new ResponseEntity<>(
                        ResponseStatusData.NOT_FOUND,
                        HttpStatus.NOT_FOUND);
    }

}