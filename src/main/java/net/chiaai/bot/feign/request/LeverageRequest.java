package net.chiaai.bot.feign.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeverageRequest extends BaseRequest {

    private String symbol;

    /**
     * 目标杠杆倍数：1 到 125 整数
     */
    private Integer leverage;

}
