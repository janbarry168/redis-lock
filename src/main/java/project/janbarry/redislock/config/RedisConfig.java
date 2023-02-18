package project.janbarry.redislock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("spring.redis")
public class RedisConfig {

    private int database;

    private String host;

    private String port;

}
