package net.chiaai.bot.entity.request;

import lombok.Data;
import net.chiaai.bot.common.enums.PositionSideEnum;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @TableName strategy_config
 */
@Data
public class UpdateStrategyRequest implements Serializable {

    //@ApiModelProperty(value = "策略id")
    @NotBlank(message = "策略id不能为空")
    private Long strategyId;

    /**
     * 是否自动切换
     */
    //@ApiModelProperty(value = "补仓回撤率")
    //@NotNull(message = "是否分享不能为空")
    private Boolean autoSwitch;

    /**
     * BOTH("多空双开"),LONG("开多"),SHORT("开空")
     */
    //@NotBlank(message = "开单方向不能为空")
    private PositionSideEnum positionSide;

    /**
     * 单向购买次数
     */
    //@ApiModelProperty(value = "单向购买次数")
    @NotBlank(message = "单向购买次数不能为空")
    private Integer times;

    /**
     * 首仓金额
     */
    //@ApiModelProperty(value = "首仓金额")
    @NotNull(message = "首仓金额不能为空")
    private BigDecimal firstPosition;

    /**
     * 步进值金额
     */
    //@ApiModelProperty(value = "步进加仓金额")
    @NotNull(message = "步进加仓金额不能为空")
    private BigDecimal steppingPosition;

    /**
     * 止盈率(/*ATR)
     */
    //@ApiModelProperty(value = "止盈率")
    @NotNull(message = "止盈率不能为空")
    private BigDecimal targetRate;

    /**
     * 止盈回撤率(/*ATR)
     */
    //@ApiModelProperty(value = "止盈回撤率")
    @NotNull(message = "止盈回撤率不能为空")
    private BigDecimal targetShrinksRate;

    /**
     * 补仓率(/*ATR)
     */
    //@ApiModelProperty(value = "补仓率")
    @NotNull(message = "补仓率不能为空")
    private BigDecimal coverRate;

    /**
     * 补仓回撤率(/*ATR)
     */
    //@ApiModelProperty(value = "补仓回撤率")
    @NotNull(message = "补仓回撤率不能为空")
    private BigDecimal coverShrinksRate;

    /**
     * 止损率(/*ATR)
     */
    //@ApiModelProperty(value = "止损率")
    @NotNull(message = "止损率不能为空")
    private BigDecimal stopRate;

    /**
     * 补仓回撤率(/*ATR)
     */
    //@ApiModelProperty(value = "补仓回撤率")
    @NotNull(message = "是否分享不能为空")
    private Boolean share;

    /**
     * 自动平仓后是否自动再次创建策略
     */
    private Boolean autoRestart;

    /**
     * 自动平仓触发仓位
     */
    private Integer autoRestartLevel;

}