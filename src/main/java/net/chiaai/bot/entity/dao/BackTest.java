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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @TableName back_test
 */
@TableName(value = "back_test")
@Data
public class BackTest {
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
     * 开始时间
     */
    @TableField(value = "start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * BOTH("多空双开"),LONG("开多"),SHORT("开空")
     */
    @TableField(value = "position_side")
    private PositionSideEnum positionSide;

    /**
     * 杠杆倍率
     */
    @TableField(value = "leverage")
    private BigDecimal leverage;

    /**
     * 单向购买次数
     */
    @TableField(value = "times")
    private Integer times;

    /**
     * 首仓金额(USDT)
     */
    @TableField(value = "first_position")
    private BigDecimal firstPosition;

    /**
     * 每仓步进金额(USDT)
     */
    @TableField(value = "stepping_position")
    private BigDecimal steppingPosition;

    /**
     * 止盈率
     */
    @TableField(value = "target_rate")
    private BigDecimal targetRate;

    /**
     * 止盈回撤率
     */
    @TableField(value = "target_shrinks_rate")
    private BigDecimal targetShrinksRate;

    /**
     * 补仓率
     */
    @TableField(value = "cover_rate")
    private BigDecimal coverRate;

    /**
     * 补仓回撤率
     */
    @TableField(value = "cover_shrinks_rate")
    private BigDecimal coverShrinksRate;

    /**
     * 止损率
     */
    @TableField(value = "stop_rate")
    private BigDecimal stopRate;

    /**
     * 自动平仓后是否重建策略
     */
    @TableField(value = "auto_restart")
    private Boolean autoRestart;

    /**
     * 自动平仓触发仓位
     */
    @TableField(value = "auto_restart_level")
    private Integer autoRestartLevel;

    /**
     * 状态
     */
    @TableField(value = "status")
    private StrategyStatusEnum status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}