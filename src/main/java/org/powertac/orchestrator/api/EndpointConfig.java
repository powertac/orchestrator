package org.powertac.orchestrator.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class EndpointConfig  implements WebSocketMessageBrokerConfigurer {

    @Value("#{'${security.api.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins(getAllowedOrigins())
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/baselines", "/brokers", "/games");
        registry.setApplicationDestinationPrefixes("/requests");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        // FIXME - this might be in conflict with SecurityConfig
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedMethods("GET", "POST", "DELETE")
                    .allowedOrigins(getAllowedOrigins());
            }
        };
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins.toArray(String[]::new);
    }

}
