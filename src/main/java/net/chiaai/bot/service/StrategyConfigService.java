package net.chiaai.bot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.chiaai.bot.common.enums.PositionSideEnum;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.dto.BollBand;
import net.chiaai.bot.entity.request.ChangeStrategyStatusRequest;
import net.chiaai.bot.entity.request.CreateStrategyRequest;
import net.chiaai.bot.entity.request.UpdateStrategyRequest;
import net.chiaai.bot.entity.response.MaxFeeVo;
import net.chiaai.bot.entity.response.ProfitVo;
import net.chiaai.bot.feign.response.AccountInfoResponse;
import net.chiaai.bot.feign.response.BalanceResponse;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface StrategyConfigService {

    StrategyConfig getStrategyConfigWithLock(Long id);

    void createStrategyConfig(CreateStrategyRequest request);

    ProfitVo countProfit(Long strategyConfigId);

    List<MaxFeeVo> maxFees();

    void updateStatus(ChangeStrategyStatusRequest request);

    void updateStrategyConfig(UpdateStrategyRequest request);

    Page<StrategyConfig> list(Integer pageNum, Integer pageSize, String symbol, String status, Boolean share, String column, String sort);

    Page<Position> positions(Long strategyId, Integer pageNum, Integer pageSize, String column, String sort);

    BalanceResponse balance();

    void closePosition(Long positionId);

    AccountInfoResponse account();

    PositionSideEnum calPositionSide(String symbol, BollBand bollBand);

}
