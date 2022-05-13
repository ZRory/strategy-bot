package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderSideEnum {

    BUY("买入"),
    SELL("卖出");

    private String desc;

}
