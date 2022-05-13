package net.chiaai.bot.entity.request;

import lombok.Data;
import net.chiaai.bot.common.enums.StrategyStatusEnum;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class ChangeStrategyStatusRequest implements Serializable {

    //@ApiModelProperty(value = "策略id")
    @NotBlank(message = "策略id不能为空")
    private Long strategyId;

    //@ApiModelProperty(value = "策略状态：RUNNING,PAUSE,STOP")
    @NotBlank(message = "策略状态不能为空")
    private StrategyStatusEnum status;

}