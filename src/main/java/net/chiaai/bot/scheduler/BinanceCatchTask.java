package net.chiaai.bot.scheduler;

import net.chiaai.bot.conf.env.BinanceCatchs;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.response.ExchangeInfoResp;
import net.chiaai.bot.feign.response.Symbol;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 币安缓存任务
 */
@Component
public class BinanceCatchTask {

    @Resource
    private BinanceFeignClient binanceClient;

    @PostConstruct
    @Scheduled(fixedRate = 600_000)
    public void loadSymbols() {
        ExchangeInfoResp exchangeInfoResp = binanceClient.exchangeInfo();
        BinanceCatchs.symbols = exchangeInfoResp.getSymbols().stream().collect(Collectors.toMap(Symbol::getSymbol, Function.identity(), (key1, key2) -> key2));
    }

}
