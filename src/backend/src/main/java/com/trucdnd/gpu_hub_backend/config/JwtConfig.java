package com.trucdnd.gpu_hub_backend.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

import javax.crypto.SecretKey;

@ConfigurationProperties(prefix = "jwt")
@Configuration
@Getter @Setter
public class JwtConfig {
    private String secret;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;

    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
