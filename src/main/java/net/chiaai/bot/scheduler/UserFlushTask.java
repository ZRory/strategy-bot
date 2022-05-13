package net.chiaai.bot.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.utils.BinanceUtil;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.ServiceAmountRateRequest;
import net.chiaai.bot.feign.response.*;
import net.chiaai.bot.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 币安缓存任务
 */
@Slf4j
@Component
public class UserFlushTask {

    @Resource
    private BinanceFeignClient binanceClient;

    @Resource
    private UserMapper userMapper;

    //@PostConstruct
    @Scheduled(cron = "0 0 0 * * ?") //每天凌晨执行一次
    public void reloadServiceAmountRate() {
        List<User> users = userMapper.selectList(new QueryWrapper<>());
        for (User user : users) {
            try {
                if (StringUtils.isBlank(user.getApiKey())) {
                    continue;
                }
                //开始查询用户费率
                ServiceAmountRateResponse serviceAmountRateResponse = binanceClient.serviceAmountRate(user.getApiKey(), BinanceUtil.encodeParams(new ServiceAmountRateRequest(), user));
                user.setServiceAmountRate(serviceAmountRateResponse.getTakerCommissionRate());
                userMapper.updateById(user);
            } catch (Exception e) {
                log.error("更新用户服务费率异常", e);
            }
        }
    }

}
