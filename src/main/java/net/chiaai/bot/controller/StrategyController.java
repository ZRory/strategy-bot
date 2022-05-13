package net.chiaai.bot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.enums.SymbolStatusEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.web.BizResponse;
import net.chiaai.bot.conf.env.BinanceCatchs;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.request.ChangeStrategyStatusRequest;
import net.chiaai.bot.entity.request.ClosePositionRequest;
import net.chiaai.bot.entity.request.CreateStrategyRequest;
import net.chiaai.bot.entity.request.UpdateStrategyRequest;
import net.chiaai.bot.entity.response.ProfitVo;
import net.chiaai.bot.feign.response.AccountInfoResponse;
import net.chiaai.bot.feign.response.BalanceResponse;
import net.chiaai.bot.service.StrategyConfigService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.stream.Collectors;

@Slf4j
@RestController
//@Api(tags = "策略接口")
@RequestMapping(path = "/strategy")
public class StrategyController {

    @Resource
    private StrategyConfigService strategyConfigService;

    //@ApiOperation("创建策略")
    @PostMapping(path = "/create")
    public BizResponse<?> createStrategyConfig(@RequestBody CreateStrategyRequest request) {
        if (request.getAutoSwitch() == null && request.getPositionSide() == null) {
            throw new BizException(BizCodeEnum.PARAM_IS_NULL, "自动判断方向与订单方向不能同时为空");
        }
        strategyConfigService.createStrategyConfig(request);
        return BizResponse.success();
    }

    //@ApiOperation("更新策略")
    @PostMapping(path = "/update")
    public BizResponse<?> updateStrategyConfig(@RequestBody UpdateStrategyRequest request) {
        strategyConfigService.updateStrategyConfig(request);
        return BizResponse.success();
    }

    //@ApiOperation("修改策略状态")
    @PostMapping(path = "/status")
    public BizResponse<?> createStrategyConfig(@RequestBody ChangeStrategyStatusRequest request) {
        strategyConfigService.updateStatus(request);
        return BizResponse.success();
    }

    //@ApiOperation("查询策略列表")
    @GetMapping(path = "/list")
    public BizResponse<Page<StrategyConfig>> list(@RequestParam Integer pageNum,
                                                  @RequestParam Integer pageSize,
                                                  @RequestParam(required = false) String symbol,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false, defaultValue = "false") Boolean share,
                                                  @RequestParam(required = false) String column, @RequestParam(required = false) String sort) {
        return BizResponse.success(strategyConfigService.list(pageNum, pageSize, symbol, status, share, column, sort));
    }

    //@ApiOperation("查询策略盈亏")
    @GetMapping(path = "/profit")
    public BizResponse<ProfitVo> profit(@RequestParam Long strategyId) {
        return BizResponse.success(strategyConfigService.countProfit(strategyId));
    }

    //@ApiOperation("查询策略执行仓位")
    @GetMapping(path = "/positions")
    public BizResponse<Page<Position>> positions(@RequestParam Long strategyId, @RequestParam Integer pageNum, @RequestParam Integer pageSize, @RequestParam(required = false) String column, @RequestParam(required = false) String sort) {
        return BizResponse.success(strategyConfigService.positions(strategyId, pageNum, pageSize, column, sort));
    }

    @GetMapping(path = "/balance")
    public BizResponse<BalanceResponse> balance() {
        return BizResponse.success(strategyConfigService.balance());
    }

    @PostMapping(path = "/closePosition")
    public BizResponse<?> closePosition(@RequestBody ClosePositionRequest request) {
        strategyConfigService.closePosition(request.getPositionId());
        return BizResponse.success();
    }

    @GetMapping(path = "/account")
    public BizResponse<AccountInfoResponse> account() {
        return BizResponse.success(strategyConfigService.account());
    }

    @GetMapping(path = "/symbols")
    public BizResponse<?> symbols() {
        return BizResponse.success(BinanceCatchs.symbols.values().stream().filter(x -> x.getStatus() != SymbolStatusEnum.CLOSE).map(x -> x.getSymbol()).collect(Collectors.toList()));
    }

}
