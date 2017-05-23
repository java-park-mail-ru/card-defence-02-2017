package com.kvteam.backend;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.gameplay.CardManager;
import com.kvteam.backend.gameplay.GameplaySettings;
import com.kvteam.backend.services.AccountService;
import com.kvteam.backend.websockets.CheckCredentialsWebsocketInterceptor;
import com.kvteam.backend.websockets.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.sql.DataSource;

/**
 * Created by maxim on 18.04.17.
 */
@Configuration
@EnableWebSocket
@EnableAutoConfiguration
@ComponentScan
public class WebSocketConfiguration implements WebSocketConfigurer {
    @Value("${origin}")
    private String origin;
    @Autowired
    private GameWebSocketHandler webSocketHandler;
    @Autowired
    private AccountService accountService;


    @Bean
    public CheckCredentialsWebsocketInterceptor credentialsWebsocketInterceptor() {
        return new CheckCredentialsWebsocketInterceptor(accountService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(webSocketHandler, "/connect")
                .addInterceptors(credentialsWebsocketInterceptor())
                .setAllowedOrigins(origin);
    }

}
