package net.chiaai.bot.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class ClosePositionRequest implements Serializable {

    @NotBlank(message = "仓位id不能为空")
    private Long positionId;

}