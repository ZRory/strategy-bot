package net.chiaai.bot.feign.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionSideResponse extends BaseResponse{

    /**
     * "true": 双向持仓模式；"false": 单向持仓模式
     */
    private Boolean dualSidePosition;

}
