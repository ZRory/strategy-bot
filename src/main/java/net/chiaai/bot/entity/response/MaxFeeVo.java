package net.chiaai.bot.entity.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * MaxFeeVo
 *
 * @author zhanghangtian
 * @description
 * @date 2021/11/15
 **/

@Getter
@Setter
public class MaxFeeVo {

    private String symbol;

    private BigDecimal fee;

    private BigDecimal rate;

}
