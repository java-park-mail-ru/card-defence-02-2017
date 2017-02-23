package com.kvteam.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kvteam.backend.AccountService;
import com.kvteam.backend.Exceptions.UserAlreadyExistException;
import com.kvteam.backend.userdata.UserData;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;


// Логин регистрация логаут
// Упрощения: Без записи в  базу, без интерфейсов, потоками рулит Jetty, потокоопасные Map

// Сервлет обработки запроса
// Класс AccountService с методом регистрации
// Класс UserProfile  с логином и логаутом

@RestController
public class BackendController {
    private AccountService accountService;
    public BackendController(AccountService accountService) {
        this.accountService = accountService;
    }

    private UserData parseUserData(String body){
        UserData userData = null;
        try {
            try (Reader reader = new StringReader(body)) {
                Gson gson = new GsonBuilder().create();
                userData = gson.fromJson(reader, UserData.class);
            }
        }catch(IOException e){
        }
        return userData;
    }

   /* private void tryLogin(UserData userData, HttpSession session){
        UUID newSessionID = null;
        if(userData != null
                && (newSessionID = accountService.login(userData)) != null){
            session.setAttribute("username", userData.getUsername());
            session.setAttribute("sessionID", newSessionID);
        }
    }

    private void tryLogout(HttpSession session){
        Object attrUsername = session.getAttribute("username");
        Object attrSessionID = session.getAttribute("sessionID");
        if(attrUsername != null
                && attrSessionID != null){
            accountService.tryLogout(attrUsername.toString(), (UUID)attrSessionID);
        }
    }*/

    @RequestMapping(path = "/api/login", method = RequestMethod.POST, produces = "application/json")
    public String login(
            @RequestBody String body,
            HttpServletResponse response) {
        UserData userData = parseUserData(body);
        if(userData == null){
            response.setStatus(400);
            return "";
        }
        //tryLogout(session);
        accountService.tryLogout(
                userData.getUsername(),
                userData.getSessionID());

        UserData answerData = null;
        UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
        if(sessionID != null){
            response.setStatus(200);
            answerData = new UserData(
                    userData.getUsername(),
                    null,
                    null,
                    sessionID);
        }else{
            response.setStatus(401);
        }

        return answerData != null ?
                (new Gson()).toJson(answerData):
                "";
    }

    @RequestMapping(path = "/api/logout", method = RequestMethod.POST, produces = "application/json")
    public String logout(@RequestBody String body,
                         HttpServletResponse response) {
        //tryLogout(session);
        //session.invalidate();

        UserData userData = parseUserData(body);
        if(userData == null){
            response.setStatus(400);
            return "";
        }
        accountService.tryLogout(
                userData.getUsername(),
                userData.getSessionID());

        return "";
    }

    @RequestMapping(path = "/api/isloggedin", method = RequestMethod.POST, produces = "application/json")
    public String isLoggedIn(@RequestBody String body,
                             HttpServletResponse response) {
        //Object attrUsername = session.getAttribute("username");
        //Object attrSessionID = session.getAttribute("sessionID");
        UserData userData = parseUserData(body);
        if(userData == null){
            response.setStatus(400);
            return "";
        }

        UserData answerData = null;
        if(!userData.getUsername().equals("")
               && userData.getSessionID() != null) {
            boolean isLoggedIn =
                    accountService.isLoggedIn(userData.getUsername(), userData.getSessionID());

            if(isLoggedIn){
                response.setStatus(200);
                answerData = accountService.get(userData.getUsername());
            }else{
                response.setStatus(401);
            }
        }else{
            response.setStatus(400);
        }
        return answerData != null ?
                (new Gson()).toJson(answerData):
                "";
    }

    @RequestMapping(path = "/api/register", method = RequestMethod.POST, produces = "application/json")
    public String register(
            @RequestBody String body,
            HttpServletResponse response) {
        UserData userData = parseUserData(body);
        if(userData == null){
            response.setStatus(400);
            return "";
        }
        //tryLogout(session);
        accountService.tryLogout(
                userData.getUsername(),
                userData.getSessionID());

        UserData answerData = null;
        try {
            accountService.add(userData);
            UUID sessionID = accountService.login(userData.getUsername(), userData.getPassword());
            if(sessionID != null){
                response.setStatus(200);
                answerData = new UserData(
                        userData.getUsername(),
                        null,
                        null,
                        sessionID);
            }else{
                response.setStatus(401);
            }
        }catch (UserAlreadyExistException e){
            response.setStatus(409);
        }
        return answerData != null ?
                (new Gson()).toJson(answerData):
                "";
    }


}