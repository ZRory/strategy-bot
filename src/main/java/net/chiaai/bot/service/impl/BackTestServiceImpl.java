package net.chiaai.bot.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.*;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.utils.BinanceUtil;
import net.chiaai.bot.common.utils.IndicatorUtil;
import net.chiaai.bot.common.utils.SessionUtils;
import net.chiaai.bot.common.web.BaseUser;
import net.chiaai.bot.entity.dao.*;
import net.chiaai.bot.entity.dto.BollBand;
import net.chiaai.bot.entity.response.PositionInfo;
import net.chiaai.bot.entity.response.ProfitVo;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.mapper.BackTestMapper;
import net.chiaai.bot.mapper.BackTestResultMapper;
import net.chiaai.bot.service.BackTestService;
import net.chiaai.bot.service.CandleLineService;
import net.chiaai.bot.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BackTestServiceImpl implements BackTestService {

    @Resource
    private BackTestMapper backTestMapper;

    @Resource
    private BackTestResultMapper backTestResultMapper;

    @Resource
    private CandleLineService candleLineService;

    @Resource
    private BinanceFeignClient binanceFeignClient;

    @Resource
    private BinanceUtil binanceUtil;

    @Resource
    private UserService userService;

    private BigDecimal serviceFee = new BigDecimal("0.0004");

    @Override
    public BackTest createBackTest(BackTest backTest) {
        //????????????ID
        BaseUser loginUser = SessionUtils.getLoginUser();
        User user = userService.getUserByPhone(loginUser.getPhone());
        //VIP????????????
        if (!user.getInviteUser()) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????????????????????????????????????????");
        }
        //????????????
        CandleLine startCandleLine = candleLineService.getCandleLine(backTest.getSymbol(), backTest.getStartTime());
        if (ObjectUtils.isEmpty(startCandleLine)) {
            log.debug("??????K????????????{}???{}", backTest.getSymbol(), backTest.getStartTime());
            throw new BizException(BizCodeEnum.PARAM_ERROR, "??????K????????????:???????????????????????????2022-1-1 00:00??????");
        }
        CandleLine endCandleLine = candleLineService.getCandleLine(backTest.getSymbol(), backTest.getEndTime());
        if (ObjectUtils.isEmpty(endCandleLine)) {
            log.debug("??????K????????????{}???{}", backTest.getSymbol(), backTest.getEndTime());
            throw new BizException(BizCodeEnum.PARAM_ERROR, "??????K????????????:????????????????????????????????????00:00??????");
        }
        binanceUtil.calcQuantity(backTest.getSymbol(), backTest.getLeverage().multiply(backTest.getFirstPosition()), startCandleLine.getClose());
        //???????????????RUNNING??????
        LambdaQueryWrapper<BackTest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BackTest::getStatus, StrategyStatusEnum.RUNNING);
        queryWrapper.eq(BackTest::getUserId, loginUser.getId());
        Long runningCount = backTestMapper.selectCount(queryWrapper);
        if (runningCount > 0) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "????????????????????????????????????????????????????????????");
        }
        backTest.setUserId(loginUser.getId());
        //??????????????????
        backTest.setStatus(StrategyStatusEnum.RUNNING);
        backTest.setCreateTime(LocalDateTime.now());
        backTest.setUpdateTime(LocalDateTime.now());
        backTestMapper.insert(backTest);
        return backTest;
    }

    /**
     * ????????????backtest
     */
    @Async
    @Override
    public void execBackTest(BackTest backTest) {
        try {
            LocalDateTime currentTime = backTest.getStartTime();
            List<Position> positions = new ArrayList<>();
            List<List<Position>> backTestPositions = new ArrayList<>();
            List<BackTestResult> backTestResults = new ArrayList<>();
            BackTestResult backTestResult = new BackTestResult();
            Map<Long, List<CandleLine>> skipCache = new HashMap<>();
            //??????????????????????????????
            while (currentTime.compareTo(backTest.getEndTime()) < 0 && backTest.getStatus() == StrategyStatusEnum.RUNNING) {
                if (backTestResult.getStartTime() == null) {
                    //??????????????????????????????
                    backTestResult.setStartTime(currentTime);
                }
                //????????????K???
                CandleLine currentCandleLine = candleLineService.getCandleLine(backTest.getSymbol(), currentTime);
                if (ObjectUtils.isEmpty(currentCandleLine)) {
                    log.debug("K????????????{}???{}", backTest.getSymbol(), currentTime);
                    currentTime = currentTime.plusMinutes(1);
                    continue;
                }
                BigDecimal currentPrice = BigDecimal.valueOf(RandomUtils.nextDouble(Math.min(currentCandleLine.getOpen().doubleValue(), currentCandleLine.getClose().doubleValue()), Math.max(currentCandleLine.getOpen().doubleValue(), currentCandleLine.getClose().doubleValue())));
                if (backTest.getAutoRestartLevel() != null && backTest.getAutoRestartLevel() >= 2) {
                    //????????????????????????
                    if (!CollectionUtils.isEmpty(positions)
                            && positions.stream().max(Comparator.comparingInt(Position::getLevel)).get().getLevel() >= backTest.getAutoRestartLevel()) {
                        //????????????????????????level??????????????????
                        //??????????????????
                        //????????????????????????
                        //1.???????????????????????????
                        BigDecimal currentLongQuantity = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.LONG && x.getStatus() == PositionStatusEnum.HOLD).map(Position::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal currentShortQuantity = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.SHORT && x.getStatus() == PositionStatusEnum.HOLD).map(Position::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                        if (currentLongQuantity.compareTo(BigDecimal.ZERO) != 0 &&
                                currentShortQuantity.compareTo(BigDecimal.ZERO) != 0) {
                            //???????????? < ??????????????? ??? ????????????????????????
                            //????????????position
                            Position currentLongPosition = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.LONG && x.getStatus() == PositionStatusEnum.HOLD).max(Comparator.comparingInt(Position::getLevel)).get();
                            BigDecimal longPosition = backTest.getFirstPosition().add(new BigDecimal(String.valueOf(currentLongPosition.getLevel())).multiply(backTest.getSteppingPosition()));
                            //?????? ?????????????????????
                            longPosition = longPosition.multiply(backTest.getLeverage());
                            //BigDecimal position = new BigDecimal(positions[positionLevel - 1]);
                            BigDecimal longQuantity = binanceUtil.calcQuantity(backTest.getSymbol(), longPosition, currentPrice);
                            //????????????quantity
                            Position currentShortPosition = positions.stream().filter(x -> x.getPositionSide() == PositionSideEnum.SHORT && x.getStatus() == PositionStatusEnum.HOLD).max(Comparator.comparingInt(Position::getLevel)).get();
                            BigDecimal shortPosition = backTest.getFirstPosition().add(new BigDecimal(String.valueOf(currentShortPosition.getLevel())).multiply(backTest.getSteppingPosition()));
                            //?????? ?????????????????????
                            shortPosition = shortPosition.multiply(backTest.getLeverage());
                            //BigDecimal position = new BigDecimal(positions[positionLevel - 1]);
                            BigDecimal shortQuantity = binanceUtil.calcQuantity(backTest.getSymbol(), shortPosition, currentPrice);
                            BigDecimal diff = currentLongQuantity.subtract(currentShortQuantity).abs();
                            if (diff.compareTo(longQuantity) <= 0 ||
                                    diff.compareTo(shortQuantity) <= 0) {
                                //????????????????????????
                                //????????????
                                log.debug("?????????????????????????????????diff:{},config:{}", diff, JSON.toJSONString(backTest));
                                backTest.setStatus(StrategyStatusEnum.STOP);
                                backTest.setUpdateTime(currentTime);
                                //??????????????????
                                List<Position> needClosePositions = positions.stream().filter(x -> x.getStatus() == PositionStatusEnum.HOLD).collect(Collectors.toList());
                                //??????????????????
                                for (Position needClosePosition : needClosePositions) {
                                    closePosition(backTest, needClosePosition, currentPrice, currentTime);
                                }
                                if (backTest.getAutoRestart()) {
                                    //????????????,?????????????????????????????????????????????
                                    backTestPositions.add(positions);
                                    backTestResult.setStatus(StrategyStatusEnum.STOP);
                                    backTestResult.setEndTime(currentTime);
                                    backTestResults.add(backTestResult);
                                    //????????????
                                    backTest.setStatus(StrategyStatusEnum.RUNNING);
                                    //?????????????????????
                                    positions = new ArrayList<>();
                                    backTestResult = new BackTestResult();
                                }
                                currentTime = currentTime.plusMinutes(1);
                                continue;
                            }
                        }
                    }
                }
                //????????????
                //??????????????????????????? ???????????????????????????
                Position currentLongPosition = null;
                Optional<Position> currentLongPositionOptional = positions.stream().filter(x -> x.getStatus() == PositionStatusEnum.HOLD && x.getPositionSide() == PositionSideEnum.LONG).max(Comparator.comparing(Position::getLevel));
                if (currentLongPositionOptional.isPresent()) {
                    currentLongPosition = currentLongPositionOptional.get();
                }
                if (currentLongPosition == null) {
                    if (backTest.getStatus() == StrategyStatusEnum.RUNNING) {
                        if (backTest.getPositionSide() == PositionSideEnum.LONG || backTest.getPositionSide() == PositionSideEnum.BOTH) {
                            if (!skipCreate(backTest, currentPrice, PositionSideEnum.LONG, currentTime, skipCache)) {
                                //????????????
                                Position position = createPosition(backTest, OrderSideEnum.BUY, PositionSideEnum.LONG, 1, BigDecimal.ZERO, currentPrice, currentTime);
                                positions.add(position);
                            }
                        }
                    }
                } else if (currentPrice.compareTo(currentLongPosition.getPrice().multiply(BigDecimal.ONE.add(backTest.getTargetRate().movePointLeft(2)))) >= 0) {
                    //???????????? > ???????????? * ???1 + ???????????? ????????????????????????
                    //??????????????????????????????????????????K????????????????????????
                    if (backTest.getTargetShrinksRate().compareTo(BigDecimal.ZERO) == 0
                            || currentPrice.compareTo(candleLineService.getMax(backTest.getSymbol(), currentLongPosition.getCreateTime(), currentTime, CandleLine::getHigh).getHigh()
                            .multiply(BigDecimal.ONE.subtract(backTest.getTargetShrinksRate().movePointLeft(2)))) <= 0) {
                        //???????????? < ???????????? * ???1 - ?????????????????? ?????????????????????????????????
                        //??????????????????
                        closePosition(backTest, currentLongPosition, currentPrice, currentTime);
                    }
                } else if (backTest.getStatus() == StrategyStatusEnum.RUNNING && (backTest.getTimes() == 0 || currentLongPosition.getLevel() < backTest.getTimes()) && currentPrice.compareTo(currentLongPosition.getPrice().multiply(BigDecimal.ONE.subtract(backTest.getCoverRate().movePointLeft(2)))) <= 0) {
                    //???????????? ??? ???????????? <= ???????????? * ???1 - ???????????? ????????????????????????
                    //????????????????????????????????????
                    if (backTest.getPositionSide() == PositionSideEnum.LONG || backTest.getPositionSide() == PositionSideEnum.BOTH) {
                        if (backTest.getCoverShrinksRate().compareTo(BigDecimal.ZERO) == 0
                                || currentPrice.compareTo(candleLineService.getMin(backTest.getSymbol(), currentLongPosition.getCreateTime(), currentTime, CandleLine::getLow).getLow()
                                .multiply(BigDecimal.ONE.add(backTest.getCoverShrinksRate().movePointLeft(2)))) >= 0) {
                            //???????????? >= ???????????? * ???1 + ?????????????????? ?????????????????????????????????
                            //??????????????????
                            if (!skipCreate(backTest, currentPrice, PositionSideEnum.LONG, currentTime, skipCache)) {
                                Position position = createPosition(backTest, OrderSideEnum.BUY, PositionSideEnum.LONG, currentLongPosition.getLevel() + 1, currentLongPosition.getPrice(), currentPrice, currentTime);
                                positions.add(position);
                            }
                        }
                    }
                }
                if (backTest.getStopRate() != null && backTest.getStopRate().compareTo(BigDecimal.ZERO) > 0) {
                    //????????????
                    //???????????????????????????
                    Position longPosition = null;
                    Optional<Position> longPositionOptional = positions.stream().filter(x -> x.getStatus() == PositionStatusEnum.HOLD && x.getPositionSide() == PositionSideEnum.LONG).min(Comparator.comparing(Position::getLevel));
                    if (longPositionOptional.isPresent()) {
                        longPosition = longPositionOptional.get();
                    }
                    if (longPosition != null && currentPrice.compareTo(longPosition.getPrice().multiply(BigDecimal.ONE.subtract(backTest.getStopRate().movePointLeft(2)))) <= 0) {
                        //???????????????
                        closePosition(backTest, longPosition, currentPrice, currentTime);
                    }
                }

                //????????????
                Position currentShortPosition = null;
                Optional<Position> currentShortPositionOptional = positions.stream().filter(x -> x.getStatus() == PositionStatusEnum.HOLD && x.getPositionSide() == PositionSideEnum.SHORT).max(Comparator.comparing(Position::getLevel));
                if (currentShortPositionOptional.isPresent()) {
                    currentShortPosition = currentShortPositionOptional.get();
                }
                if (currentShortPosition == null) {
                    if (backTest.getStatus() == StrategyStatusEnum.RUNNING) {
                        if (backTest.getPositionSide() == PositionSideEnum.SHORT || backTest.getPositionSide() == PositionSideEnum.BOTH) {
                            //????????????????????????
                            if (!skipCreate(backTest, currentPrice, PositionSideEnum.SHORT, currentTime, skipCache)) {
                                //????????????
                                Position position = createPosition(backTest, OrderSideEnum.SELL, PositionSideEnum.SHORT, 1, BigDecimal.ZERO, currentPrice, currentTime);
                                positions.add(position);
                            }
                        }
                    }
                } else if (currentPrice.compareTo(currentShortPosition.getPrice().multiply(BigDecimal.ONE.subtract(backTest.getTargetRate().movePointLeft(2)))) <= 0) {
                    //???????????? <= ???????????? * ???1 - ???????????? ????????????????????????
                    //??????????????????????????????????????????K????????????????????????
                    if (backTest.getTargetShrinksRate().compareTo(BigDecimal.ZERO) == 0
                            || currentPrice.compareTo(candleLineService.getMin(backTest.getSymbol(), currentShortPosition.getCreateTime(), currentTime, CandleLine::getLow).getLow()
                            .multiply(BigDecimal.ONE.add(backTest.getTargetShrinksRate().movePointLeft(2)))) >= 0) {
                        //???????????? > ???????????? * ???1 + ?????????????????? ?????????????????????????????????
                        //??????????????????
                        closePosition(backTest, currentShortPosition, currentPrice, currentTime);
                    }
                } else if (backTest.getStatus() == StrategyStatusEnum.RUNNING && (backTest.getTimes() == 0 || currentShortPosition.getLevel() < backTest.getTimes()) && currentPrice.compareTo(currentShortPosition.getPrice().multiply(BigDecimal.ONE.add(backTest.getCoverRate().movePointLeft(2)))) >= 0) {
                    if (backTest.getPositionSide() == PositionSideEnum.SHORT || backTest.getPositionSide() == PositionSideEnum.BOTH) {
                        //???????????? ??? ???????????? >= ???????????? * ???1 + ???????????? ????????????????????????
                        //????????????????????????????????????
                        if (backTest.getCoverShrinksRate().compareTo(BigDecimal.ZERO) == 0
                                || currentPrice.compareTo(candleLineService.getMax(backTest.getSymbol(), currentShortPosition.getCreateTime(), currentTime, CandleLine::getHigh).getHigh()
                                .multiply(BigDecimal.ONE.subtract(backTest.getCoverShrinksRate().movePointLeft(2)))) <= 0) {
                            //???????????? <= ???????????? * ???1 - ?????????????????? ?????????????????????????????????
                            //????????????
                            if (!skipCreate(backTest, currentPrice, PositionSideEnum.SHORT, currentTime, skipCache)) {
                                Position position = createPosition(backTest, OrderSideEnum.SELL, PositionSideEnum.SHORT, currentShortPosition.getLevel() + 1, currentShortPosition.getPrice(), currentPrice, currentTime);
                                positions.add(position);
                            }
                        }
                    }
                }
                if (backTest.getStopRate() != null && backTest.getStopRate().compareTo(BigDecimal.ZERO) > 0) {
                    Position shortPosion = null;
                    Optional<Position> shortPositionOptional = positions.stream().filter(x -> x.getStatus() == PositionStatusEnum.HOLD && x.getPositionSide() == PositionSideEnum.SHORT).min(Comparator.comparing(Position::getLevel));
                    if (shortPositionOptional.isPresent()) {
                        shortPosion = shortPositionOptional.get();
                    }
                    if (shortPosion != null && currentPrice.compareTo(shortPosion.getPrice().multiply(BigDecimal.ONE.add(backTest.getStopRate().movePointLeft(2)))) >= 0) {
                        //???????????????
                        closePosition(backTest, shortPosion, currentPrice, currentTime);
                        //log.debug("[{}]????????????:{},  ????????????????????????{}  ???????????????{} | ????????????{}", backTest.getSymbol(), x.getPositionSide(), LocalDateTime.now(), currentPrice, x.getPrice());
                    }
                }
                currentTime = currentTime.plusMinutes(1);
            }
            //????????????
            backTestPositions.add(positions);
            backTestResult.setEndTime(currentTime);
            if (backTestResult.getStatus() == null) {
                backTestResult.setStatus(StrategyStatusEnum.RUNNING);
            }
            backTestResults.add(backTestResult);

            backTest.setUpdateTime(LocalDateTime.now());
            backTest.setStatus(StrategyStatusEnum.STOP);
            backTestMapper.updateById(backTest);

            CandleLine currentCandleLine = candleLineService.getCandleLine(backTest.getSymbol(), currentTime);
            BigDecimal currentPrice = BigDecimal.valueOf(RandomUtils.nextDouble(Math.min(currentCandleLine.getOpen().doubleValue(), currentCandleLine.getClose().doubleValue()), Math.max(currentCandleLine.getOpen().doubleValue(), currentCandleLine.getClose().doubleValue())));

            for (int i = 0; i < backTestResults.size(); i++) {
                positions = backTestPositions.get(i);
                if (CollectionUtils.isEmpty(positions)) {
                    continue;
                }
                backTestResult = backTestResults.get(i);
                backTestResult.setBackTaskId(backTest.getId());
                backTestResult.setMaxLevel(positions.stream().max(Comparator.comparingInt(Position::getLevel)).get().getLevel());
                backTestResult.setTimes(positions.size());
                ProfitVo profitVo = calProfit(positions, currentPrice, backTest);
                backTestResult.setMaxAmount(profitVo.getMaxAmount().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setTotalProfit(profitVo.getTotalProfit().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setServiceAmount(profitVo.getServiceAmount().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setSlidingProfit(profitVo.getSlidingProfit().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setCurrentProfit(profitVo.getCurrentProfit().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setTargetProfit(profitVo.getTargetProfit().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setStopProfit(profitVo.getStopProfit().setScale(2, RoundingMode.HALF_DOWN));
                backTestResult.setCreateTime(LocalDateTime.now());
                backTestResult.setUpdateTime(LocalDateTime.now());
                backTestResultMapper.insert(backTestResult);
            }
        } catch (Exception e) {
            backTest.setUpdateTime(LocalDateTime.now());
            backTest.setStatus(StrategyStatusEnum.ERROR);
            backTestMapper.updateById(backTest);
            log.error("?????????????????????{}", JSON.toJSONString(backTest), e);
        }
    }

    @Override
    public Page<BackTest> list(Integer pageNum, Integer pageSize, String symbol, String status, String column, String sort) {
        LambdaQueryWrapper<BackTest> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(symbol)) {
            queryWrapper.eq(BackTest::getSymbol, symbol);
        }
        if (StringUtils.isNotBlank(status)) {
            queryWrapper.eq(BackTest::getStatus, status);
        }
        if (StringUtils.isNotBlank(column) && StringUtils.isNotBlank(sort)) {
            queryWrapper.last("ORDER BY " + getColumnByField(BackTest.class, column) + " " + sort);
        }
        return backTestMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    @Override
    public Page<BackTestResult> detail(Integer pageNum, Integer pageSize, Long backTestId, String column, String sort) {
        LambdaQueryWrapper<BackTestResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BackTestResult::getBackTaskId, backTestId);
        if (StringUtils.isNotBlank(column) && StringUtils.isNotBlank(sort)) {
            queryWrapper.last("ORDER BY " + getColumnByField(BackTestResult.class, column) + " " + sort);
        }
        return backTestResultMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
    }

    protected String getColumnByField(Class<?> clazz, String fieldName) {
        //?????????????????????????????????????????????
        try {
            Field field = clazz.getDeclaredField(fieldName);
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null) {
                return tableField.value();
            }
            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId != null) {
                return tableId.value();
            }
            return fieldName;
        } catch (NoSuchFieldException e) {
            throw new BizException(BizCodeEnum.PARAM_ERROR, "???????????????????????????");
        }
    }

    private ProfitVo calProfit(List<Position> positions, BigDecimal currentPrice, BackTest backTest) {
        List<PositionInfo> positionInfos = positions.stream().map(x -> {
            PositionInfo positionInfo = new PositionInfo();
            BeanUtils.copyProperties(x, positionInfo);
            positionInfo.setPositionAmount(positionInfo.getPrice().multiply(positionInfo.getQuantity()).divide(backTest.getLeverage(), 8, RoundingMode.HALF_DOWN));
            if (positionInfo.getStatus() == PositionStatusEnum.HOLD) {
                //??????????????????
                if (positionInfo.getPositionSide() == PositionSideEnum.LONG) {
                    //???????????????-??????????????? ?? ??????
                    positionInfo.setSlidingProfitAmount(currentPrice.subtract(positionInfo.getPrice()).multiply(positionInfo.getQuantity()));
                } else {
                    //???????????????-??????????????? ?? ??????
                    positionInfo.setSlidingProfitAmount(positionInfo.getPrice().subtract(currentPrice).multiply(positionInfo.getQuantity()));
                }
            }
            return positionInfo;
        }).collect(Collectors.toList());
        return new ProfitVo(backTest.getSymbol(), positionInfos);
    }

    /**
     * ??????????????????
     */
    private Boolean skipCreate(BackTest backTest, BigDecimal price, PositionSideEnum
            positionSideEnum, LocalDateTime currentTime, Map<Long, List<CandleLine>> skipCache) {
        long startTime = currentTime.withHour(8).withMinute(0).withSecond(0).withNano(0).minusDays(21).toInstant(ZoneOffset.of("+8")).toEpochMilli();
        List<CandleLine> candleLines = null;
        if (skipCache.containsKey(startTime)) {
            candleLines = skipCache.get(startTime);
        } else {
            CandleLineRequest candleLineRequest = new CandleLineRequest();
            //BN?????????8???????????? ????????????  21???????????????
            candleLineRequest.setStartTime(startTime);
            candleLineRequest.setSymbol(backTest.getSymbol());
            candleLineRequest.setLimit(21L);
            candleLineRequest.setInterval(IntervalTypeEnum.ONE_DAY.getCode());
            candleLines = binanceFeignClient.candleLines(candleLineRequest);
            skipCache.put(startTime, candleLines);
        }
        if (candleLines.size() < 21) {
            //??????????????????????????????
            return false;
        }
        //BOLL??????????????????????????????????????????????????????????????????
        //??????????????????????????????
        candleLines.get(candleLines.size() - 1).setClose(price);
        //??????boll???
        BollBand bollBand = IndicatorUtil.getCurrentBoll(candleLines);
        if (positionSideEnum == PositionSideEnum.LONG && price.compareTo(bollBand.getUpper().add(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) >= 0) {
            //??????????????? ?????? ????????? +???md/2??? ???????????????(????????????)
            log.debug("???????????????????????????????????????????????????????????????{}", JSON.toJSONString(backTest));
            return true;
        }
        if (positionSideEnum == PositionSideEnum.SHORT && price.compareTo(bollBand.getLower().subtract(bollBand.getMd().divide(new BigDecimal(BigInteger.valueOf(2)), 8, RoundingMode.DOWN))) < 0) {
            //??????????????? ?????? ????????? - ???md/2??? ???????????????(????????????)
            log.debug("???????????????????????????????????????????????????????????????{}", JSON.toJSONString(backTest));
            return true;
        }
        return false;
    }

    private Position createPosition(BackTest backTest, OrderSideEnum orderSide, PositionSideEnum
            positionSide, Integer positionLevel, BigDecimal prePositionPrice, BigDecimal currentPrice, LocalDateTime
                                            currentTime) {
        //??????position
        Position position = new Position();
        position.setStrategyConfigId(backTest.getId());
        position.setLevel(positionLevel);
        if (prePositionPrice == null || prePositionPrice.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("[{}]  ????????????:{} ????????????,   ?????????{}", backTest.getSymbol(), positionSide, currentPrice);
            position.setRealCoverRate(BigDecimal.ZERO);
        } else {
            position.setRealCoverRate(prePositionPrice.subtract(currentPrice).abs().divide(prePositionPrice, 6, RoundingMode.HALF_DOWN).movePointRight(2));
            log.debug("[{}]  ????????????:{} ????????????,   ?????????:{}, ?????????:{}, ?????????:{}", backTest.getSymbol(), positionSide, prePositionPrice, currentPrice, position.getRealCoverRate() + "%");
        }
        position.setPositionSide(positionSide);
        BigDecimal positionAmount = backTest.getFirstPosition().add(new BigDecimal(String.valueOf(positionLevel - 1)).multiply(backTest.getSteppingPosition()));
        //?????? ?????????????????????
        positionAmount = positionAmount.multiply(backTest.getLeverage());
        position.setQuantity(binanceUtil.calcQuantity(backTest.getSymbol(), positionAmount, currentPrice));
        position.setPrice(currentPrice);
        position.setStatus(PositionStatusEnum.HOLD);
        position.setCreateTime(currentTime);
        position.setUpdateTime(currentTime);
        //??????????????? = price ?? quantity ?? ????????????
        position.setServiceAmount(position.getPrice().multiply(position.getQuantity()).multiply(serviceFee));
        //??????????????????
        return position;
    }

    private Position closePosition(BackTest backTest, Position position, BigDecimal currentPrice, LocalDateTime
            currentTime) {
        if (position.getStatus() == PositionStatusEnum.CLOSE) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "??????????????????:" + position.getId());
        }
        position.setUpdateTime(currentTime);
        position.setSellPrice(currentPrice);
        position.setStatus(PositionStatusEnum.CLOSE);
        switch (position.getPositionSide()) {
            case LONG:
                //?????????????????????
                position.setRealTargetRate(currentPrice.subtract(position.getPrice()).divide(position.getPrice(), 6, RoundingMode.HALF_DOWN).movePointRight(2));
                position.setProfitAmount(currentPrice.subtract(position.getPrice()).multiply(position.getQuantity()));
                break;
            case SHORT:
                //???????????????????????????
                position.setRealTargetRate(position.getPrice().subtract(currentPrice).divide(position.getPrice(), 6, RoundingMode.HALF_DOWN).movePointRight(2));
                position.setProfitAmount(position.getPrice().subtract(currentPrice).multiply(position.getQuantity()));
        }
        if (position.getRealTargetRate().compareTo(BigDecimal.ZERO) > 0) {
            log.debug("[{}]  ????????????:{} ????????????,   ?????????:{}, ?????????:{}, ?????????:{}", backTest.getSymbol(), position.getPositionSide(), position.getPrice(), currentPrice, position.getRealTargetRate() + "%");
        } else {
            log.debug("[{}]  ????????????:{} ????????????,   ?????????:{}, ?????????:{}, ?????????:{}", backTest.getSymbol(), position.getPositionSide(), position.getPrice(), currentPrice, position.getRealTargetRate() + "%");
        }
        //??????????????? = ??????????????? + ??????price ?? quantity ?? ????????????
        position.setServiceAmount(position.getServiceAmount().add(position.getSellPrice().multiply(position.getQuantity()).multiply(serviceFee)));
        return position;
    }

}
