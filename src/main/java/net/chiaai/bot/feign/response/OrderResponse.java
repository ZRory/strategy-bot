package net.chiaai.bot.feign.response;

import lombok.Getter;
import lombok.Setter;
import net.chiaai.bot.common.enums.*;

import java.math.BigDecimal;

/**
 * 创建订单返回值
 */
@Getter
@Setter
public class OrderResponse extends BaseResponse{

    // 用户自定义的订单号
    private String clientOrderId;

    private String cumQty;

    // 成交金额
    private BigDecimal cumQuote;

    // 成交量
    private BigDecimal executedQty;

    // 系统订单号
    private Long orderId;

    // 平均成交价
    private BigDecimal avgPrice;

    // 原始委托数量
    private BigDecimal origQty;

    // 委托价格
    private BigDecimal price;

    // 仅减仓
    private Boolean reduceOnly;

    // 买卖方向
    private OrderSideEnum side;

    // 持仓方向
    private PositionSideEnum positionSide;

    // 订单状态
    private OrderStatusEnum status;

    // 触发价，对`TRAILING_STOP_MARKET`无效
    private BigDecimal stopPrice;

    // 是否条件全平仓
    private Boolean closePosition;

    // 交易对
    private String symbol;

    // 有效方法
    private TimeInForceEnum timeInForce;

    // 订单类型
    private OrderTypeEnum type;

    // 触发前订单类型
    private OrderTypeEnum origType;

    // 跟踪止损激活价格, 仅`TRAILING_STOP_MARKET` 订单返回此字段
    private BigDecimal activatePrice;

    // 跟踪止损回调比例, 仅`TRAILING_STOP_MARKET` 订单返回此字段
    private BigDecimal priceRate;

    // 更新时间
    private Long updateTime;

    // 条件价格触发类型
    private WorkingTypeEnum workingType;

    // 是否开启条件单触发保护
    private Boolean priceProtect;

}
