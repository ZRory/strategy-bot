package net.chiaai.bot.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.*;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.utils.BinanceUtil;
import net.chiaai.bot.common.utils.IndicatorUtil;
import net.chiaai.bot.conf.GlobalConfig;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.entity.dto.BollBand;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.feign.request.LeverageRequest;
import net.chiaai.bot.entity.dao.CandleLine;
import net.chiaai.bot.feign.response.LeverageResponse;
import net.chiaai.bot.feign.response.PriceResponse;
import net.chiaai.bot.mapper.PositionMapper;
import net.chiaai.bot.mapper.StrategyConfigMapper;
import net.chiaai.bot.mapper.UserMapper;
import net.chiaai.bot.service.AutoTradeService;
import net.chiaai.bot.service.DealService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Service
public class AutoTradeCommonService implements AutoTradeService {

    @Resource
    private BinanceFeignClient binanceClient;

    @Resource
    private BinanceUtil binanceUtil;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private StrategyConfigMapper strategyConfigMapper;

    @Resource
    private DealService dealService;

    @Resource
    private UserMapper userMapper;

    /**
     * 自动补仓和止盈逻辑
     */
    @Override
    public void autoTrade() {
        log.debug("----------策略开始执行----------");
        //1.查询所有在运行中的策略
        LambdaQueryWrapper<StrategyConfig> strategyConfigQueryWrapper = new LambdaQueryWrapper<>();
        //封装查询条件
        strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.STOP);
        strategyConfigQueryWrapper.ne(StrategyConfig::getStatus, StrategyStatusEnum.ERROR);
        List<StrategyConfig> strategyConfigs = strategyConfigMapper.selectList(strategyConfigQueryWrapper);
        CountDownLatch tradeLatch = new CountDownLatch(strategyConfigs.size());
        for (StrategyConfig strategyConfig : strategyConfigs) {
            GlobalConfig.executor.submit(() -> strategyCore(strategyConfig, tradeLatch));
        }
        try {
            tradeLatch.await();
        } catch (InterruptedException e) {
        }
        log.debug("----------策略执行结束----------");
    }

    public void strategyCore(StrategyConfig strategyConfig, CountDownLatch tradeLatch) {
        try {
            log.debug("开始执行策略：{}", strategyConfig.getSymbol());
            //重新查询strategyConfig
            strategyConfig = strategyConfigMapper.selectById(strategyConfig.getId());
            if (strategyConfig.getAutoRestartLevel() != null && strategyConfig.getAutoRestartLevel() >= 2) {
                //判断是否平仓重开
                //1.查询所有策略下的单子
                LambdaQueryWrapper<Position> queryWrapper = new LambdaQueryWrapper<>();
                //封装查询条件
                queryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
                List<Position> positions = positionMapper.selectList(queryWrapper);
                if (!CollectionUtils.isEmpty(positions)
                        && positions.stream().max(Comparator.comparingInt(Position::getLevel)).get().getLevel() >= strategyConfig.getAutoRestartLevel()) {
                    //持有单子并且最大level比配置的要大
                    //判断平仓逻辑
                    //如果多空金额接近
                    //1.获取空单当前持仓金额
                    BigDecimal currentLongQuantity = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.LONG && x.getStatus() == PositionStatusEnum.HOLD).map(Position::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal currentShortQuantity = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.SHORT && x.getStatus() == PositionStatusEnum.HOLD).map(Position::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (currentLongQuantity.compareTo(BigDecimal.ZERO) != 0 &&
                            currentShortQuantity.compareTo(BigDecimal.ZERO) != 0) {
                        //多空差值 < 下一仓多单 或 下一仓空单的金额
                        //计算多单position
                        Position currentLongPosition = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.LONG && x.getStatus() == PositionStatusEnum.HOLD).max(Comparator.comparingInt(Position::getLevel)).get();
                        BigDecimal longPosition = strategyConfig.getFirstPosition().add(new BigDecimal(String.valueOf(currentLongPosition.getLevel())).multiply(strategyConfig.getSteppingPosition()));
                        //仓位 需要乘杠杆倍率
                        longPosition = longPosition.multiply(strategyConfig.getLeverage());
                        //BigDecimal position = new BigDecimal(positions[positionLevel - 1]);
                        BigDecimal longQuantity = binanceUtil.calcQuantity(strategyConfig.getSymbol(), longPosition);
                        //计算空单quantity
                        Position currentShortPosition = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.SHORT && x.getStatus() == PositionStatusEnum.HOLD).max(Comparator.comparingInt(Position::getLevel)).get();
                        BigDecimal shortPosition = strategyConfig.getFirstPosition().add(new BigDecimal(String.valueOf(currentShortPosition.getLevel())).multiply(strategyConfig.getSteppingPosition()));
                        //仓位 需要乘杠杆倍率
                        shortPosition = shortPosition.multiply(strategyConfig.getLeverage());
                        //BigDecimal position = new BigDecimal(positions[positionLevel - 1]);
                        BigDecimal shortQuantity = binanceUtil.calcQuantity(strategyConfig.getSymbol(), shortPosition);
                        BigDecimal diff = currentLongQuantity.subtract(currentShortQuantity).abs();
                        if (diff.compareTo(longQuantity) <= 0 ||
                                diff.compareTo(shortQuantity) <= 0) {
                            //触发自动关单逻辑
                            log.info("策略触发自动关单逻辑：diff:{},config:{}", diff, JSON.toJSONString(strategyConfig));
                            strategyConfig.setStatus(StrategyStatusEnum.STOP);
                            strategyConfig.setUpdateTime(LocalDateTime.now());
                            strategyConfig.setRemark("自动关单");
                            strategyConfigMapper.updateById(strategyConfig);
                            //卖出所有持仓
                            LambdaQueryWrapper<Position> positionQueryWrapper = new LambdaQueryWrapper<>();
                            //封装查询条件
                            positionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
                            positionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
                            List<Position> needClosePositions = positionMapper.selectList(positionQueryWrapper);
                            //卖出所有单子
                            for (Position needClosePosition : needClosePositions) {
                                dealService.closePosition(strategyConfig, needClosePosition, true);
                            }
                            if (strategyConfig.getAutoRestart()) {
                                //重新开单
                                strategyConfig.setRemark("自动开单：" + strategyConfig.getId());
                                strategyConfig.setStatus(StrategyStatusEnum.RUNNING);
                                strategyConfig.setId(null);
                                strategyConfig.setCreateTime(LocalDateTime.now());
                                strategyConfig.setUpdateTime(LocalDateTime.now());
                                strategyConfigMapper.insert(strategyConfig);
                            }
                            return;
                        }
                    }
                }
            }
            //查询当前价格
            PriceResponse priceResponse = binanceClient.price(strategyConfig.getSymbol());
            //查询持仓方式

            //查询多单
            LambdaQueryWrapper<Position> longPositionQueryWrapper = new LambdaQueryWrapper<>();
            //封装查询条件
            longPositionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
            longPositionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
            longPositionQueryWrapper.eq(Position::getPositionSide, PositionSideEnum.LONG);
            longPositionQueryWrapper.orderByDesc(Position::getLevel);
            //开单时间必须为1分钟前的单(1分钟内的单不处理)
            //longPositionQueryWrapper.le("create_time", LocalDateTime.now().minusMinutes(1L));
            longPositionQueryWrapper.last("limit 1");
            //查询仓位最高的多单 越大说明跌的越厉害
            Position currentLongPosition = positionMapper.selectOne(longPositionQueryWrapper);
            if (currentLongPosition == null) {
                if (strategyConfig.getStatus() == StrategyStatusEnum.RUNNING) {
                    if (strategyConfig.getPositionSide() == PositionSideEnum.LONG || strategyConfig.getPositionSide() == PositionSideEnum.BOTH) {
                        if (!skipCreate(strategyConfig, priceResponse.getPrice(), PositionSideEnum.LONG)) {
                            //调整币种杠杆倍率
                            changeLeverage(strategyConfig);
                            //创建首单
                            dealService.createPosition(strategyConfig, OrderSideEnum.BUY, PositionSideEnum.LONG, 1, BigDecimal.ZERO);
                        }
                    }
                }
            } else if (priceResponse.getPrice().compareTo(currentLongPosition.getPrice().multiply(BigDecimal.ONE.add(strategyConfig.getTargetRate().movePointLeft(2)))) >= 0) {
                //当前价格 > 开仓价格 * （1 + 止盈率） 说明到达止盈点位
                //判断回撤值是否符合要求（根据K线获取最大点位）
                if (strategyConfig.getTargetShrinksRate().compareTo(BigDecimal.ZERO) == 0 || priceResponse.getPrice().compareTo(binanceUtil.getHighestPrice(strategyConfig.getSymbol(), currentLongPosition.getCreateTime()).multiply(BigDecimal.ONE.subtract(strategyConfig.getTargetShrinksRate().movePointLeft(2)))) <= 0) {
                    //当前价格 < 最高价格 * （1 - 止盈回撤率） 说明回撤了止盈回撤点位
                    //执行平仓操作
                    dealService.closePosition(strategyConfig, currentLongPosition, false);
                }
            } else if (strategyConfig.getStatus() == StrategyStatusEnum.RUNNING && (strategyConfig.getTimes() == 0 || currentLongPosition.getLevel() < strategyConfig.getTimes()) && priceResponse.getPrice().compareTo(currentLongPosition.getPrice().multiply(BigDecimal.ONE.subtract(strategyConfig.getCoverRate().movePointLeft(2)))) <= 0) {
                //仓位不满 且 当前价格 <= 开仓价格 * （1 - 补仓率） 说明到达补仓点位
                //判断补仓回撤是否符合要求
                if (strategyConfig.getPositionSide() == PositionSideEnum.LONG || strategyConfig.getPositionSide() == PositionSideEnum.BOTH) {
                    if (strategyConfig.getCoverShrinksRate().compareTo(BigDecimal.ZERO) == 0 || priceResponse.getPrice().compareTo(binanceUtil.getLowestPrice(strategyConfig.getSymbol(), currentLongPosition.getCreateTime()).multiply(BigDecimal.ONE.add(strategyConfig.getCoverShrinksRate().movePointLeft(2)))) >= 0) {
                        //当前价格 >= 最低价格 * （1 + 补仓回撤率） 说明回撤到回撤点位之上
                        //创建补仓订单
                        if (!skipCreate(strategyConfig, priceResponse.getPrice(), PositionSideEnum.LONG)) {
                            dealService.createPosition(strategyConfig, OrderSideEnum.BUY, PositionSideEnum.LONG, currentLongPosition.getLevel() + 1, currentLongPosition.getPrice());
                        }
                    }
                }
            }
            if (strategyConfig.getStopRate() != null && strategyConfig.getStopRate().compareTo(BigDecimal.ZERO) > 0) {
                //多单止损
                longPositionQueryWrapper.clear();
                //封装查询条件
                longPositionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
                longPositionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
                longPositionQueryWrapper.eq(Position::getPositionSide, PositionSideEnum.LONG);
                longPositionQueryWrapper.orderByAsc(Position::getLevel);
                longPositionQueryWrapper.last("limit 1");
                //查开单最早的一个单
                Position longPosition = positionMapper.selectOne(longPositionQueryWrapper);
                if (longPosition != null && priceResponse.getPrice().compareTo(longPosition.getPrice().multiply(BigDecimal.ONE.subtract(strategyConfig.getStopRate().movePointLeft(2)))) <= 0) {
                    //百分比止损
                    dealService.closePosition(strategyConfig, longPosition, false);
                    //log.info("[{}]订单方向:{},  止损：止损时间：{}  止损价格：{} | 成本价：{}", strategyConfig.getSymbol(), x.getPositionSide(), LocalDateTime.now(), priceResponse.getPrice(), x.getPrice());
                }
            }

            //查询空单
            LambdaQueryWrapper<Position> shortPositionQueryWrapper = new LambdaQueryWrapper<>();
            //封装查询条件
            shortPositionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
            shortPositionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
            shortPositionQueryWrapper.eq(Position::getPositionSide, PositionSideEnum.SHORT);
            shortPositionQueryWrapper.orderByDesc(Position::getLevel);
            //开单时间必须为1分钟前的单(1分钟内的单不处理)
            //shortPositionQueryWrapper.le("create_time", LocalDateTime.now().minusMinutes(1L));
            shortPositionQueryWrapper.last("limit 1");
            //查询仓位最高的空单 越大说明涨的越厉害
            Position currentShortPosition = positionMapper.selectOne(shortPositionQueryWrapper);
            if (currentShortPosition == null) {
                if (strategyConfig.getStatus() == StrategyStatusEnum.RUNNING) {
                    if (strategyConfig.getPositionSide() == PositionSideEnum.SHORT || strategyConfig.getPositionSide() == PositionSideEnum.BOTH) {
                        //调整币种杠杆倍率
                        if (!skipCreate(strategyConfig, priceResponse.getPrice(), PositionSideEnum.SHORT)) {
                            //买入首仓
                            changeLeverage(strategyConfig);
                            dealService.createPosition(strategyConfig, OrderSideEnum.SELL, PositionSideEnum.SHORT, 1, BigDecimal.ZERO);
                        }
                    }
                }
            } else if (priceResponse.getPrice().compareTo(currentShortPosition.getPrice().multiply(BigDecimal.ONE.subtract(strategyConfig.getTargetRate().movePointLeft(2)))) <= 0) {
                //当前价格 <= 开仓价格 * （1 - 止盈率） 说明到达止盈点位
                //判断回撤值是否符合要求（根据K线获取最小点位）
                if (strategyConfig.getTargetShrinksRate().compareTo(BigDecimal.ZERO) == 0 || priceResponse.getPrice().compareTo(binanceUtil.getLowestPrice(strategyConfig.getSymbol(), currentShortPosition.getCreateTime()).multiply(BigDecimal.ONE.add(strategyConfig.getTargetShrinksRate().movePointLeft(2)))) >= 0) {
                    //当前价格 > 最低价格 * （1 + 止盈回撤率） 说明回撤了止盈回撤点位
                    //执行平仓操作
                    dealService.closePosition(strategyConfig, currentShortPosition, false);
                }
            } else if (strategyConfig.getStatus() == StrategyStatusEnum.RUNNING && (strategyConfig.getTimes() == 0 || currentShortPosition.getLevel() < strategyConfig.getTimes()) && priceResponse.getPrice().compareTo(currentShortPosition.getPrice().multiply(BigDecimal.ONE.add(strategyConfig.getCoverRate().movePointLeft(2)))) >= 0) {
                if (strategyConfig.getPositionSide() == PositionSideEnum.SHORT || strategyConfig.getPositionSide() == PositionSideEnum.BOTH) {
                    //仓位不满 且 当前价格 >= 开仓价格 * （1 + 补仓率） 说明到达补仓点位
                    //判断补仓回撤是否符合要求
                    if (strategyConfig.getCoverShrinksRate().compareTo(BigDecimal.ZERO) == 0 || priceResponse.getPrice().compareTo(binanceUtil.getHighestPrice(strategyConfig.getSymbol(), currentShortPosition.getCreateTime()).multiply(BigDecimal.ONE.subtract(strategyConfig.getCoverShrinksRate().movePointLeft(2)))) <= 0) {
                        //当前价格 <= 最高价格 * （1 - 补仓回撤率） 说明回撤到回撤点位之上
                        //补仓空单
                        if (!skipCreate(strategyConfig, priceResponse.getPrice(), PositionSideEnum.SHORT)) {
                            dealService.createPosition(strategyConfig, OrderSideEnum.SELL, PositionSideEnum.SHORT, currentShortPosition.getLevel() + 1, currentShortPosition.getPrice());
                        }
                    }
                }
            }
            if (strategyConfig.getStopRate() != null && strategyConfig.getStopRate().compareTo(BigDecimal.ZERO) > 0) {
                //空单止损
                shortPositionQueryWrapper.clear();
                //封装查询条件
                shortPositionQueryWrapper.eq(Position::getStrategyConfigId, strategyConfig.getId());
                shortPositionQueryWrapper.eq(Position::getStatus, PositionStatusEnum.HOLD);
                shortPositionQueryWrapper.eq(Position::getPositionSide, PositionSideEnum.SHORT);
                shortPositionQueryWrapper.orderByAsc(Position::getLevel);
                shortPositionQueryWrapper.last("limit 1");
                Position shortPosion = positionMapper.selectOne(shortPositionQueryWrapper);
                if (shortPosion != null && priceResponse.getPrice().compareTo(shortPosion.getPrice().multiply(BigDecimal.ONE.add(strategyConfig.getStopRate().movePointLeft(2)))) >= 0) {
                    //百分比止损
                    dealService.closePosition(strategyConfig, shortPosion, false);
                    //log.info("[{}]订单方向:{},  止损：止损时间：{}  止损价格：{} | 成本价：{}", strategyConfig.getSymbol(), x.getPositionSide(), LocalDateTime.now(), priceResponse.getPrice(), x.getPrice());
                }
            }
        } catch (BizException e) {
            log.error("币种：{} autoTrade执行biz异常", strategyConfig.getSymbol(), e);
            strategyConfig.setRemark(e.getMessage());
            strategyConfig.setStatus(StrategyStatusEnum.ERROR);
            strategyConfigMapper.updateById(strategyConfig);
        } catch (Exception e) {
            log.error("币种：{} autoTrade执行系统异常", strategyConfig.getSymbol(), e);
        } finally {
            //释放
            tradeLatch.countDown();
        }
    }

    /**
     * 是否跳过买入
     */
    private Boolean skipCreate(StrategyConfig strategyConfig, BigDecimal price, PositionSideEnum positionSideEnum) {
        //BOLL线判断是否上插针（如果上插针则不买入做多！）
        CandleLineRequest candleLineRequest = new CandleLineRequest();
        candleLineRequest.setSymbol(strategyConfig.getSymbol());
        candleLineRequest.setLimit(21L);
        candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
        List<CandleLine> candleLines = binanceClient.candleLines(candleLineRequest);
        if (candleLines.size() < 21) {
            //指标不足以计算布林线
            return false;
        }
        //计算boll值
        BollBand bollBand = IndicatorUtil.getCurrentBoll(candleLines);
        if (positionSideEnum == PositionSideEnum.LONG && price.compareTo(bollBand.getUpper().add(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) >= 0) {
            //上插针的话 超过 上轨道 +（md/2） 就不做多了(防止被套)
            log.info("创建仓位：做多，判断为上插针，不执行买入：{}", JSON.toJSONString(strategyConfig));
            return true;
        }
        if (positionSideEnum == PositionSideEnum.SHORT && price.compareTo(bollBand.getLower().subtract(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) < 0) {
            //下插针的话 超过 下轨道 - （md/2） 就不做空了(防止被套)
            log.info("创建仓位：做空，判断为下插针，不执行买入：{}", JSON.toJSONString(strategyConfig));
            return true;
        }
        return false;
    }

    private void changeLeverage(StrategyConfig strategyConfig) {
        try {
            User user = userMapper.selectById(strategyConfig.getUserId());
            LeverageResponse leverageResp = binanceClient.leverage(user.getApiKey(), BinanceUtil.encodeParams(new LeverageRequest(strategyConfig.getSymbol(), strategyConfig.getLeverage().intValue()), user));
            log.info("调整杠杆倍率：{}", JSON.toJSONString(leverageResp));
            if (leverageResp.getCode() != 200) {
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "调整杠杆倍率失败:" + leverageResp.getMsg());
            }
        } catch (Exception e) {
            log.error("调整杠杆倍率失败", e);
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "调整杠杆倍率失败");
        }
    }

}
