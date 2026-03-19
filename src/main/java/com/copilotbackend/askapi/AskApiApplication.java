package com.copilotbackend.askapi;

import com.copilotbackend.askapi.config.SearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SearchProperties.class)
public class AskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AskApiApplication.class, args);
    }
}
