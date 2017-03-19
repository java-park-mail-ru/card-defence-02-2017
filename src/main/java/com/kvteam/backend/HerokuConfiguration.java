package com.kvteam.backend;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by maxim on 19.03.17.
 */
@Profile("heroku")
@ComponentScan
@Configuration
@EnableAutoConfiguration
public class HerokuConfiguration {
}
