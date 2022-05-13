package net.chiaai.bot.controller;

import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.web.BizResponse;
import net.chiaai.bot.entity.dao.BackTest;
import net.chiaai.bot.service.BackTestService;
import net.chiaai.bot.service.CandleLineService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping(path = "/backTest")
public class BackTestController {

    @Resource
    private BackTestService backTestService;

    @Resource
    private CandleLineService candleLineService;

    @GetMapping(path = "/updateSymbol")
    public BizResponse<?> addSymbol(@RequestParam String symbol) {
        candleLineService.update(symbol);
        return BizResponse.success();
    }

    //@ApiOperation("创建策略")
    @PostMapping(path = "/create")
    public BizResponse<?> createStrategyConfig(@RequestBody BackTest request) {
        BackTest backTest = backTestService.createBackTest(request);
        backTestService.execBackTest(backTest);
        return BizResponse.success();
    }

    @GetMapping(path = "/list")
    public BizResponse<?> list(@RequestParam Integer pageNum,
                               @RequestParam Integer pageSize,
                               @RequestParam(required = false) String symbol,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String column, @RequestParam(required = false) String sort) {
        return BizResponse.success(backTestService.list(pageNum, pageSize, symbol, status, column, sort));
    }

    @GetMapping(path = "/detail")
    public BizResponse<?> detail(@RequestParam Integer pageNum,
                                 @RequestParam Integer pageSize,
                                 @RequestParam Long backTestId,
                                 @RequestParam(required = false) String column, @RequestParam(required = false) String sort) {
        return BizResponse.success(backTestService.detail(pageNum, pageSize, backTestId, column, sort));
    }

    @GetMapping(path = "/symbols")
    public BizResponse<?> symbols() {
        return BizResponse.success(candleLineService.listSymbols());
    }

}
