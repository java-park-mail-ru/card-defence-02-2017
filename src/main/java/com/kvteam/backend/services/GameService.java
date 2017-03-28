package com.kvteam.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.dataformats.*;
import com.kvteam.backend.websockets.IPlayerConnection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by maxim on 26.03.17.
 */
@Service
public class GameService {

    private void sendStartData(
            UUID gameID,
            IPlayerConnection attack,
            IPlayerConnection defence)
                throws IOException {
        // Это очень маловероятно, до этого они должны быть установлены
        if(defence.getUsername() == null
                || attack.getUsername() == null){
            throw new NullPointerException();
        }

        final CardData[] forAttack = {new CardData("a"), new CardData("b")};
        final CardData[] forDefence = {new CardData("c"), new CardData("d")};
        final GameStartData startDataAttack = new GameStartData(
                gameID,
                defence.getUsername(),
                1,
                2,
                forAttack);

        final GameStartData startDataDefence = new GameStartData(
                gameID,
                attack.getUsername(),
                1,
                2,
                forDefence);
        final ObjectMapper mapper = new ObjectMapper();
        attack.send(mapper.writeValueAsString(startDataAttack));
        defence.send(mapper.writeValueAsString(startDataDefence));
    }

    private void receive(IPlayerConnection me, IPlayerConnection other, String message){
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final GameClientData baseData = mapper.readValue(message, GameClientData.class);
            if(baseData.getStatus().equals(GameClientData.SEND_CHAT_MESSAGE)
                    && me.getGameID() != null
                    && me.getUsername() != null){
                final MessageClientData data = mapper.readValue(message, MessageClientData.class);

                final MessageServerData serverData = new MessageServerData(
                        me.getGameID(),
                        me.getUsername(),
                        data.getMessage()
                );
                other.send(mapper.writeValueAsString(serverData));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close(IPlayerConnection me, IPlayerConnection other){
        try {
            other.close();
        } catch(IOException ignored) {

        }
    }

    public void startGame(IPlayerConnection attacker, IPlayerConnection defender){
        try {
            // При получении информации с клиента будет вызываться receive,
            // Причем, за счет замыканий, в методе будут доступны оба игрока
            attacker.onReceive((conn, str) -> receive(conn, defender, str));
            defender.onReceive((conn, str) -> receive(conn, attacker, str));
            attacker.onClose((conn, status) -> close(conn, defender));
            defender.onClose((conn, status) -> close(conn, attacker));

            final UUID gameID = UUID.randomUUID();
            attacker.setGameID(gameID);
            defender.setGameID(gameID);
            sendStartData(gameID, attacker, defender);
        } catch (IOException e) {
            attacker.onReceive(null);
            attacker.onClose(null);
            defender.onReceive(null);
            defender.onReceive(null);
            try {
                attacker.close();
                defender.close();
            } catch (IOException ignored) {

            }
        }
    }

}
