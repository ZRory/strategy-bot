package net.chiaai.bot.feign.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DingMsgContent {

    private String content;

    public DingMsgContent(String content) {
        this.content = content;
    }

}
