package com.kvteam.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvteam.backend.gameplay.Card;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Created by maxim on 19.03.17.
 */
@SuppressWarnings({"SpringAutowiredFieldsWarningInspection", "SpringFacetCodeInspection"})
@Configuration
@EnableWebSocket
@EnableAutoConfiguration
@ComponentScan
public class AppConfiguration implements WebSocketConfigurer {
    @Value("${origin}")
    private String origin;
    @Autowired
    private GameWebSocketHandler webSocketHandler;
    @Autowired
    private AccountService accountService;

    @Bean
    public FilterRegistrationBean corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin(origin);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        final FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }


    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public CheckCredentialsWebsocketInterceptor credentialsWebsocketInterceptor() {
        return new CheckCredentialsWebsocketInterceptor(accountService);
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CardManager cardManager(){
        final CardManager manager;
        try {
            final ClassPathResource resource
                    = new ClassPathResource("cards.json");
            final Card[] cards = objectMapper()
                    .readValue(resource.getInputStream(), Card[].class);
            manager = new CardManager(cards);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
            e.printStackTrace();
            return new CardManager(null);
        }
        return manager;
    }

    @Bean
    public GameplaySettings gameplaySettings(){
        final GameplaySettings settings;
        try {
            final ClassPathResource resource
                    = new ClassPathResource("game_settings.json");
            settings = objectMapper()
                    .readValue(resource.getInputStream(), GameplaySettings.class);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
            e.printStackTrace();
            return new GameplaySettings(1, 1);
        }
        return settings;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(webSocketHandler, "/connect")
                .addInterceptors(credentialsWebsocketInterceptor())
                .setAllowedOrigins(origin);
    }

}
