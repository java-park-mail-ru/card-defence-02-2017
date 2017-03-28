package com.kvteam.backend.websockets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Created by maxim on 26.03.17.
 */
public class PlayerConnection
        implements IPlayerConnection{
    private UUID gameID;
    private String username;
    private WebSocketSession session;


    private BiConsumer<IPlayerConnection, String> onReceiveEvent;
    private BiConsumer<IPlayerConnection, CloseStatus> onCloseEvent;

    public PlayerConnection(
            @NotNull String username,
            @NotNull WebSocketSession session
            ){
        this.username = username;
        this.session = session;
    }

    @Override
    @Nullable
    public UUID getGameID(){
        return gameID;
    }
    @Override
    public void setGameID(@Nullable UUID gameID){
        this.gameID = gameID;
    }

    @Override
    public String getUsername(){
        return username;
    }

    @Override
    public void send(@NotNull String payload) throws IOException {
        session.sendMessage( new TextMessage(payload) );
    }

    @Override
    public void onReceive(@Nullable BiConsumer<IPlayerConnection, String> event){
        onReceiveEvent = event;
    }

    @Override
    public void onClose(@Nullable BiConsumer<IPlayerConnection, CloseStatus> event){
        onCloseEvent = event;
    }

    @Override
    public void notifyOnReceive(@NotNull String payload){
        if(onReceiveEvent != null) {
            onReceiveEvent.accept(this, payload);
        }
    }

    @Override
    public void notifyOnClose(@NotNull CloseStatus status){
        if(onCloseEvent != null) {
            onCloseEvent.accept(this, status);
        }
    }

    @Override
    public void close() throws IOException{
        if(session.isOpen()) {
            session.close();
        }
    }
}
