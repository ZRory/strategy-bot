package net.chiaai.bot.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.enums.*;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.utils.BinanceUtil;
import net.chiaai.bot.common.utils.DateUtils;
import net.chiaai.bot.common.utils.RedisUtils;
import net.chiaai.bot.entity.dao.Order;
import net.chiaai.bot.entity.dao.Position;
import net.chiaai.bot.entity.dao.StrategyConfig;
import net.chiaai.bot.entity.dao.User;
import net.chiaai.bot.feign.BinanceFeignClient;
import net.chiaai.bot.feign.DingTalkFeignClient;
import net.chiaai.bot.feign.request.DingMsgRequest;
import net.chiaai.bot.feign.request.OrderRequest;
import net.chiaai.bot.feign.response.OrderResponse;
import net.chiaai.bot.mapper.OrderMapper;
import net.chiaai.bot.mapper.PositionMapper;
import net.chiaai.bot.mapper.UserMapper;
import net.chiaai.bot.service.DealService;
import net.chiaai.bot.service.DingTalkService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * DealService 订单交易服务
 *
 * @author zhanghangtian
 * @description
 * @date 2021/12/16
 **/
@Slf4j
@Service
public class DealServiceImpl implements DealService {

    @Resource
    private BinanceFeignClient binanceClient;

    @Resource
    private BinanceUtil binanceUtil;

    @Resource
    private PositionMapper positionMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private DingTalkService dingTalkService;

    @Override
    public void createPosition(StrategyConfig strategyConfig, OrderSideEnum orderSide, PositionSideEnum positionSide, Integer positionLevel, BigDecimal prePositionPrice) {
        User user = userMapper.selectById(strategyConfig.getUserId());
        OrderRequest orderRequest = new OrderRequest();
        try {
            //构建订单参数
            orderRequest.setSymbol(strategyConfig.getSymbol());
            orderRequest.setSide(orderSide);
            orderRequest.setPositionSide(positionSide);
            orderRequest.setType(OrderTypeEnum.MARKET);
            //计算仓位买入金额 基准position
            //从第二仓开始算步进值 N仓 = 首仓金额 + ((当前仓位-1) × 步进值)
            BigDecimal position = strategyConfig.getFirstPosition().add(new BigDecimal(String.valueOf(positionLevel - 1)).multiply(strategyConfig.getSteppingPosition()));
            //仓位 需要乘杠杆倍率
            position = position.multiply(strategyConfig.getLeverage());
            //BigDecimal position = new BigDecimal(positions[positionLevel - 1]);
            orderRequest.setQuantity(binanceUtil.calcQuantity(strategyConfig.getSymbol(), position));
            orderRequest.setNewOrderRespType(OrderRespTypeEnum.RESULT);
            //创建订单
            OrderResponse longOrderResp = binanceClient.createOrder(user.getApiKey(), BinanceUtil.encodeParams(orderRequest, user));
            //log.info("开仓返回值：{}", JSON.toJSONString(longOrderResp));
            insertPositionInfo(strategyConfig.getId(), positionLevel, orderRequest, longOrderResp, prePositionPrice, user);
        } catch (Exception e) {
            log.error("创建订单异常:{}", JSON.toJSONString(orderRequest), e);
            dingTalkService.sendMessage(user,
                    "交易对：" + orderRequest.getSymbol() +
                            "\r\n交易方向:" + orderRequest.getPositionSide().getDesc() +
                            "\r\n操作：开仓" +
                            "\r\n执行结果：订单执行异常" +
                            "\r\n异常原因：" + e.getMessage() +
                            "\r\n执行时间：" + DateUtils.format(LocalDateTime.now()));
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "创建订单异常:" + e.getMessage());
        }
    }

    @Override
    public void closePosition(StrategyConfig strategyConfig, Position position, Boolean force) {
        if (position.getStatus() == PositionStatusEnum.CLOSE) {
            throw new BizException(BizCodeEnum.OPERATION_FAILED, "该订单已平仓:" + position.getId());
        }
        String key = "CLOSE_ORDER_LOCK:" + position.getId();
        String lock = redisUtils.getString(key);
        if (StringUtils.isBlank(lock)) {
            //加锁
            redisUtils.set(key, position.getId().toString(), 20, TimeUnit.SECONDS);
            OrderRequest orderSellRequest = new OrderRequest();
            User user = userMapper.selectById(strategyConfig.getUserId());
            try {
                //构建订单参数
                orderSellRequest.setSymbol(strategyConfig.getSymbol());
                if (position.getPositionSide() == PositionSideEnum.LONG) {
                    orderSellRequest.setSide(OrderSideEnum.SELL);
                } else if (position.getPositionSide() == PositionSideEnum.SHORT) {
                    orderSellRequest.setSide(OrderSideEnum.BUY);
                }
                orderSellRequest.setPositionSide(position.getPositionSide());
                orderSellRequest.setType(OrderTypeEnum.MARKET);
                orderSellRequest.setQuantity(position.getQuantity());
                orderSellRequest.setNewOrderRespType(OrderRespTypeEnum.RESULT);
                //卖出多单
                OrderResponse longSellResp = binanceClient.createOrder(user.getApiKey(), BinanceUtil.encodeParams(orderSellRequest, user));
                //更新保存订单信息
                updateClosePositonInfo(position, orderSellRequest, longSellResp, user);
            } catch (Exception e) {
                log.error("卖出订单异常:{}", JSON.toJSONString(orderSellRequest), e);
                if (force || (e.getMessage() != null && e.getMessage().contains("ReduceOnly Order is rejected"))) {
                    log.info("卖出时报错: 且 强制卖出,仓位置为卖出状态");
                    LocalDateTime now = LocalDateTime.now();
                    position.setUpdateTime(now);
                    position.setSellPrice(BigDecimal.ZERO);
                    position.setStatus(PositionStatusEnum.CLOSE);
                    //实际止盈率计算
                    position.setRealTargetRate(BigDecimal.ZERO);
                    position.setProfitAmount(BigDecimal.ZERO);
                    //更新position
                    positionMapper.updateById(position);
                    //插入order记录
                    //无order
                    return;
                }
                dingTalkService.sendMessage(user,
                        "交易对：" + orderSellRequest.getSymbol() +
                                "\r\n交易方向:" + orderSellRequest.getPositionSide().getDesc() +
                                "\r\n操作：平仓" +
                                "\r\n执行结果：订单执行异常" +
                                "\r\n异常原因：" + e.getMessage() +
                                "\r\n执行时间：" + DateUtils.format(LocalDateTime.now()));
                throw new BizException(BizCodeEnum.OPERATION_FAILED, "卖出订单异常:" + e.getMessage());
            } finally {
                redisUtils.del(key);
            }
        } else {
            throw new BizException(BizCodeEnum.TOO_MANY_REQUEST);
        }
    }

    private void insertPositionInfo(Long strategyConfigId, Integer level, OrderRequest orderRequest, OrderResponse orderResponse, BigDecimal prePositionPrice, User user) {
        LocalDateTime now = LocalDateTime.now();
        //创建position
        Position position = new Position();
        position.setStrategyConfigId(strategyConfigId);
        position.setLevel(level);
        if (prePositionPrice == null || prePositionPrice.compareTo(BigDecimal.ZERO) == 0) {
            log.info("[{}]  订单方向:{} 首仓买入,   买入价{}", orderResponse.getSymbol(), orderResponse.getPositionSide(), orderResponse.getAvgPrice());
            position.setRealCoverRate(BigDecimal.ZERO);
        } else {
            position.setRealCoverRate(prePositionPrice.subtract(orderResponse.getAvgPrice()).abs().divide(prePositionPrice, 6, RoundingMode.HALF_DOWN).movePointRight(2));
            log.info("[{}]  订单方向:{} 补仓买入,   前仓价:{}, 买入价:{}, 补仓率:{}", orderResponse.getSymbol(), orderResponse.getPositionSide(), prePositionPrice, orderResponse.getAvgPrice(), position.getRealCoverRate() + "%");
        }
        dingTalkService.sendMessage(user,
                "交易对：" + orderResponse.getSymbol() +
                        "\r\n当前仓位：" + position.getLevel() +
                        "\r\n交易方向:" + orderResponse.getPositionSide().getDesc() +
                        "\r\n操作：开仓" +
                        "\r\n买入价格：" + orderResponse.getAvgPrice() +
                        "\r\n执行数量：" + orderResponse.getExecutedQty() +
                        "\r\n补仓率：" + position.getRealCoverRate() +
                        "\r\n执行时间：" + DateUtils.format(LocalDateTime.now()));
        position.setPositionSide(orderRequest.getPositionSide());
        position.setQuantity(orderResponse.getExecutedQty());
        position.setPrice(orderResponse.getAvgPrice());
        position.setStatus(PositionStatusEnum.HOLD);
        position.setCreateTime(now);
        position.setUpdateTime(now);
        //计算手续费 = price × quantity × 手续费率
        position.setServiceAmount(position.getPrice().multiply(position.getQuantity()).multiply(user.getServiceAmountRate()));
        positionMapper.insert(position);
        //创建订单结果
        insertOrder(orderRequest, orderResponse, position, user.getServiceAmountRate());
    }

    private void updateClosePositonInfo(Position position, OrderRequest orderRequest, OrderResponse orderResponse, User user) {
        LocalDateTime now = LocalDateTime.now();
        position.setUpdateTime(now);
        position.setSellPrice(orderResponse.getAvgPrice());
        position.setStatus(PositionStatusEnum.CLOSE);
        switch (position.getPositionSide()) {
            case LONG:
                //实际止盈率计算
                position.setRealTargetRate(orderResponse.getAvgPrice().subtract(position.getPrice()).divide(position.getPrice(), 6, RoundingMode.HALF_DOWN).movePointRight(2));
                position.setProfitAmount(orderResponse.getAvgPrice().subtract(position.getPrice()).multiply(position.getQuantity()));
                break;
            case SHORT:
                //空单实际止盈率计算
                position.setRealTargetRate(position.getPrice().subtract(orderResponse.getAvgPrice()).divide(position.getPrice(), 6, RoundingMode.HALF_DOWN).movePointRight(2));
                position.setProfitAmount(position.getPrice().subtract(orderResponse.getAvgPrice()).multiply(position.getQuantity()));
        }
        if (position.getRealTargetRate().compareTo(BigDecimal.ZERO) > 0) {
            log.info("[{}]  订单方向:{} 止盈卖出,   成本价:{}, 卖出价:{}, 止盈率:{}", orderResponse.getSymbol(), orderResponse.getPositionSide(), position.getPrice(), orderResponse.getAvgPrice(), position.getRealTargetRate() + "%");
        } else {
            log.info("[{}]  订单方向:{} 止损卖出,   成本价:{}, 卖出价:{}, 止损率:{}", orderResponse.getSymbol(), orderResponse.getPositionSide(), position.getPrice(), orderResponse.getAvgPrice(), position.getRealTargetRate() + "%");
        }
        dingTalkService.sendMessage(user,
                "交易对：" + orderResponse.getSymbol() +
                        "\r\n当前仓位：" + position.getLevel() +
                        "\r\n交易方向:" + orderResponse.getPositionSide().getDesc() +
                        "\r\n操作：平仓" +
                        "\r\n买入价格：" + position.getPrice() +
                        "\r\n卖出价格：" + orderResponse.getAvgPrice() +
                        "\r\n执行数量：" + position.getQuantity() +
                        "\r\n盈亏率：" + position.getRealTargetRate() +
                        "\r\n盈亏金额：" + position.getProfitAmount() +
                        "\r\n执行时间：" + DateUtils.format(LocalDateTime.now()));
        //更新position
        //计算手续费 = 买入手续费 + 卖出price × quantity × 手续费率
        position.setServiceAmount(position.getServiceAmount().add(position.getSellPrice().multiply(position.getQuantity()).multiply(user.getServiceAmountRate())));
        positionMapper.updateById(position);
        //插入order记录
        insertOrder(orderRequest, orderResponse, position, user.getServiceAmountRate());
    }

    private void insertOrder(OrderRequest orderRequest, OrderResponse orderResponse, Position position, BigDecimal serviceAmountRate) {
        Order order = new Order();
        order.setPositionId(position.getId());
        order.setSymbol(orderRequest.getSymbol());
        order.setPositionSide(orderRequest.getPositionSide());
        order.setSide(orderRequest.getSide());
        order.setType(orderRequest.getType());
        order.setQuantity(orderRequest.getQuantity());
        order.setPrice(orderResponse.getAvgPrice());
        order.setClientOrderId(orderResponse.getClientOrderId());
        order.setCumQuote(orderResponse.getCumQuote());
        order.setOrderId(orderResponse.getOrderId());
        order.setStatus(orderResponse.getStatus());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        //交易手续费
        order.setServiceAmount(order.getCumQuote().multiply(serviceAmountRate));
        orderMapper.insert(order);
    }

}
