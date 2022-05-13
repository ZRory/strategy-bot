package net.chiaai.bot.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.chiaai.bot.common.enums.PositionSideEnum;
import net.chiaai.bot.common.enums.StrategyStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @TableName strategy_config
 */
@TableName(value = "strategy_config")
@Data
public class StrategyConfig implements Serializable {
    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 交易对
     */
    @TableField(value = "symbol")
    private String symbol;

    /**
     * 是否自动切换方向
     */
    @TableField(value = "auto_switch")
    private Boolean autoSwitch;

    /**
     * 自动平仓后是否自动再次创建策略
     */
    @TableField(value = "auto_restart")
    private Boolean autoRestart;

    /**
     * 自动平仓触发仓位
     */
    @TableField(value = "auto_restart_level")
    private Integer autoRestartLevel;

    /**
     * BOTH("多空双开"),LONG("开多"),SHORT("开空")
     */
    @TableField(value = "position_side")
    private PositionSideEnum positionSide;

    /**
     * 杠杆倍数
     */
    @TableField(value = "leverage")
    private BigDecimal leverage;

    /**
     * 单向购买次数
     */
    @TableField(value = "times")
    private Integer times;

    /**
     * 首仓金额
     */
    @TableField(value = "first_position")
    private BigDecimal firstPosition;

    /**
     * 步进值金额
     */
    @TableField(value = "stepping_position")
    private BigDecimal steppingPosition;

    /**
     * 每一仓仓位用,隔开
     */
    //@TableField(value = "positions")
    //private String positions;

    /**
     * 止盈率(/*ATR)
     */
    @TableField(value = "target_rate")
    private BigDecimal targetRate;

    /**
     * 止盈回撤率(/*ATR)
     */
    @TableField(value = "target_shrinks_rate")
    private BigDecimal targetShrinksRate;

    /**
     * 补仓率(/*ATR)
     */
    @TableField(value = "cover_rate")
    private BigDecimal coverRate;

    /**
     * 补仓回撤率(/*ATR)
     */
    @TableField(value = "cover_shrinks_rate")
    private BigDecimal coverShrinksRate;

    /**
     * 止损率(/*ATR)
     */
    @TableField(value = "stop_rate")
    private BigDecimal stopRate;

    /**
     * 状态
     */
    @TableField(value = "status")
    private StrategyStatusEnum status;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 是否分享
     */
    @TableField(value = "share")
    private Boolean share;

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