package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FilterTypeEnum {

    /**
     * 价格过滤器用于检测order订单中price参数的合法性
     * <p>
     * minPrice 定义了 price/stopPrice 允许的最小值
     * maxPrice 定义了 price/stopPrice 允许的最大值。
     * tickSize 定义了 price/stopPrice 的步进间隔，即price必须等于minPrice+(tickSize的整数倍) 以上每一项均可为0，为0时代表这一项不再做限制。
     */
    PRICE_FILTER("价格过滤器"),
    /**
     * lots是拍卖术语，这个过滤器对订单中的quantity也就是数量参数进行合法性检查。包含三个部分：
     * <p>
     * minQty 表示 quantity 允许的最小值.
     * maxQty 表示 quantity 允许的最大值
     * stepSize 表示 quantity允许的步进值。
     */
    LOT_SIZE("订单尺寸"),
    MARKET_LOT_SIZE("市价订单尺寸"),
    /**
     * 定义了某个交易对最多允许的挂单数量(不包括已关闭的订单)
     * <p>
     * 普通订单与条件订单均计算在内
     */
    MAX_NUM_ORDERS("最多订单数"),
    /**
     * 定义了某个交易对最多允许的条件订单的挂单数量(不包括已关闭的订单)。
     * <p>
     * 条件订单目前包括STOP, STOP_MARKET, TAKE_PROFIT, TAKE_PROFIT_MARKET, 和 TRAILING_STOP_MARKET
     */
    MAX_NUM_ALGO_ORDERS("最多条件订单数"),
    /**
     * PERCENT_PRICE 定义了基于标记价格计算的挂单价格的可接受区间.
     */
    PERCENT_PRICE("价格振幅过滤器"),
    /**
     * MIN_NOTIONAL过滤器定义了交易对订单所允许的最小名义价值(成交额)。 订单的名义价值是价格*数量。 由于MARKET订单没有价格，因此会使用 mark price 计算。
     */
    MIN_NOTIONAL("最小名义价值");

    private String desc;

}
