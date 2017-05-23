package com.kvteam.backend.websockets;

import com.kvteam.backend.services.AccountService;
import org.apache.http.NameValuePair;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.servlet.http.HttpSession;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by maxim on 20.03.17.
 */
public class CheckCredentialsWebsocketInterceptor implements HandshakeInterceptor {
    static final List<String> ALLOWED_SIDES
            = Arrays.asList("attack", "defence", "all");

    static final String USERNAME_PARAM = "username";
    static final String TYPE_PARAM = "type";
    static final String SIDE_PARAM = "side";
    static final String SINGLEPLAYER = "singleplayer";
    static final String MULTIPLAYER = "multiplayer";
    private static final int MAX_QUERY_LENGTH = 30;


    private AccountService accountService;

    public CheckCredentialsWebsocketInterceptor(AccountService accountService){
        this.accountService = accountService;
    }

    /**
     * Перехват запроса на открытие соедниение
     * Проверка сессии пользователя, типа желаемой игры(сингл, мульти)
     * Запись в вебсокет сессию параметров для дальшейшей работы хендлера
     */
    @Override
    // Сложность незначительна из-за маленьких обрабатываемых коллекций
    @SuppressWarnings("OverlyComplexMethod")
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        boolean allowConnect = false;
        // Допускаем только авторизованных пользователей
        if (request instanceof ServletServerHttpRequest) {
            final HttpSession session ;
            final UUID sessionID ;
            String username = "";
            // Получение из кода ID сессии и username и сравнение с базой
            final boolean access = (session = ((ServletServerHttpRequest)request).getServletRequest().getSession(false)) != null
                    && (sessionID = (UUID)session.getAttribute("sessionID")) != null
                    && (username = (String)session.getAttribute("username")) != null
                    && accountService.isLoggedIn(username, sessionID);
            // Если пользователь залогинен и не прислал слишком большой запрос
            // (который нет смысла обрабатывать
            //  т.к. ожидается конечное множество параметров)
            if (access
                    && request.getURI().getQuery().length() < MAX_QUERY_LENGTH) {
                try {
                    // Парсинг гет параметров
                    final List<NameValuePair> params
                            = URLEncodedUtils.parse(request.getURI(), "UTF-8");
                    final Map<String, String> queryParams =
                            params
                            .stream()
                            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                    attributes.put(USERNAME_PARAM, username);
                    // В аттрибуты вебсокет сессии добавляем информацию о намерениях
                    // пользователя.
                    // Дальнейшая обработка будет произведена в GameWebSocketHandler
                    if(queryParams.size() == 1
                            && queryParams.get(TYPE_PARAM).equals(SINGLEPLAYER)){
                        attributes.put(TYPE_PARAM, SINGLEPLAYER);
                        allowConnect = true;
                    } else if(queryParams.size() == 2
                            && queryParams.get(TYPE_PARAM).equals(MULTIPLAYER)
                            && ALLOWED_SIDES.contains(queryParams.get(SIDE_PARAM))){
                        attributes.put(TYPE_PARAM, MULTIPLAYER);
                        attributes.put(SIDE_PARAM, queryParams.get(SIDE_PARAM));
                        allowConnect = true;
                    }
                } catch (IndexOutOfBoundsException ignored){
                    // Может возникнуть при разборе query параметров
                }
            }
        }
        return allowConnect;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception ex) {
    }
}
