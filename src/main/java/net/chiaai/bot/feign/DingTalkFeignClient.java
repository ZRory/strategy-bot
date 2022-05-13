package net.chiaai.bot.feign;

import net.chiaai.bot.feign.request.*;
import net.chiaai.bot.feign.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ding-talk-service", url = "${ding-talk.url}")
public interface DingTalkFeignClient {

    /**
     * 发送通知消息
     */
    @PostMapping(value = "/robot/send", consumes = MediaType.APPLICATION_JSON_VALUE)
    void sendMessage(@RequestParam(value = "access_token") String access_token, DingMsgRequest request);

}
