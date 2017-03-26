package com.kvteam.backend.websockets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Created by maxim on 26.03.17.
 */
public interface IPlayerConnection {
    String getUsername();
    void send(@NotNull String payload) throws IOException;
    void onReceive(@Nullable BiConsumer<IPlayerConnection, String> event);
    void onClose(@Nullable BiConsumer<IPlayerConnection, CloseStatus> event);
    void notifyOnReceive(@NotNull String payload);
    void notifyOnClose(@NotNull CloseStatus status);
    void forceClose() throws IOException;
}
