package net.chiaai.bot.feign.request;

import lombok.Getter;
import lombok.Setter;
import net.chiaai.bot.common.enums.*;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderRequest extends BaseRequest {

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 买卖方向 SELL, BUY
     */
    private OrderSideEnum side;

    /**
     * 持仓方向，单向持仓模式下非必填，默认且仅可填BOTH;在双向持仓模式下必填,且仅可选择 LONG 或 SHORT
     */
    private PositionSideEnum positionSide;

    /**
     * 订单类型 LIMIT, MARKET, STOP, TAKE_PROFIT, STOP_MARKET, TAKE_PROFIT_MARKET, TRAILING_STOP_MARKET
     */
    private OrderTypeEnum type;

    /**
     * true, false; 非双开模式下默认false；双开模式下不接受此参数； 使用closePosition不支持此参数。
     */
    private Boolean reduceOnly;

    /**
     * 下单数量,使用closePosition不支持此参数。
     */
    private BigDecimal quantity;

    /**
     * 委托价格
     */
    private BigDecimal price;

    /**
     * 用户自定义orderId
     */
    private String newClientOrderId;

    /**
     * 触发价, 仅 STOP, STOP_MARKET, TAKE_PROFIT, TAKE_PROFIT_MARKET 需要此参数
     */
    private BigDecimal stopPrice;

    /**
     * true, false；触发后全部平仓，仅支持STOP_MARKET和TAKE_PROFIT_MARKET；不与quantity合用；自带只平仓效果，不与reduceOnly 合用
     */
    private Boolean closePosition;

    /**
     * 追踪止损激活价格，仅TRAILING_STOP_MARKET 需要此参数, 默认为下单当前市场价格(支持不同workingType)
     */
    private BigDecimal activationPrice;

    /**
     * 追踪止损回调比例，可取值范围[0.1, 5],其中 1代表1% ,仅TRAILING_STOP_MARKET 需要此参数
     */
    private BigDecimal callbackRate;

    /**
     * 有效方法
     */
    private TimeInForceEnum timeInForce;

    /**
     * stopPrice 触发类型: MARK_PRICE(标记价格), CONTRACT_PRICE(合约最新价). 默认 CONTRACT_PRICE
     */
    private WorkingTypeEnum workingType;

    /**
     * 条件单触发保护："TRUE","FALSE", 默认"FALSE". 仅 STOP, STOP_MARKET, TAKE_PROFIT, TAKE_PROFIT_MARKET 需要此参数
     */
    private Boolean priceProtect;

    /**
     * "ACK", "RESULT", 默认 "ACK"
     */
    private OrderRespTypeEnum newOrderRespType;

}
