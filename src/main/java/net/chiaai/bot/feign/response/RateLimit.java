package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateLimit {

    private String rateLimitType;
    private String interval;
    private Long intervalNum;
    private Long limit;

}
