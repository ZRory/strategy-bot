package net.chiaai.bot.entity.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import net.chiaai.bot.common.enums.StrategyStatusEnum;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @TableName back_test_result
 */
@TableName(value = "back_test_result")
@Data
public class BackTestResult {
    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联回测id
     */
    @TableField(value = "back_task_id")
    private Long backTaskId;

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
     * 最大仓位
     */
    @TableField(value = "max_level")
    private Integer maxLevel;

    /**
     * 最大持仓金额
     */
    @TableField(value = "max_amount")
    private BigDecimal maxAmount;

    /**
     * 总仓位
     */
    @TableField(value = "times")
    private Integer times;

    /**
     * 总盈亏金额
     */
    @TableField(value = "total_profit")
    private BigDecimal totalProfit;

    /**
     * 止盈金额
     */
    @TableField(value = "target_profit")
    private BigDecimal targetProfit;

    /**
     * 止损金额
     */
    @TableField(value = "stop_profit")
    private BigDecimal stopProfit;

    /**
     * 手续费
     */
    @TableField(value = "service_amount")
    private BigDecimal serviceAmount;

    /**
     * 已实现盈亏金额
     */
    @TableField(value = "current_profit")
    private BigDecimal currentProfit;

    /**
     * 浮亏金额
     */
    @TableField(value = "sliding_profit")
    private BigDecimal slidingProfit;

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