package net.chiaai.bot.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.utils.BinanceUtil;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.feign.request.ServiceAmountRateRequest;
import net.chiaai.bot.feign.response.ServiceAmountRateResponse;
import net.chiaai.bot.mapper.UserMapper;
import net.chiaai.bot.service.CandleLineService;
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
public class UpdateCandleLineTask {

    @Resource
    private CandleLineService candleLineService;

    //@PostConstruct
    @Scheduled(cron = "0 5 0 * * ?") //每天凌晨00:05执行一次
    public void reloadServiceAmountRate() {
        List<String> symbols = candleLineService.listSymbols();
        symbols.forEach(x -> {
            candleLineService.update(x);
        });
    }

}
