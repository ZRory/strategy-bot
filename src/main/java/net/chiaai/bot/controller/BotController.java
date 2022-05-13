package net.chiaai.bot.controller;

import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.web.BizResponse;
import net.chiaai.bot.scheduler.TradeTask;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
//@Api(tags = "策略接口")
@RequestMapping(path = "/bot")
public class BotController {

    @GetMapping(path = "/stop")
    public BizResponse<?> stop() {
        TradeTask.enable = false;
        return BizResponse.success();
    }

    @GetMapping(path = "/start")
    public BizResponse<?> start() {
        TradeTask.enable = true;
        return BizResponse.success();
    }

}
