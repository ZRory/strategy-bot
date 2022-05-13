package net.chiaai.bot.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderTypeEnum {

    LIMIT("限价单"),
    MARKET("市价单"),
    STOP("止损单"),
    STOP_MARKET("止损市价单"),
    TAKE_PROFIT("止盈单"),
    TAKE_PROFIT_MARKET("止盈市价单"),
    TRAILING_STOP_MARKET("跟踪止损市价单");

    private String desc;

}
