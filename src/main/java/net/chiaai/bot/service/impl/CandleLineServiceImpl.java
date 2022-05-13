package net.chiaai.bot.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.enums.IntervalTypeEnum;
import net.chiaai.bot.common.enums.SymbolStatusEnum;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.utils.RedisUtils;
import net.chiaai.bot.conf.env.BinanceCatchs;
import net.chiaai.bot.entity.dao.CandleLine;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.request.CandleLineRequest;
import net.chiaai.bot.feign.response.Symbol;
import net.chiaai.bot.mapper.CandleLineMapper;
import net.chiaai.bot.service.CandleLineService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CandleLineServiceImpl implements CandleLineService {

    @Resource
    private BinanceFeignClient binanceFeignClient;

    @Resource
    private CandleLineMapper candleLineMapper;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public CandleLine getCandleLine(String symbol, LocalDateTime openTime) {
        LambdaQueryWrapper<CandleLine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CandleLine::getSymbol, symbol);
        queryWrapper.eq(CandleLine::getOpenTime, openTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        return candleLineMapper.selectOne(queryWrapper);
    }

    @Override
    public CandleLine getMin(String symbol, LocalDateTime startTime, LocalDateTime endTime, SFunction<CandleLine, ?> column) {
        LambdaQueryWrapper<CandleLine> queryWrapper = new LambdaQueryWrapper<>();
        //BN cn时间每天8点更新日K
        queryWrapper.ge(CandleLine::getOpenTime, startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        queryWrapper.le(CandleLine::getOpenTime, endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        queryWrapper.eq(CandleLine::getSymbol, symbol);
        queryWrapper.orderByAsc(column);
        queryWrapper.last("limit 1");
        return candleLineMapper.selectOne(queryWrapper);
    }

    @Override
    public CandleLine getMax(String symbol, LocalDateTime startTime, LocalDateTime endTime, SFunction<CandleLine, ?> column) {
        LambdaQueryWrapper<CandleLine> queryWrapper = new LambdaQueryWrapper<>();
        //BN cn时间每天8点更新日K
        queryWrapper.ge(CandleLine::getOpenTime, startTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        queryWrapper.le(CandleLine::getOpenTime, endTime.toInstant(ZoneOffset.of("+8")).toEpochMilli());
        queryWrapper.eq(CandleLine::getSymbol, symbol);
        queryWrapper.orderByDesc(column);
        queryWrapper.last("limit 1");
        return candleLineMapper.selectOne(queryWrapper);
    }

    @Async
    @Override
    public void update(String symbolName) {
        log.info("开始更新K线信息 交易对：{}", symbolName);
        String key = "UPDATE_CANDLE_LINE_LOCK:" + symbolName;
        String lock = redisUtils.getString(key);
        if (StringUtils.isBlank(lock)) {
            //加锁
            redisUtils.set(key, symbolName, 120, TimeUnit.MINUTES);
            try {
                Symbol symbol = BinanceCatchs.symbols.get(symbolName);
                if (symbol == null || symbol.getStatus() == SymbolStatusEnum.CLOSE || symbol.getStatus() == SymbolStatusEnum.PENDING_TRADING) {
                    log.error("交易对：{} 不存在或不处于可交易状态", symbolName);
                    throw new BizException(BizCodeEnum.OPERATION_FAILED, "交易对： " + symbolName + " 不存在或不处于可交易状态");
                }
                LambdaQueryWrapper<CandleLine> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(CandleLine::getSymbol, symbolName);
                queryWrapper.orderByDesc(CandleLine::getCloseTime);
                queryWrapper.last("limit 1");
                CandleLine candleLine = candleLineMapper.selectOne(queryWrapper);
                //Long startTime = LocalDateTime.of(2022, 1, 1, 0, 0).toInstant(ZoneOffset.of("+8")).toEpochMilli();
                Long startTime = 1640966400000L; //2022.1.1 00:00
                if (ObjectUtils.isNotEmpty(candleLine)) {
                    startTime = candleLine.getCloseTime();
                }
                CandleLineRequest candleLineRequest = new CandleLineRequest(symbolName);
                candleLineRequest.setInterval(IntervalTypeEnum.ONE_MINUTE.getCode());
                candleLineRequest.setLimit(1000L);
                candleLineRequest.setStartTime(startTime);
                while (true) {
                    List<CandleLine> candleLines = binanceFeignClient.candleLines(candleLineRequest);
                    if (CollectionUtils.isEmpty(candleLines)) {
                        break;
                    }
                    CandleLine lastCandleLine = candleLines.get(candleLines.size() - 1);
                    if (candleLines.size() < 1000) {
                        candleLines.remove(lastCandleLine);
                    }
                    candleLines.forEach(x -> {
                        //插入数据库
                        x.setSymbol(symbolName);
                        candleLineMapper.insert(x);
                    });
                    candleLineRequest.setStartTime(lastCandleLine.getCloseTime());
                }
            } catch (Exception e) {
                log.error("更新K线到数据库异常", e);
            } finally {
                redisUtils.del(key);
            }
        } else {
            throw new BizException(BizCodeEnum.TOO_MANY_REQUEST);
        }
    }

    @Override
    public List<String> listSymbols() {
        String key = "BackTest:Symbols";
        List<String> symbols = redisUtils.getList(key, String.class);
        if (CollectionUtils.isEmpty(symbols)) {
            LambdaQueryWrapper<CandleLine> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(CandleLine::getSymbol);
            queryWrapper.groupBy(CandleLine::getSymbol);
            symbols = candleLineMapper.selectList(queryWrapper).stream().map(x -> x.getSymbol()).collect(Collectors.toList());
            redisUtils.set(key, JSON.toJSONString(symbols), 10, TimeUnit.MINUTES);
        }
        return symbols;
    }

    public static void main(String[] args) {
        int amount = 0;
        for (int i = 1; i <= 57; i++) {
            amount += i * 5 + 10;
        }
        System.out.println(amount);
    }
}