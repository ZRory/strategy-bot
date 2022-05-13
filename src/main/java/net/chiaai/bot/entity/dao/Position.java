package net.chiaai.bot.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.chiaai.bot.common.enums.PositionSideEnum;
import net.chiaai.bot.common.enums.PositionStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓位表
 *
 * @TableName position
 */
@TableName(value = "position")
@Data
public class Position implements Serializable {
    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     *
     */
    @TableField(value = "strategy_config_id")
    private Long strategyConfigId;

    /**
     * 仓位等级
     */
    @TableField(value = "level")
    private Integer level;

    /**
     * 实际补仓率
     */
    @TableField(value = "real_cover_rate")
    private BigDecimal realCoverRate;

    /**
     * 持仓方向
     */
    @TableField(value = "position_side")
    private PositionSideEnum positionSide;

    /**
     * 数量
     */
    @TableField(value = "quantity")
    private BigDecimal quantity;

    /**
     * 成本价
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 卖出单价
     */
    @TableField(value = "sell_price")
    private BigDecimal sellPrice;

    /**
     * 实际止盈率
     */
    @TableField(value = "real_target_rate")
    private BigDecimal realTargetRate;

    /**
     * 交易手续费（买入+卖出）
     */
    @TableField(value = "service_amount")
    private BigDecimal serviceAmount;

    /**
     * 盈亏金额
     */
    @TableField(value = "profit_amount")
    private BigDecimal profitAmount;

    /**
     * 仓位状态
     */
    @TableField(value = "status")
    private PositionStatusEnum status;

    /**
     *
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     *
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}