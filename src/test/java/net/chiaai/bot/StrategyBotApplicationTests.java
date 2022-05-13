package net.chiaai.bot;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.service.CandleLineService;
import net.chiaai.bot.service.DingTalkService;
import net.chiaai.bot.service.StrategyConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StrategyBotApplicationTests {

    @Autowired
    private BinanceFeignClient binanceClient;

    @Autowired
    private StrategyConfigService strategyConfigService;

    @Autowired
    private DingTalkService dingTalkService;

    @Autowired
    private CandleLineService candleLineService;

    @Test
    void contextLoads() throws Exception {
        Thread.sleep(1000L);
    }

}
