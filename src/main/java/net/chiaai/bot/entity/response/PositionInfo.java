package net.chiaai.bot.entity.response;

import lombok.Data;
import net.chiaai.bot.common.enums.PositionSideEnum;
import net.chiaai.bot.common.enums.PositionStatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓位表
 *
 * @TableName position
 */
@Data
public class PositionInfo implements Serializable {

    /**
     *
     */
    private Long strategyConfigId;

    /**
     * 仓位等级
     */
    private Integer level;

    /**
     * 实际补仓率
     */
    private BigDecimal realCoverRate;

    /**
     * 持仓方向
     */
    private PositionSideEnum positionSide;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 成本价
     */
    private BigDecimal price;

    /**
     * 仓位金额
     */
    private BigDecimal positionAmount;

    /**
     * 卖出单价
     */
    private BigDecimal sellPrice;

    /**
     * 实际止盈率
     */
    private BigDecimal realTargetRate;

    /**
     * 盈亏金额
     */
    private BigDecimal profitAmount;

    /**
     * 服务费
     */
    private BigDecimal serviceAmount;

    /**
     * 浮动盈亏金额
     */
    private BigDecimal slidingProfitAmount;

    /**
     * 仓位状态
     */
    private PositionStatusEnum status;

    /**
     *
     */
    private LocalDateTime createTime;

    /**
     *
     */
    private LocalDateTime updateTime;

}