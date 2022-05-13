package net.chiaai.bot.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.IntervalTypeEnum;
import net.chiaai.bot.common.enums.PositionSideEnum;
import net.chiaai.bot.common.enums.StrategyStatusEnum;
import net.chiaai.bot.common.utils.IndicatorUtil;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.dto.BollBand;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.entity.dao.CandleLine;
import net.chiaai.bot.mapper.PositionMapper;
import net.chiaai.bot.mapper.StrategyConfigMapper;
import net.chiaai.bot.service.AutoTradeService;
import net.chiaai.bot.service.StrategyConfigService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TradeTask {

    public static Boolean enable = true;

    @Resource
    private AutoTradeService autoTradeService;

    @Resource
    private StrategyConfigMapper strategyConfigMapper;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private BinanceFeignClient binanceClient;

    @Resource
    private StrategyConfigService strategyConfigService;

    @Scheduled(initialDelay = 20_000, fixedDelay = 1_500)
    public void autoTradeTask() {
        if (enable) {
            autoTradeService.autoTrade();
        }
    }

    @Scheduled(initialDelay = 5_000, fixedDelay = 18_00_000)
    public void updateErrorTask() {
        //查询所有异常的策略
        LambdaQueryWrapper<StrategyConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyConfig::getStatus, StrategyStatusEnum.ERROR);
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(wrapper);
        for (StrategyConfig strategyConfig : strategyConfigs) {
            try {
                strategyConfig.setRemark("AUTO ERROR->RUNNING:" + strategyConfig.getRemark());
                strategyConfig.setStatus(StrategyStatusEnum.RUNNING);
                strategyConfigMapper.updateById(strategyConfig);
            } catch (Exception e) {
                log.error("自动刷新ERROR数据异常:", e);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") //每天凌晨执行一次
    public void updateShareTask() {
        //查询所有分享的策略
        LambdaQueryWrapper<StrategyConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
        wrapper.eq(StrategyConfig::getShare, true);
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(strategyConfigs)) {
            return;
        }
        for (StrategyConfig strategyConfig : strategyConfigs) {
            try {
                //查询仓位
                LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
                //封装查询条件
                positionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
                Long positionCount = positionMapper.selectCount(positionQueryWrapper);
                if (positionCount < 20) {
                    //总仓位 < 20的 不分享
                    StrategyConfig updateConfig = new StrategyConfig();
                    updateConfig.setId(strategyConfig.getId());
                    updateConfig.setShare(false);
                    strategyConfigMapper.updateById(updateConfig);
                }
            } catch (Exception e) {
                log.error("自动取消无用分享数据异常:", e);
            }
        }
    }

    /**
     * 自动切换订单方向
     */
    //@Scheduled(cron = "0 0/1 * * * ?") //每一分钟执行一次
    public void autoSwitchSide() {
        try {
            //查询所有需要自动切换的策略
            LambdaQueryWrapper<StrategyConfig> wrapper = new LambdaQueryWrapper<>();
            //非终止策略
            wrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
            //自动切换策略
            wrapper.eq(StrategyConfig::getAutoSwitch, true);
            List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(wrapper);
            Map<String, BollBand> bollBands = new HashMap<>();
            if (CollectionUtils.isEmpty(strategyConfigs)) {
                return;
            }
            for (StrategyConfig strategyConfig : strategyConfigs) {
                BollBand bollBand = bollBands.get(strategyConfig.getSymbol());
                if (bollBand == null) {
                    //查询K线 21条
                    CandleLineRequest candleLineRequest = new CandleLineRequest();
                    candleLineRequest.setSymbol(strategyConfig.getSymbol());
                    candleLineRequest.setLimit(21L);
                    candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
                    List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
                    //计算boll值
                    bollBand = IndicatorUtil.getCurrentBoll(candleLines);
                    bollBands.put(strategyConfig.getSymbol(), bollBand);
                }
                PositionSideEnum positionSide = strategyConfigService.calPositionSide(strategyConfig.getSymbol(), bollBand);
                if (positionSide != strategyConfig.getPositionSide()) {
                    StrategyConfig updateConfig = new StrategyConfig();
                    updateConfig.setId(strategyConfig.getId());
                    updateConfig.setPositionSide(positionSide);
                    updateConfig.setRemark("AUTO-UPDATE-P_SIDE");
                    updateConfig.setUpdateTime(LocalDateTime.now());
                    strategyConfigMapper.updateById(updateConfig);
                }
            }
        } catch (Exception e) {
            log.error("自动刷新ERROR数据异常:", e);
        }
    }

}
