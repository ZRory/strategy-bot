package net.chiaai.bot.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.chiaai.bot.common.enums.OrderSideEnum;
import net.chiaai.bot.common.enums.OrderStatusEnum;
import net.chiaai.bot.common.enums.OrderTypeEnum;
import net.chiaai.bot.common.enums.PositionSideEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表
 *
 * @TableName order
 */
@TableName(value = "orders")
@Data
public class Order implements Serializable {
    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联仓位id
     */
    @TableField(value = "position_id")
    private Long positionId;

    /**
     * 交易对
     */
    @TableField(value = "symbol")
    private String symbol;

    /**
     * 持仓方向
     */
    @TableField(value = "position_side")
    private PositionSideEnum positionSide;

    /**
     * 买卖方向 SELL, BUY
     */
    @TableField(value = "side")
    private OrderSideEnum side;

    /**
     * 订单类型 LIMIT, MARKET, STOP, TAKE_PROFIT, STOP_MARKET, TAKE_PROFIT_MARKET, TRAILING_STOP_MARKET
     */
    @TableField(value = "type")
    private OrderTypeEnum type;

    /**
     * 成交数量
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 成交价格
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 成交金额
     */
    @TableField(value = "cum_quote")
    private BigDecimal cumQuote;

    /**
     * 交易手续费
     */
    @TableField(value = "service_amount")
    private BigDecimal serviceAmount;

    /**
     * 用户自定义orderId
     */
    @TableField(value = "client_order_id")
    private String clientOrderId;

    /**
     * 币安订单号
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 订单状态
     */
    @TableField(value = "status")
    private OrderStatusEnum status;

    /**
     *
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     *
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}