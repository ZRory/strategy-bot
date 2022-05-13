package net.chiaai.bot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableAsync
@EnableScheduling
@EnableFeignClients
@SpringBootApplication
@MapperScan("net.chiaai.bot.mapper")
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400)
public class StrategyBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrategyBotApplication.class, args);
    }

}