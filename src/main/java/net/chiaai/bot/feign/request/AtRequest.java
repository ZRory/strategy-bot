package net.chiaai.bot.feign.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AtRequest {

    private List<String> atMobiles;

    private List<String> atUserIds;

    private List<String> isAtAll;

}
