package net.chiaai.bot.feign.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionSideRequest extends BaseRequest {

    /**
     * "true": 双向持仓模式；"false": 单向持仓模式
     */
    private Boolean dualSidePosition;

}
