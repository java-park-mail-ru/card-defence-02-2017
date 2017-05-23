package com.kvteam.backend.services;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Created by maxim on 24.04.17.
 */
@SuppressWarnings("ALL")
public class GameTestWebSocketSession implements WebSocketSession {
    private String lastReceivedString;
    private boolean forceClose = false;
    public String getLastReceivedString(){
        return lastReceivedString;
    }


    @Override
    public String getId() {
        return null;
    }

    @Override
    public URI getUri() {
        return null;
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public String getAcceptedProtocol() {
        return null;
    }

    @Override
    public void setTextMessageSizeLimit(int i) {

    }

    @Override
    public int getTextMessageSizeLimit() {
        return 0;
    }

    @Override
    public void setBinaryMessageSizeLimit(int i) {

    }

    @Override
    public int getBinaryMessageSizeLimit() {
        return 0;
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
        return null;
    }

    @Override
    public void sendMessage(WebSocketMessage<?> webSocketMessage) throws IOException {
        lastReceivedString = webSocketMessage.getPayload().toString();
    }

    @Override
    public boolean isOpen() {
        return !forceClose;
    }

    @Override
    public void close() throws IOException {
        forceClose = true;
    }

    @Override
    public void close(CloseStatus closeStatus) throws IOException {

    }
}
