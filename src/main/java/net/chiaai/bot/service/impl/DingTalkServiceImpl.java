package net.chiaai.bot.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.utils.RedisUtils;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.feign.DingTalkFeignClient;
import net.chiaai.bot.feign.request.DingMsgRequest;
import net.chiaai.bot.service.DingTalkService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DingTalkServiceImpl implements DingTalkService {

    @Resource
    private DingTalkFeignClient dingTalkFeignClient;

    @Resource
    private RedisUtils redisUtils;

    @Async
    @Override
    public void sendMessage(User user, String message) {
        if (StringUtils.isNotBlank(user.getAccessToken())) {
            String key = "DingTalk:" + user.getAccessToken();
            if (redisUtils.hasKey(key)) {
                Long incr = redisUtils.incr(key);
                if (incr > 20) {
                    //发送超过20条 不继续发送
                    log.info("发送消息过于频繁，不继续发送消息：user：{}", JSON.toJSONString(user));
                    return;
                }
            } else {
                redisUtils.set(key, "1", 60, TimeUnit.SECONDS);
            }
            dingTalkFeignClient.sendMessage(user.getAccessToken(), new DingMsgRequest(user.getPhone(), message));
        }
    }

}
